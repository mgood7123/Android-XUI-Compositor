package XUI.Platforms.Android;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import XUI.Platform.AndroidInternal.MiniCompositor;
import smallville7123.xui.compositor.MainActivity;

public class EGLView extends TextureView implements TextureView.SurfaceTextureListener {
    MiniCompositor miniCompositor;

    public EGLView(Context context) {
        super(context);
        setSurfaceTextureListener(this);
        miniCompositor = new MiniCompositor();
        MainActivity.OnPauseActions.add(new Runnable() {
            @Override
            public void run() {
                Log.e("GL", "activity pause");
                miniCompositor.PauseRender();
            }
        });
        MainActivity.OnResumeActions.add(new Runnable() {
            @Override
            public void run() {
                Log.e("GL", "activity resume");
                miniCompositor.ResumeRender();
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.e("GL", "attach to window");
        miniCompositor.Start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e("GL", "detach from window");
        miniCompositor.Stop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        miniCompositor.mWidth = width;
        miniCompositor.mHeight = height;
        miniCompositor.Resume(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        miniCompositor.mWidth = width;
        miniCompositor.mHeight = height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        miniCompositor.Pause();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
