package com.example.talkingplayer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.example.talkingplayer.SphereRenderer.checkGLError;
import static com.example.talkingplayer.SphereRenderer.linkVertexAndFragmentShaders;
import static com.example.talkingplayer.SphereRenderer.loadFragmentShader;
import static com.example.talkingplayer.SphereRenderer.loadVertexShader;

public class NightSky {
    private final static String TAG = NightSky.class.getSimpleName();
    private static final int BYTES_PER_FLOAT = 4;
    private static final short BYTES_PER_SHORT = 2;
    private final int mBytesPerFloat = 4;
    private final int mStrideBytes = 3 * mBytesPerFloat;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int[] ints = new int[5];

    private FloatBuffer textureDataBuffer;
    private FloatBuffer coordinateDataBuffer;
    private ShortBuffer indicesDataBuffer;
    private FloatBuffer normalDataBuffer;
    private FloatBuffer colorDataBuffer;

    private int mProgramHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureHandle;
    private int mUniformTextureHandle;
    private int mNormalHandle;
    private int mLightPosHandle;
    private int mSkyTextureDataHandle;
    private float[] mMVPMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] lightSourcePosition = new float[]{0, 0, 0};

    public NightSky(Context context, String vertexShader, String fragmentShader) {

        final float[] coordinateData = new float[]{
                10.0f, 10.0f, -2.0f,
                -10.0f, 10.0f, -2.0f,
                -10.0f, -10.0f, -2.0f,
                10.0f, -10.0f, -2.0f
        };

        final short[] indicesData = new short[]{
                (short) 1, (short) 2, (short) 0,
                (short) 0, (short) 2, (short) 3};

        final float[] normalData = new float[]{
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f
        };

        final float[] textureCoordinateData = new float[]{
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        };

        final float[] colorData = new float[]{
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
        };
        textureDataBuffer = ByteBuffer
                .allocateDirect(textureCoordinateData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureDataBuffer.put(textureCoordinateData).position(0);

        coordinateDataBuffer = ByteBuffer
                .allocateDirect(coordinateData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        coordinateDataBuffer.put(coordinateData).position(0);

        indicesDataBuffer = ByteBuffer
                .allocateDirect(indicesData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indicesDataBuffer.put(indicesData).position(0);

        normalDataBuffer = ByteBuffer
                .allocateDirect(normalData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        normalDataBuffer.put(normalData).position(0);

        colorDataBuffer = ByteBuffer
                .allocateDirect(colorData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorDataBuffer.put(colorData).position(0);

        mProgramHandle = linkVertexAndFragmentShaders(loadVertexShader(vertexShader),
                loadFragmentShader(fragmentShader));

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        mUniformTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        checkGLError(TAG, "Handles Created");
        mSkyTextureDataHandle = SphereRenderer.loadTexture(context, R.drawable.night_sky);
        checkGLError(TAG, "Texture Loaded");

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTextureHandle);

        GLES20.glGenBuffers(5, ints, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, coordinateDataBuffer.capacity() * 4,
                coordinateDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Vertex Buffer Binded");
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        checkGLError(TAG, "position Handle enabled");


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureDataBuffer.capacity() * 4,
                textureDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(mTextureHandle, 2,
                GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(mTextureHandle);
        checkGLError(TAG, "texture Handle Enabled");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[2]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalDataBuffer.capacity() * 4,
                normalDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Normal Buffer Binded");
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        checkGLError(TAG, "Normal Handle enabled");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[3]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorDataBuffer.capacity() * 4,
                colorDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Color Buffer Binded");
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        checkGLError(TAG, "color Handle enabled");

    }

    public void draw(float[] mModelMatrix, float[] mViewMatrix, float[] mProjectionMatrix) {

        // use the mProgramHandle for which everything has been set up in init
        GLES20.glUseProgram(mProgramHandle);

        // the only operation that need to be continuously done here
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);
        GLES20.glUniform3f(mLightPosHandle, lightSourcePosition[0], lightSourcePosition[1], lightSourcePosition[2]);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[0]);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[1]);
        GLES20.glVertexAttribPointer(mTextureHandle, 2,
                GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[2]);
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[3]);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSkyTextureDataHandle);
        GLES20.glUniform1i(mUniformTextureHandle, 0);

        //Draw
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ints[4]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesDataBuffer.capacity() * 2,
                indicesDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indicesDataBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
        checkGLError(TAG, "Elements Drawn");
    }



}
