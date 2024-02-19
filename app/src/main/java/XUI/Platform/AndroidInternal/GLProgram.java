package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;
import static XUI.Platform.AndroidInternal.GLErrorLog.checkGlError;

import android.opengl.GLES20;

public class GLProgram {
    int program = 0;
    int vertexShader = 0;
    int fragmentShader = 0;

    public int Create(String vertexSource, String fragmentSource) {
        return createProgram(vertexSource, fragmentSource);
    }

    public void Destroy() {
        if (fragmentShader != 0) {
            android.opengl.GLES20.glDeleteShader(fragmentShader);
            fragmentShader = 0;
        }
        if (vertexShader != 0) {
            android.opengl.GLES20.glDeleteShader(vertexShader);
            vertexShader = 0;
        }
        if (program != 0) {
            android.opengl.GLES20.glDeleteProgram(program);
            program = 0;
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = android.opengl.GLES20.glCreateShader(shaderType);
        if (shader == 0) {
            Error("failed to create shader");
            return shader;
        }
        android.opengl.GLES20.glShaderSource(shader, source);
        android.opengl.GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        android.opengl.GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Error("Could not compile shader " + shaderType + ":");
            Error(GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            Error("failed to load vertex shader");
            return 0;
        }
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            Error("failed to load fragment shader");
            return 0;
        }

        program = GLES20.glCreateProgram();
        if (program == 0) {
            Error("failed to glCreateProgram");
            return program;
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Error("Could not link program: ");
            Error(GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }
}
