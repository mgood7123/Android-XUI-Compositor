package XUI.Platform.AndroidInternal;

import static XUI.Platform.AndroidInternal.GLErrorLog.Error;
import static XUI.Platform.AndroidInternal.GLErrorLog.checkGlError;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OESTexture {

    int FLOAT_SIZE_BYTES = 4;
    int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    float[] mTriangleVerticesData = {
            // X,     Y,     Z,     U,     V
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f
    };

    FloatBuffer mTriangleVertices;

    String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";
    float[] mMVPMatrix = new float[16];

    float[] mSTMatrix = new float[16];
    int mTextureID;
    int muMVPMatrixHandle;
    int muSTMatrixHandle;
    int maPositionHandle;

    int maTextureHandle;
    GLProgram mProgram = new GLProgram();

    int[] textures = new int[1];

    public void Create() {
        android.opengl.Matrix.setIdentityM(mMVPMatrix, 0);
        android.opengl.Matrix.setIdentityM(mSTMatrix, 0);

        mTriangleVertices = ByteBuffer.allocateDirect(
                        mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        mProgram.Create(mVertexShader, mFragmentShader);
        if (mProgram.program == 0) {
            Error("failed to create program");
            return;
        }

        maPositionHandle = android.opengl.GLES20.glGetAttribLocation(mProgram.program, "aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = android.opengl.GLES20.glGetAttribLocation(mProgram.program, "aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = android.opengl.GLES20.glGetUniformLocation(mProgram.program, "uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = android.opengl.GLES20.glGetUniformLocation(mProgram.program, "uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        android.opengl.GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        Bind();
        android.opengl.GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    public void Bind() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
    }

    public void Unbind() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void Draw() {
        GLES20.glUseProgram(mProgram.program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        android.opengl.Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void Destroy() {
        mProgram.Destroy();
        android.opengl.GLES10.glDeleteTextures(1, textures, 0);
    }
}
