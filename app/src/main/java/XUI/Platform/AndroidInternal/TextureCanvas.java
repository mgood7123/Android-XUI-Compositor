package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Build;

public class TextureCanvas {
    OESTexture oesTexture = new OESTexture();
    SurfaceTexture surfaceTexture;
    android.view.Surface surface;

    void Create() {
        oesTexture.Create();
        Error("creating surface texture");
        surfaceTexture = new SurfaceTexture(false);
    }

    void Resize(int width, int height) {
        surfaceTexture.setDefaultBufferSize(width, height);
    }

    public Canvas BindOesTextureAndAcquireCanvas() {
        Error("attaching to texture");
        oesTexture.Bind();
        // despite the documentation, a `OpenGL ES texture object`
        // is not actually created via this call, it is simply binded
        //
        //https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/libs/nativedisplay/surfacetexture/EGLConsumer.cpp;drc=85c7dfc9fb9b77b8978b628a4fe852fedd19bb0f;l=431
        surfaceTexture.attachToGLContext(oesTexture.mTextureID);
        surfaceTexture.getTransformMatrix(oesTexture.mSTMatrix);
        Error("create surface");
        surface = new android.view.Surface(surfaceTexture);
        Error("lock hardware canvas");
        Canvas canvas = surface.lockHardwareCanvas();
        Error("locked hardware canvas");
        return canvas;
    }

    public void ReleaseCanvas(Canvas acquiredCanvas) {
        Error("detaching from texture");
        Error("bind OES texture");
        oesTexture.Bind();
        Error("unlock canvas and post");
        surface.unlockCanvasAndPost(acquiredCanvas);
        Error("unlocked canvas and posted");
        Error("disposing surface");
        surface.release();
        surface = null;
        Error("update tex image");
        surfaceTexture.updateTexImage();
        Error("release tex image");
        surfaceTexture.releaseTexImage();
        surfaceTexture.detachFromGLContext();
        Error("detached from texture");
    }

    void Destroy() {
        Error("disposing surface texture");
        surfaceTexture.release();
        surfaceTexture = null;
        oesTexture.Destroy();
    }

    public boolean IsCreated() {
        return surfaceTexture != null;
    }
}
