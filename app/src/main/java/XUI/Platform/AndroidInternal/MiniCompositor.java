package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class MiniCompositor {
    EGLRenderThread compositor = new EGLRenderThread();

    //EGLRenderThread canvas = new EGLRenderThread();

    int mWidth = 0, mHeight = 0;

    SurfaceTexture surface;

    public MiniCompositor() {
        compositor.thread.SetOnDoFrame(this::OnCompositeFrame);
        //canvas.thread.SetOnDoFrame(this::OnCanvasFrame);
    }

    int g = 0;

//    private void OnCanvasFrame(long timeNanos) {
//        // EGL resources shared with compositor thread
//        for (TextureCanvas textureCanvas : textureCanvasList) {
//            if (canvas.egl.HasSurface()) continue;
//            if (textureCanvas.IsCreated()) {
//                synchronized (textureCanvas.isWriting) {
//                    if (!textureCanvas.isReading.get()) {
//                        textureCanvas.isWriting.set(true);
//                    }
//                }
//                if (!textureCanvas.isWriting.get()) {
//                    Error("CANVAS THREAD: compositor is reading");
//                    continue;
//                } else {
//                    Error("CANVAS THREAD: compositor is not reading");
//                }
//                Error("CANVAS THREAD: attach to surface texture");
//                if (!canvas.egl.TryAttachToSurfaceTexture(textureCanvas.surfaceTexture)) {
//                    Error("CANVAS THREAD: failed to attach to surface texture");
//                    continue;
//                }
//                if (!canvas.egl.MakeCurrent()) {
//                    Error("CANVAS THREAD: failed to make egl context current");
//                    continue;
//                }
//                Error("CANVAS THREAD: made egl context current");
//                Error("CANVAS THREAD: resize texture canvas");
//                textureCanvas.Resize(mWidth, mHeight);
//                Error("CANVAS THREAD: resized texture canvas");
//                Canvas glCanvas = textureCanvas.BindOesTextureAndAcquireCanvas();
//                if (glCanvas != null) {
//                    g++;
//                    if (g == 256) {
//                        g = 0;
//                    }
//                    Error("CANVAS THREAD: draw A 255 R 255 G " + g + " B 0");
//                    glCanvas.drawARGB(255, 255, g, 0);
//                    GLES20.glFlush();
//                    textureCanvas.ReleaseCanvas(glCanvas, () -> {
//                        if (!canvas.egl.TryDetachFromSurfaceTexture()) {
//                            Error("CANVAS THREAD: failed to detach from surface texture");
//                        }
//                        if (!canvas.egl.ReleaseCurrent()) {
//                            Error("CANVAS THREAD: failed to release current egl context");
//                        }
//                    });
//                } else {
//                    if (!canvas.egl.TryDetachFromSurfaceTexture()) {
//                        Error("CANVAS THREAD: failed to detach from surface texture");
//                    }
//                    if (!canvas.egl.ReleaseCurrent()) {
//                        Error("CANVAS THREAD: failed to release current egl context");
//                    }
//                }
//            }
//        }
//    }

    CopyOnWriteArrayList<TextureCanvas> textureCanvasList = new CopyOnWriteArrayList<>();
    enum STATE {
        CHOOSE_TEXTURE,
        ATTACH_SCREEN_TEXTURE_FOR_TEXTURE_INIT,
        INIT_TEXTURE,
        DRAW_TEXTURE,
        SCREEN_TEXTURE__GL_CLEAR,
        COMPOSITE_TEXTURE_TO_SCREEN_TEXTURE,
        DETACH_SCREEN_TEXTURE,
        DESTROY_TEXTURE,
    }
    final AtomicReference<STATE> state = new AtomicReference<>(STATE.CHOOSE_TEXTURE);
    final AtomicReference<TextureCanvas> currentTexture = new AtomicReference<>(null);

    private void OnCompositeFrame(long timeNanos) {
        if (surface == null || mWidth == 0 || mHeight == 0) {
            return;
        }

        STATE compositorState = state.get();
        switch(compositorState) {
            case CHOOSE_TEXTURE:
                Error("MiniCompositor state: " + compositorState.name());
                for (TextureCanvas textureCanvas : textureCanvasList) {
                    TextureCanvas.STATE textureState = textureCanvas.state.get();
                    if (textureState == TextureCanvas.STATE.INIT) {
                        currentTexture.set(textureCanvas);
                        state.set(STATE.ATTACH_SCREEN_TEXTURE_FOR_TEXTURE_INIT);
                    }
                }
                break;
            case ATTACH_SCREEN_TEXTURE_FOR_TEXTURE_INIT:
                Error("MiniCompositor state: " + compositorState.name());
                if (!compositor.egl.TryAttachToSurfaceTexture(surface)) {
                    Error("failed to attach to surface texture");
                    break;
                }
                Error("attached to surface texture");
                if (!compositor.egl.MakeCurrent()) {
                    Error("failed to make egl context current");
                    break;
                }
                Error("attached to egl context");
                state.set(STATE.INIT_TEXTURE);
                break;
            case INIT_TEXTURE:
                Error("MiniCompositor state: " + compositorState.name());
                TextureCanvas textureCanvas3 = currentTexture.get();
                TextureCanvas.STATE textureState2 = textureCanvas3.state.get();
                Error("TextureCanvas state:  " + textureState2.name());
                textureCanvas3.Create();
                textureCanvas3.Resize(mWidth, mHeight);
                textureCanvas3.state.set(TextureCanvas.STATE.DRAW);
                state.set(STATE.DRAW_TEXTURE);
                break;
            case SCREEN_TEXTURE__GL_CLEAR:
                Error("MiniCompositor state: " + compositorState.name());
                android.opengl.GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                android.opengl.GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                state.set(STATE.COMPOSITE_TEXTURE_TO_SCREEN_TEXTURE);
                break;
            case DESTROY_TEXTURE:
                Error("MiniCompositor state: " + compositorState.name());
                TextureCanvas textureCanvas2 = currentTexture.get();
                textureCanvas2.Destroy();
                currentTexture.set(null);
                state.set(STATE.DETACH_SCREEN_TEXTURE);
                break;
            case DETACH_SCREEN_TEXTURE:
                Error("MiniCompositor state: " + compositorState.name());
                if (!compositor.egl.TryDetachFromSurfaceTexture()) {
                    Error("failed to detach to surface texture");
                    break;
                }
                Error("detached from surface texture");
                if (!compositor.egl.ReleaseCurrent()) {
                    Error("failed to release egl context");
                    break;
                }
                Error("released egl context");
                state.set(STATE.CHOOSE_TEXTURE);
                break;
            case COMPOSITE_TEXTURE_TO_SCREEN_TEXTURE:
                Error("MiniCompositor state: " + compositorState.name());
                Error("drawing texture");
                TextureCanvas textureCanvas1 = currentTexture.get();
                textureCanvas1.oesTexture.Bind();
                textureCanvas1.oesTexture.Draw();
                android.opengl.GLES20.glFlush();
                compositor.egl.SwapBuffers();
                Error("drawn texture");
                state.set(STATE.DESTROY_TEXTURE);
                break;
            case DRAW_TEXTURE:
                TextureCanvas textureCanvas = currentTexture.get();
                TextureCanvas.STATE textureState = textureCanvas.state.get();
                if (textureState == TextureCanvas.STATE.POSTED) {
                    // waiting for UPDATE_TEXTURE
                    break;
                }
                switch (textureState) {
                    case DRAW:
                        Error("MiniCompositor state: " + compositorState.name());
                        Error("TextureCanvas state:  " + textureState.name());
                        Canvas glCanvas = textureCanvas.AcquireCanvas();
                        if (glCanvas != null) {
                            Error("draw A 255 R 255 G " + g + " B 0");
                            glCanvas.drawARGB(255, 255, g, 0);
                            android.graphics.Paint paint = new android.graphics.Paint();
                            paint.setTextSize(80);
                            paint.setColor(android.graphics.Color.BLUE);
                            glCanvas.drawText("draw A 255 R 255 G " + g + " B 0", 0, paint.getTextSize(), paint);
                            if (g == 255) {
                                g = 0;
                            } else {
                                g++;
                            }
                            GLES20.glFlush();
                            textureCanvas.state.set(TextureCanvas.STATE.POSTED);
                            Error("unlock canvas and post");
                            textureCanvas.surface.unlockCanvasAndPost(glCanvas);
                            Error("unlocked canvas and posted");
                        }
                        break;
                    case UPDATE_TEXTURE:
                        Error("MiniCompositor state: " + compositorState.name());
                        Error("TextureCanvas state:  " + textureState.name());
                        Error("binding OES texture");
                        textureCanvas.oesTexture.Bind();
                        Error("bound OES texture");
                        Error("attaching to OES texture");
                        textureCanvas.surfaceTexture.attachToGLContext(textureCanvas.oesTexture.mTextureID);
                        Error("attached to OES texture");
                        Error("updating tex image");
                        textureCanvas.surfaceTexture.updateTexImage();
                        Error("updated tex image");
                        textureCanvas.surfaceTexture.getTransformMatrix(textureCanvas.oesTexture.mSTMatrix);
                        state.set(STATE.SCREEN_TEXTURE__GL_CLEAR);
                        break;
                }
                break;
        }
    }

    public void Start() {
        compositor.thread.Start();
        compositor.thread.PostAndWait(() -> {
            compositor.egl.TryCreateHighest(8, 8, 8, 0, 16, 0);
            // ignore failure
            Error("creating gl resources");
            textureCanvasList.add(new TextureCanvas());
            Error("created gl resources");
        });
        compositor.thread.SetRenderMode(true);
//        canvas.thread.Start();
//        canvas.thread.PostAndWait(() -> {
//            if (canvas.egl.TryCreate(8, 8, 8, 0, 16, 0, compositor.egl.glesContextVersionEnum, compositor.egl.eglContext)) {
//                Error("creating gl resources");
//                textureCanvasList.add(new TextureCanvas());
//                Error("created gl resources");
//            }
//        });
//        canvas.thread.SetRenderMode(true);
    }

    public void Resume(@NonNull SurfaceTexture surface) {
        this.surface = surface;
        compositor.thread.SetRenderMode(true);
//        canvas.thread.SetRenderMode(true);
    }

    public void Pause() {
//        canvas.thread.SetRenderMode(false);
        compositor.thread.SetRenderMode(false);
    }

    public void Stop() {
//        canvas.thread.SetRenderMode(false);
//        canvas.thread.PostAndWait(() -> {
//            canvas.egl.TryDestroy();
//            // ignore failure
//        });
//        canvas.thread.Stop();
        compositor.thread.SetRenderMode(false);
        compositor.thread.PostAndWait(() -> {
            if (!compositor.egl.TryDetachFromSurfaceTexture()) {
                Error("failed to detach to surface texture");
            }
            Error("detached from surface texture");
            if (!compositor.egl.ReleaseCurrent()) {
                Error("failed to release egl context");
            }
            Error("released egl context");
            if (!compositor.egl.TryAttachToSurfaceTexture(surface)) {
                Error("failed to attach to surface texture");
            }
            Error("attached to surface texture");
            if (!compositor.egl.MakeCurrent()) {
                Error("failed to make egl context current");
            }
            Error("attached to egl context");
            Error("disposing gl resources");
            for (TextureCanvas textureCanvas : textureCanvasList) {
                if (textureCanvas.IsCreated()) {
                    textureCanvas.Destroy();
                }
            }
            Error("disposed");
            if (!compositor.egl.TryDetachFromSurfaceTexture()) {
                Error("failed to detach to surface texture");
            }
            Error("detached from surface texture");
            if (!compositor.egl.ReleaseCurrent()) {
                Error("failed to release egl context");
            }
            Error("released egl context");
            compositor.egl.TryDestroy();
        });
        compositor.thread.Stop();
    }

    public void PauseRender() {
//        canvas.thread.SetRenderMode(false);
        compositor.thread.SetRenderMode(false);
    }

    public void ResumeRender() {
//        canvas.thread.SetRenderMode(true);
        compositor.thread.SetRenderMode(true);
    }
}
