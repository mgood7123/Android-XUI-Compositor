package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TextureCanvas implements SurfaceTexture.OnFrameAvailableListener {
    final OESTexture oesTexture = new OESTexture();
    SurfaceTexture surfaceTexture;
    Surface surface;
    final AtomicBoolean isWriting = new AtomicBoolean(false);
    final AtomicBoolean isReading = new AtomicBoolean(true);
    enum STATE {
        INIT,
        DRAW,
        POSTED,
        UPDATE_TEXTURE
    }

    final AtomicReference<STATE> state = new AtomicReference<>(STATE.INIT);
    Runnable runnable;

    void Create() {
        Error("creating OES texture");
        oesTexture.Create();
        Error("created OES texture");
        Error("creating surface texture");
        surfaceTexture = new SurfaceTexture(false);
        Error("created surface texture");
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    void Resize(int width, int height) {
        Error("resizing surface texture to " + width + "x" + height);
        surfaceTexture.setDefaultBufferSize(width, height);
        Error("resized surface texture to " + width + "x" + height);
    }

    public Canvas AcquireCanvas() {
        // despite the documentation, a `OpenGL ES texture object`
        // is not actually created via this call, it is simply binded
        //
        //https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/libs/nativedisplay/surfacetexture/EGLConsumer.cpp;drc=85c7dfc9fb9b77b8978b628a4fe852fedd19bb0f;l=431
        //
        // although it does create an internal EGLImage
        //
        Error("creating surface");
        surface = new Surface(surfaceTexture);
        Error("created surface");
        Error("locking hardware canvas");
        Canvas canvas = surface.lockHardwareCanvas();
        if (canvas == null) {
            Error("failed to lock hardware canvas");
        } else {
            Error("locked hardware canvas");
        }
        return canvas;
    }

    void Destroy() {
        Error("disposing surface");
        surface.release();
        surface = null;
        Error("disposed surface");
        Error("disposing surface texture");
        surfaceTexture.release();
        surfaceTexture = null;
        Error("disposed surface texture");
        Error("destroying OES texture");
        oesTexture.Destroy();
        Error("destroyed OES texture");
        state.set(STATE.INIT);
    }

    public boolean IsCreated() {
        return surfaceTexture != null;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // SurfaceTexture objects may be created on any thread.
        // updateTexImage() may only be called on the thread with the OpenGL ES context that contains the texture object.
        // The frame-available callback is called on an arbitrary thread,
        // so unless special care is taken updateTexImage() should not be called directly from the callback.
        //
        state.set(STATE.UPDATE_TEXTURE);
    }
}
