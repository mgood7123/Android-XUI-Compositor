package XUI.Platform.AndroidInternal;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

import static XUI.Platform.AndroidInternal.GLErrorLog.*;

public class MiniCompositor {
    EGLRenderThread compositor = new EGLRenderThread();

    EGLRenderThread canvas = new EGLRenderThread();

    int mWidth = 0, mHeight = 0;

    SurfaceTexture surface;

    CopyOnWriteArrayList<TextureCanvas> textureCanvasList = new CopyOnWriteArrayList<>();

    public MiniCompositor() {
        compositor.thread.SetOnDoFrame(this::OnCompositeFrame);
        canvas.thread.SetOnDoFrame(this::OnCanvasFrame);
    }

    int g = 0;

    private void OnCanvasFrame(long timeNanos) {
        // EGL resources shared with compositor thread
        for (TextureCanvas textureCanvas : textureCanvasList) {
            if (canvas.egl.HasSurface()) continue;
            if (textureCanvas.IsCreated()) {
                synchronized (textureCanvas.isWriting) {
                    if (!textureCanvas.isReading.get()) {
                        textureCanvas.isWriting.set(true);
                    }
                }
                if (!textureCanvas.isWriting.get()) {
                    Error("CANVAS THREAD: compositor is reading");
                    continue;
                } else {
                    Error("CANVAS THREAD: compositor is not reading");
                }
                Error("CANVAS THREAD: attach to surface texture");
                if (!canvas.egl.TryAttachToSurfaceTexture(textureCanvas.surfaceTexture)) {
                    Error("CANVAS THREAD: failed to attach to surface texture");
                    continue;
                }
                if (!canvas.egl.MakeCurrent()) {
                    Error("CANVAS THREAD: failed to make egl context current");
                    continue;
                }
                Error("CANVAS THREAD: made egl context current");
                Error("CANVAS THREAD: resize texture canvas");
                textureCanvas.Resize(mWidth, mHeight);
                Error("CANVAS THREAD: resized texture canvas");
                Canvas glCanvas = textureCanvas.BindOesTextureAndAcquireCanvas();
                if (glCanvas != null) {
                    g++;
                    if (g == 256) {
                        g = 0;
                    }
                    Error("CANVAS THREAD: draw A 255 R 255 G " + g + " B 0");
                    glCanvas.drawARGB(255, 255, g, 0);
                    GLES20.glFlush();
                    textureCanvas.ReleaseCanvas(glCanvas, () -> {
                        if (!canvas.egl.TryDetachFromSurfaceTexture()) {
                            Error("CANVAS THREAD: failed to detach from surface texture");
                        }
                        if (!canvas.egl.ReleaseCurrent()) {
                            Error("CANVAS THREAD: failed to release current egl context");
                        }
                    });
                } else {
                    if (!canvas.egl.TryDetachFromSurfaceTexture()) {
                        Error("CANVAS THREAD: failed to detach from surface texture");
                    }
                    if (!canvas.egl.ReleaseCurrent()) {
                        Error("CANVAS THREAD: failed to release current egl context");
                    }
                }
            }
        }
    }

    private void OnCompositeFrame(long timeNanos) {
        if (surface == null) {
            return;
        }
//        android.opengl.GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
//        android.opengl.GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Error("COMPOSITOR THREAD: drawing textures");
        for (TextureCanvas textureCanvas : textureCanvasList) {
            synchronized (textureCanvas.isWriting) {
                if (!textureCanvas.isWriting.get()) {
                    textureCanvas.isReading.set(true);
                }
            }
            if (textureCanvas.isReading.get()) {
                Error("COMPOSITOR THREAD: canvas is not writing");
                if (!compositor.egl.TryAttachToSurfaceTexture(surface)) {
                    Error("COMPOSITOR THREAD: failed to get EGL Surface");
                } else {
                    Error("COMPOSITOR THREAD: got EGL Surface");
                    if (!compositor.egl.MakeCurrent()) {
                        Error("COMPOSITOR THREAD: failed to make egl context current");
                    } else {
                        Error("COMPOSITOR THREAD: made egl context current");
                        if (!textureCanvas.IsCreated()) {
                            textureCanvas.Create();
                        } else {
                            Error("COMPOSITOR THREAD: drawing texture");
                            textureCanvas.oesTexture.Bind();
                            textureCanvas.oesTexture.Draw();
                            android.opengl.GLES20.glFlush();
                            compositor.egl.SwapBuffers();
                            Error("COMPOSITOR THREAD: drawn texture");
                        }
                        compositor.egl.ReleaseCurrent();
                    }
                    compositor.egl.TryDetachFromSurfaceTexture();
                }
            } else {
                Error("COMPOSITOR THREAD: canvas is writing");
            }
            synchronized (textureCanvas.isWriting) {
                textureCanvas.isReading.set(false);
            }
        }
        Error("COMPOSITOR THREAD: drawn textures");
    }

    public void Start() {
        compositor.thread.Start();
        compositor.thread.PostAndWait(() -> {
            compositor.egl.TryCreateHighest(8, 8, 8, 0, 16, 0);
            // ignore failure
        });
        compositor.thread.SetRenderMode(true);
        canvas.thread.Start();
        canvas.thread.PostAndWait(() -> {
            if (canvas.egl.TryCreate(8, 8, 8, 0, 16, 0, compositor.egl.glesContextVersionEnum, compositor.egl.eglContext)) {
                Error("creating gl resources");
                textureCanvasList.add(new TextureCanvas());
                Error("created gl resources");
            }
        });
        compositor.thread.SetRenderMode(true);
    }

    public void Resume(@NonNull SurfaceTexture surface) {
        this.surface = surface;
        compositor.thread.SetRenderMode(true);
        canvas.thread.SetRenderMode(true);
    }

    public void Pause() {
        canvas.thread.SetRenderMode(false);
        compositor.thread.SetRenderMode(false);
    }

    public void Stop() {
        canvas.thread.SetRenderMode(false);
        canvas.thread.PostAndWait(() -> {
            canvas.egl.TryDestroy();
            // ignore failure
        });
        canvas.thread.Stop();
        compositor.thread.SetRenderMode(false);
        compositor.thread.PostAndWait(() -> {
            Error("disposing gl resources");
            for (TextureCanvas textureCanvas : textureCanvasList) {
                if (textureCanvas.IsCreated()) {
                    textureCanvas.Destroy();
                }
            }
            Error("disposed");
            compositor.egl.TryDestroy();
            // ignore failure
        });
        compositor.thread.Stop();
    }

    public void PauseRender() {
        canvas.thread.SetRenderMode(false);
        compositor.thread.SetRenderMode(false);
    }

    public void ResumeRender() {
        canvas.thread.SetRenderMode(true);
        compositor.thread.SetRenderMode(true);
    }
}
