package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.EGLContextManager.GLES_VERSION.*;

import android.opengl.*;
import android.util.Log;

public class EGLContextManager {
    // declared in EGL15 (EGL 1.5) but absent in EGL14 (EGL 1.4)
    // we assume these work in EGL14 (EGL 1.4)
    public static final int EGL_CONTEXT_MINOR_VERSION = 12539;
    public static final int EGL_CONTEXT_MAJOR_VERSION = 12440;

    int[] config = new int[15];

    EGLDisplay eglDisplay;
    EGLConfig eglConfig;
    EGLContext eglContext;
    EGLSurface eglSurface;
    int[] eglVersion = new int[2];
    int[] glesContextVersion = new int[2];
    int[] eglNumConfigs = new int[1];
    int[] tmp_value = new int[1];
    int[] attrib_list = new int[5];

    GLES_VERSION glesContextVersionEnum = GLES_VERSION_UNDEFINED;

    public EGLContextManager() {
        config[0] = EGL14.EGL_RED_SIZE;
        config[2] = EGL14.EGL_GREEN_SIZE;
        config[4] = EGL14.EGL_BLUE_SIZE;
        config[6] = EGL14.EGL_ALPHA_SIZE;
        config[8] = EGL14.EGL_DEPTH_SIZE;
        config[10] = EGL14.EGL_STENCIL_SIZE;
        config[12] = EGL14.EGL_RENDERABLE_TYPE;
        config[14] = EGL14.EGL_NONE;

        attrib_list[0] = EGL_CONTEXT_MAJOR_VERSION;
        attrib_list[2] = EGL_CONTEXT_MINOR_VERSION;
        attrib_list[4] = EGL14.EGL_NONE;
    }

    public enum GLES_VERSION {
        GLES_VERSION_UNDEFINED,
        GLES_VERSION_1_0,
        GLES_VERSION_2_0,
        GLES_VERSION_3_0,
        GLES_VERSION_3_1,
        GLES_VERSION_3_2,
    }

