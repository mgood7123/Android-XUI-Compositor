package XUI.Platform.AndroidInternal;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

import static XUI.Platform.AndroidInternal.GLErrorLog.*;

public class MiniCompositor {

    EGLRenderThread compositor = new EGLRenderThread();

    EGLRenderThread canvas = new EGLRenderThread();

    int mWidth = 0, mHeight = 0;

    CopyOnWriteArrayList<TextureCanvas> textureCanvasList = new CopyOnWriteArrayList<>();

    public MiniCompositor() {
        compositor.thread.SetOnDoFrame(this::OnCompositeFrame);
        canvas.thread.SetOnDoFrame(this::OnCanvasFrame);
    }

    private void OnCanvasFrame(long timeNanos) {
        // EGL resources shared with compositor thread
        for (TextureCanvas textureCanvas : textureCanvasList) {
            if (textureCanvas.IsCreated()) {
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
                Canvas glCanvas = textureCanvas.BindOesTextureAndAcquireCanvas();
                if (glCanvas != null) {
                    Error("CANVAS THREAD: draw A 255 R 255 G 255 B 0");
                    glCanvas.drawARGB(255, 255, 255, 0);
                    textureCanvas.ReleaseCanvas(glCanvas);
                }
                GLES20.glFlush();
                if (!canvas.egl.TryDetachFromSurfaceTexture()) {
                    Error("CANVAS THREAD: failed to detach from surface texture");
                    continue;
                }
                if (!canvas.egl.ReleaseCurrent()) {
                    Error("CANVAS THREAD: failed to release current egl context");
                    continue;
                }
            }
        }
    }

    private void OnCompositeFrame(long timeNanos) {
        if (!compositor.egl.HasSurface()) {
            Error("COMPOSITOR THREAD: no EGL Surface");
            return;
        }
        Error("COMPOSITOR THREAD: got EGL Surface");
        if (!compositor.egl.MakeCurrent()) {
            Error("COMPOSITOR THREAD: failed to make egl context current");
            return;
        }
        Error("COMPOSITOR THREAD: made egl context current");

        android.opengl.GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        android.opengl.GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Error("COMPOSITOR THREAD: drawing textures");
        for (TextureCanvas textureCanvas : textureCanvasList) {
            if (!textureCanvas.IsCreated()) {
                textureCanvas.Create();
            }
            Error("COMPOSITOR THREAD: resize texture canvas");
            textureCanvas.Resize(mWidth, mHeight);
            Error("COMPOSITOR THREAD: resized texture canvas");
            Error("COMPOSITOR THREAD: drawing texture");
            textureCanvas.oesTexture.Bind();
            textureCanvas.oesTexture.Draw();
            Error("COMPOSITOR THREAD: drawn texture");
        }
        Error("COMPOSITOR THREAD: drawing textures");

        android.opengl.GLES20.glFlush();
        compositor.egl.SwapBuffers();
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
        compositor.thread.Post(() -> {
            if (compositor.egl.HasContext()) {
                compositor.egl.TryAttachToSurfaceTexture(surface);
                // ignore failure
            }
        });
        compositor.thread.SetRenderMode(true);
        canvas.thread.SetRenderMode(true);
    }

    public void Pause() {
        canvas.thread.SetRenderMode(false);
        compositor.thread.SetRenderMode(false);
        compositor.thread.PostAndWait(() -> {
            if (compositor.egl.HasContext()) {
                compositor.egl.TryDetachFromSurfaceTexture();
                // ignore failure
            }
        });
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
            if (compositor.egl.HasContext()) {
                compositor.egl.TryDetachFromSurfaceTexture();
                // ignore failure
            }
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
