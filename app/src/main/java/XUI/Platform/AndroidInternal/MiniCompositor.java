package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.Surface;

import java.util.concurrent.CopyOnWriteArrayList;

/** @noinspection Anonymous2MethodRef, Convert2Lambda */
public class MiniCompositor {
    EGLRenderThread compositor = new EGLRenderThread();

    public int mWidth = 0, mHeight = 0;

    SurfaceTexture surface;

    public MiniCompositor() {
        compositor.thread.SetOnDoFrame(new RunnableLong() {
            @Override
            public void run(long timeNanos) {
                MiniCompositor.this.OnCompositeFrame(timeNanos);
            }
        });
    }

    int g = 0;

    CopyOnWriteArrayList<TextureCanvas> textureCanvasList = new CopyOnWriteArrayList<>();

    long render_frame = 0;
    private void OnCompositeFrame(long timeNanos) {
        if (surface == null || mWidth == 0 || mHeight == 0) {
            return;
        }
        if (!compositor.egl.HasSurface()) {
            if (!compositor.egl.TryAttachToSurfaceTexture(surface)) {
                Error("failed to attach to surface texture");
                return;
            }
            Error("attached to surface texture");
            if (!compositor.egl.MakeCurrent()) {
                Error("failed to make egl context current");
                return;
            }
            Error("attached to egl context");
        } else if(!compositor.egl.HasContext()) {
            if (!compositor.egl.MakeCurrent()) {
                Error("failed to make egl context current");
                return;
            }
            Error("attached to egl context");
        }

        for (TextureCanvas textureCanvas : textureCanvasList) {
            int textureState = textureCanvas.textureState.get();
            final int textureStateReal = textureState;
            if (textureState == TextureCanvas.TEXTURE_STATE_INVALIDATE) {
                if (!textureCanvas.IsCreated()) {
                    textureCanvas.Create();
                }

                textureCanvas.Resize(mWidth, mHeight);

                Canvas glCanvas = textureCanvas.AcquireCanvas();
                if (glCanvas != null) {
                    glCanvas.drawARGB(255, 255, g, 0);
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setTextSize(80);
                    paint.setColor(android.graphics.Color.BLUE);
                    glCanvas.drawText("draw A 255 R 255 G " + g + " B 0", 0, 80, paint);
                    paint.setTextSize(60);
                    glCanvas.drawText("compositor render frame: " + render_frame, 0, 160, paint);
                    glCanvas.drawText("texture render frame:         " + textureCanvas.surface_frame, 0, 220, paint);
                    if (g == 255) {
                        g = 0;
                    } else {
                        g++;
                    }
                    GLES20.glFlush();
                    textureCanvas.textureState.set(TextureCanvas.TEXTURE_STATE_PENDING);
                    // the texture will become available at the next frame
                    textureCanvas.surface.unlockCanvasAndPost(glCanvas);
                } else {
                    // if we fail then draw the previous texture and try again next frame
                    textureState = TextureCanvas.TEXTURE_STATE_CAN_DRAW;
                }
            }
            if (textureState == TextureCanvas.TEXTURE_STATE_PENDING) {
                // the previous frame posted a texture but we have not received it yet
                // draw previous texture until we obtain a new one
                textureState = TextureCanvas.TEXTURE_STATE_CAN_DRAW;
            }
            if (textureState == TextureCanvas.TEXTURE_STATE_COPY_SURFACE_TO_TEXTURE) {
                textureCanvas.oesTexture.Bind();
                textureCanvas.surfaceTexture.attachToGLContext(textureCanvas.oesTexture.mTextureID);
                textureCanvas.surfaceTexture.updateTexImage();
                textureCanvas.surfaceTexture.getTransformMatrix(textureCanvas.oesTexture.mSTMatrix);
                textureCanvas.surfaceTexture.release();
                textureCanvas.surfaceTexture = new SurfaceTexture(false);
                textureCanvas.surfaceTexture.setOnFrameAvailableListener(textureCanvas);
                textureCanvas.surface.release();
                textureCanvas.surface = new Surface(textureCanvas.surfaceTexture);
                textureState = TextureCanvas.TEXTURE_STATE_CAN_DRAW;
                textureCanvas.textureState.set(textureState);
            }
            if (textureState == TextureCanvas.TEXTURE_STATE_CAN_DRAW) {
                android.opengl.GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                android.opengl.GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                textureCanvas.oesTexture.Bind();
                textureCanvas.oesTexture.Draw();
                android.opengl.GLES20.glFlush();
                compositor.egl.SwapBuffers();
                textureCanvas.surface_frame++;
                if (textureStateReal != TextureCanvas.TEXTURE_STATE_PENDING) {
                    textureCanvas.Resize(mWidth, mHeight);
                    Canvas glCanvas = textureCanvas.AcquireCanvas();
                    if (glCanvas != null) {
                        // draw and post the next frame
                        glCanvas.drawARGB(255, 255, g, 0);
                        android.graphics.Paint paint = new android.graphics.Paint();
                        paint.setTextSize(80);
                        paint.setColor(android.graphics.Color.BLUE);
                        glCanvas.drawText("draw A 255 R 255 G " + g + " B 0", 0, 80, paint);
                        paint.setTextSize(60);
                        glCanvas.drawText("compositor render frame: " + render_frame, 0, 160, paint);
                        glCanvas.drawText("texture render frame:         " + textureCanvas.surface_frame, 0, 220, paint);
                        if (g == 255) {
                            g = 0;
                        } else {
                            g++;
                        }
                        GLES20.glFlush();
                        textureCanvas.textureState.set(TextureCanvas.TEXTURE_STATE_PENDING);
                        // the texture will become available at the next frame
                        textureCanvas.surface.unlockCanvasAndPost(glCanvas);
                    } else {
                        // if we fail to lock the canvas, try again next frame
                        textureCanvas.textureState.set(TextureCanvas.TEXTURE_STATE_INVALIDATE);
                    }
                }
            }
        }
        render_frame++;
    }

    public void Start() {
        compositor.thread.Start();
        compositor.thread.PostAndWait(new Runnable() {
            @Override
            public void run() {
                compositor.egl.TryCreateHighest(8, 8, 8, 0, 16, 0);
                // ignore failure
                Error("creating gl resources");
                textureCanvasList.add(new TextureCanvas());
                Error("created gl resources");
            }
        });
        compositor.thread.SetRenderMode(true);
    }

    public void Resume(SurfaceTexture surface) {
        this.surface = surface;
        compositor.thread.SetRenderMode(true);
    }

    public void Pause() {
        compositor.thread.SetRenderMode(false);
    }

    public void Stop() {
        compositor.thread.SetRenderMode(false);
        compositor.thread.PostAndWait(new Runnable() {
            @Override
            public void run() {
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
            }
        });
        compositor.thread.Stop();
    }

    public void PauseRender() {
        compositor.thread.SetRenderMode(false);
    }

    public void ResumeRender() {
        compositor.thread.SetRenderMode(true);
    }
}