    public boolean TryCreateHighest(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
        return TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, GLES_VERSION_3_2)
            || TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, GLES_VERSION_3_1)
            || TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, GLES_VERSION_3_0)
            || TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, GLES_VERSION_2_0)
            || TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, GLES_VERSION_1_0)
        ;
    }

    public boolean TryCreate(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize, GLES_VERSION version) {
        return TryCreate(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, version, EGL14.EGL_NO_CONTEXT);
    }

    public boolean TryCreate(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize, GLES_VERSION version, EGLContext shared) {
        TryDestroy();
        boolean ret = TryCreateInternal(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, version, shared);
        if (!ret) {
            TryDestroy();
        }
        return ret;
    }
    private boolean TryCreateInternal(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize, GLES_VERSION version, EGLContext shared) {
        if (version == GLES_VERSION_UNDEFINED) {
            return TryCreateHighest(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize);
        }
        glesContextVersionEnum = version;
        switch (version) {
            case GLES_VERSION_1_0:
                glesContextVersion[0] = 1;
                glesContextVersion[1] = 0;
                config[13] = EGL14.EGL_OPENGL_ES_BIT;
                break;
            case GLES_VERSION_2_0:
                glesContextVersion[0] = 2;
                glesContextVersion[1] = 0;
                config[13] = EGL14.EGL_OPENGL_ES2_BIT;
                break;
            case GLES_VERSION_3_0:
                glesContextVersion[0] = 3;
                glesContextVersion[1] = 0;
                config[13] = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
                break;
            case GLES_VERSION_3_1:
                glesContextVersion[0] = 3;
                glesContextVersion[1] = 1;
                config[13] = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
                break;
            case GLES_VERSION_3_2:
                glesContextVersion[0] = 3;
                glesContextVersion[1] = 2;
                config[13] = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
                break;
            default:
                return false;
        }
        config[1] = redSize;
        config[3] = greenSize;
        config[5] = blueSize;
        config[7] = alphaSize;
        config[9] = depthSize;
        config[11] = stencilSize;

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            return false;
        }

        int[] maj = new int[1];
        int[] min = new int[1];

        if (!EGL14.eglInitialize(eglDisplay, maj, 0, min, 0)) {
            return false;
        }

        eglVersion[0] = maj[0];
        eglVersion[1] = min[0];

        Log.e("EGL", "EGL initialized to " + maj[0] + "." + min[0]);

        if (!EGL14.eglChooseConfig(eglDisplay, config, 0, null, 0, 0, eglNumConfigs, 0)) {
            return false;
        }

        if (eglNumConfigs[0] < 0) {
            return false;
        }

        EGLConfig[] configs = new EGLConfig[eglNumConfigs[0]];
        int[] numConfigs = new int[eglNumConfigs[0]];

        if (!EGL14.eglChooseConfig(eglDisplay, config, 0, configs, 0, eglNumConfigs[0], numConfigs, 0)) {
            return false;
        }

        boolean config_found = false;
        for (EGLConfig value : configs) {
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_RED_SIZE, tmp_value, 0) || tmp_value[0] != redSize) {
                continue;
            }
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_GREEN_SIZE, tmp_value, 0) || tmp_value[0] != greenSize) {
                continue;
            }
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_BLUE_SIZE, tmp_value, 0) || tmp_value[0] != blueSize) {
                continue;
            }
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_ALPHA_SIZE, tmp_value, 0) || tmp_value[0] != alphaSize) {
                continue;
            }
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_DEPTH_SIZE, tmp_value, 0) || tmp_value[0] != depthSize) {
                continue;
            }
            if (!EGL14.eglGetConfigAttrib(eglDisplay, value, EGL14.EGL_STENCIL_SIZE, tmp_value, 0) || tmp_value[0] != stencilSize) {
                continue;
            }
            config_found = true;
            eglConfig = value;
            break;
        }

        if (!config_found) {
            return false;
        }

        attrib_list[1] = glesContextVersion[0];
        attrib_list[3] = glesContextVersion[1];

        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, shared, attrib_list, 0);

        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            return false;
        }

        Log.e("EGL", "EGL initialized OpenGL ES context version " + glesContextVersion[0] + "." + glesContextVersion[1]);

        return true;
    }

    boolean is_current = false;
    long current_thread = 0;

    public boolean MakeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            return false;
        }
        current_thread = Thread.currentThread().getId();
        is_current = true;
        return true;
    }

    public boolean ReleaseCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            return false;
        }
        current_thread = 0;
        is_current = false;
        return true;
    }

    public boolean IsCurrent() {
        return is_current && current_thread == Thread.currentThread().getId();
    }

    public boolean SwapBuffers() {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    public boolean HasDisplay() {
        return eglDisplay != null && eglDisplay != EGL14.EGL_NO_DISPLAY;
    }

    public boolean HasContext() {
        return eglContext != null && eglContext != EGL14.EGL_NO_CONTEXT;
    }

    public boolean HasSurface() {
        return eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE;
    }

    public int Major() {
        return eglVersion[0];
    }

    public int Minor() {
        return eglVersion[1];
    }

    public boolean TryDestroy() {
        if (HasContext()) {
            if (HasSurface()) {
                if (!TryDetachFromSurfaceTexture()) {
                    return false;
                }
            }
            if (!EGL14.eglDestroyContext(eglDisplay, eglContext)) {
                return false;
            }
            Log.e("EGL", "EGL destroyed OpenGL ES context version " + glesContextVersion[0] + "." + glesContextVersion[0]);
            eglContext = null;
        }
        if (HasDisplay()) {
            if (!EGL14.eglTerminate(eglDisplay)) {
                return false;
            }
            eglDisplay = null;
            Log.e("EGL", "EGL uninitialized");
        }
        return true;
    }

    public boolean TryAttachToSurfaceTexture(android.graphics.SurfaceTexture surfaceTexture) {
        if (!HasContext()) {
            return false;
        }
        // attrib list cannot be null
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, new int[]{EGL14.EGL_NONE}, 0);
        return eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE;
    }

    public boolean TryDetachFromSurfaceTexture() {
        if (!HasContext()) {
            return false;
        }
        if (HasSurface()) {
            if (!EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                return false;
            }
        }
        eglSurface = null;
        return true;
    }
}
