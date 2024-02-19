package XUI.Platform.AndroidInternal;

import android.opengl.GLES20;
import android.util.Log;

public class GLErrorLog {
    public static void Error(String op) {
        Log.e("GL", op);
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error == GLES20.GL_NO_ERROR) {
            return;
        }
        Error(op + ": glError " + error);
        throw new RuntimeException(op + ": glError " + error);
    }
}
