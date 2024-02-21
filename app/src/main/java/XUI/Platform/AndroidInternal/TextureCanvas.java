package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicInteger;

public class TextureCanvas implements SurfaceTexture.OnFrameAvailableListener {
    final OESTexture oesTexture = new OESTexture();
    SurfaceTexture surfaceTexture;
    Surface surface;
    final public static int TEXTURE_STATE_INVALIDATE = 0;
    final public static int TEXTURE_STATE_PENDING = 2;
    final public static int TEXTURE_STATE_COPY_SURFACE_TO_TEXTURE = 3;
    final public static int TEXTURE_STATE_CAN_DRAW = 4;
    final AtomicInteger textureState = new AtomicInteger(TEXTURE_STATE_INVALIDATE);
    long surface_frame = 0;
    void Create() {
        oesTexture.Create();
        surfaceTexture = new SurfaceTexture(false);
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);
    }

    void Resize(int width, int height) {
        surfaceTexture.setDefaultBufferSize(width, height);
    }

    public Canvas AcquireCanvas() {
        // despite the documentation, a `OpenGL ES texture object`
        // is not actually created via this call, it is simply binded
        //
        //https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/libs/nativedisplay/surfacetexture/EGLConsumer.cpp;drc=85c7dfc9fb9b77b8978b628a4fe852fedd19bb0f;l=431
        //
        // although it does create an internal EGLImage
        //
        Canvas canvas = surface.lockHardwareCanvas();
        if (canvas == null) {
            Error("failed to lock hardware canvas");
        }
        return canvas;
    }

    void Destroy() {
        surface.release();
        surface = null;
        surfaceTexture.release();
        surfaceTexture = null;
        oesTexture.Destroy();
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
        textureState.set(TEXTURE_STATE_COPY_SURFACE_TO_TEXTURE);
    }

    public static String stateToName(int textureState) {
        switch (textureState) {
            case TEXTURE_STATE_INVALIDATE: return "INVALIDATE";
            case TEXTURE_STATE_PENDING: return "PENDING";
            case TEXTURE_STATE_COPY_SURFACE_TO_TEXTURE: return "COPY_SURFACE_TO_TEXTURE";
            case TEXTURE_STATE_CAN_DRAW: return "CAN_DRAW";
            default: return "UNKNOWN";
        }
    }
}
