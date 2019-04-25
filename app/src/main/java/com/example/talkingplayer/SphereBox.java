package com.example.talkingplayer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static com.example.talkingplayer.SphereRenderer.checkGLError;
import static com.example.talkingplayer.SphereRenderer.linkVertexAndFragmentShaders;
import static com.example.talkingplayer.SphereRenderer.loadFragmentShader;
import static com.example.talkingplayer.SphereRenderer.loadVertexShader;

public class SphereBox {
    private final static String TAG = SphereBox.class.getSimpleName();
    private static final short FLOATS_PER_VERTEX = 3;
    private static final short NUM_VERTICAL_BANDS = 36;
    private static final short NUM_STEPS_IN_BAND = 36;
    //first and last step will have just 1 vertices
    private static final int NUM_VERTICES = ((NUM_VERTICAL_BANDS - 2) * NUM_STEPS_IN_BAND) + 2;
    //First and last steps will have just 1 triangles.
    // So both combine to form one complete step.
    //VERTICES_FOR_TRIANGLE * NUMBER_OF_TRIANGLES_IN)SQUARE
    // * (NUMBER_OF_STEP*(NUMBER_OF_VERTICAL_BANDS-1-1))
    private static final int NUM_INDICES = 3 * 2 * ((NUM_VERTICAL_BANDS - 2) * NUM_STEPS_IN_BAND);
    private static final short BYTES_PER_FLOAT = 4;
    private static final short BYTES_PER_SHORT = 2;
    private final int mBytesPerFloat = 4;
    private final int mStrideBytes = 3 * mBytesPerFloat;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final float[] coordinateData = new float[FLOATS_PER_VERTEX * NUM_VERTICES];
    private final float[] textureData = new float[2 * NUM_VERTICES];
    private final float[] normalData = new float[FLOATS_PER_VERTEX * NUM_VERTICES];
    private final short[] indexData = new short[NUM_INDICES];
    private final float[] colorData = new float[4 * NUM_VERTICES];
    private final int[] bufferId = new int[5];

    private ShortBuffer mIndexBuffer = null;
    private FloatBuffer vertexDataBuffer = null;
    private FloatBuffer colorDataBuffer = null;
    private FloatBuffer normalDataBuffer = null;
    private FloatBuffer textureDataBuffer = null;

    private int mProgramHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureHandle;
    private int mSphereTextureDataHandle;
    private int mUniformTextureHandle;
    private int mNormalHandle;
    private int mLightPosHandle;

    private float[] mMVPMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] lightSourcePosition = new float[]{0, 0, 0};
    final String fragmentShader =
            "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a
                    // precision in the fragment shader.
                    + "uniform vec3 u_LightPos;       \n"        // The position of the light in eye space.
                    + "uniform sampler2D u_Texture;   \n"       // The input texture.

                    + "varying vec3 v_Position;		  \n"        // Interpolated position for this fragment.
                    + "varying vec4 v_Color;          \n"        // This is the color from the vertex shader interpolated across the
                    // triangle per fragment.
                    + "varying vec3 v_Normal;         \n"        // Interpolated normal for this fragment.
                    + "varying vec2 v_TexCoordinate;  \n"       // Interpolated texture coordinate per fragment.
                    // The entry point for our fragment shader.
                    + "void main()                    \n"
                    + "{                              \n"
                    // Will be used for attenuation.
                    + "   float distance = length(u_LightPos - v_Position);                  \n"
                    // Get a lighting direction vector from the light to the vertex.
                    + "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    + "   float diffuse = max(dot(v_Normal, lightVector), 0.9);              \n"
                    // Add attenuation.
                    + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
                    // Multiply the color by the diffuse illumination level to get final output color.
                    + "diffuse = diffuse + 0.3;                                              \n"
                    + "gl_FragColor = v_Color * diffuse * texture2D(u_Texture, v_TexCoordinate); \n"
                    + "}                                                                     \n";
    private final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                    + "uniform mat4 u_MVMatrix;       \n"        // A constant representing the combined model/view matrix.

                    + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                    + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.
                    + "attribute vec3 a_Normal;       \n"        // Per-vertex normal information we will pass in.
                    + "attribute vec2 a_TexCoordinate;\n"       // Per-vertex texture coordinate information we will pass in.

                    + "varying vec3 v_Position;       \n"        // This will be passed into the fragment shader.
                    + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.
                    + "varying vec3 v_Normal;         \n"        // This will be passed into the fragment shader.
                    + "varying vec2 v_TexCoordinate;  \n"       // This will be passed into the fragment shader.
                    // The entry point for our vertex shader.
                    + "void main()                                                \n"
                    + "{                                                          \n"
                    // Transform the vertex into eye space.
                    + "   v_Position = vec3(u_MVMatrix * a_Position);             \n"
                    // Pass through the color.
                    + "   v_Color = a_Color;                                      \n"
                    // Transform the normal's orientation into eye space.
                    + "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));      \n"
                    // gl_Position is a special variable used to store the final position.
                    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                    + "   gl_Position = u_MVPMatrix * a_Position;                 \n"
                    + "   v_TexCoordinate = a_TexCoordinate;                      \n"
                    + "}                                                          \n";


    public SphereBox(Context context) {
        //Generating vertex array data
        //get angles for steps in band first

        float theta = 0f;
        float stepAngle = 2 * ((float) Math.PI / NUM_STEPS_IN_BAND);
        float[] sinTheta = new float[NUM_STEPS_IN_BAND];
        float[] cosTheta = new float[NUM_STEPS_IN_BAND];
        for (int i = 0; i < NUM_STEPS_IN_BAND; i++) {
            sinTheta[i] = (float) Math.sin(theta);
            cosTheta[i] = (float) Math.cos(theta);
            theta += stepAngle;
        }

        //hardcoding vertices for zeroth band
        coordinateData[0] = 0;
        coordinateData[1] = +1f;
        coordinateData[2] = 0;

        //divide the vertical axis of length 2 into 8 equal parts
        float stepBand = 2f / (NUM_VERTICAL_BANDS - 1);
        //start from second bandPos and go all add coordinates until bandPos >-1
        float bandPos = 1.0f - stepBand;
        //traversing through bands
        int vertices_in_current_band = 0;
        for (int band = 1; band < (NUM_VERTICAL_BANDS - 1); band++, bandPos -= stepBand) {
            float sinPhi = (float) Math.sqrt(1 - (bandPos * bandPos));
            vertices_in_current_band = (((band - 1) * NUM_STEPS_IN_BAND) + 1) * 3;
            int j = 0;
            // Coordinates for a particular band
            for (int i = 0; i < NUM_STEPS_IN_BAND * 3; i += 3) {
                coordinateData[vertices_in_current_band + i] = cosTheta[j] * sinPhi;  // X-coordinate
                coordinateData[vertices_in_current_band + i + 1] = bandPos;              // Y-coordinate
                coordinateData[vertices_in_current_band + i + 2] = sinTheta[j] * sinPhi;// Z-coordinate
                j++;
            }
        }

        //hardcoding vertices for last band
        coordinateData[(NUM_VERTICES * 3) - 3] = 0;
        coordinateData[(NUM_VERTICES * 3) - 2] = -1f;
        coordinateData[(NUM_VERTICES * 3) - 1] = 0;

        //creating indices array
        int elementsAdded = 0;
        for (int i = 0; i < (NUM_VERTICAL_BANDS - 1); i++) {
            short k1 = (short) (i * (NUM_STEPS_IN_BAND));     // beginning of current stack
            short k2 = (short) (k1 + NUM_STEPS_IN_BAND);      // beginning of next stack

            for (int j = 0; j < NUM_STEPS_IN_BAND; j++, k1++, k2++) {
                // 2 triangles per sector excluding first and last stacks
                // k1 => k2 => k1+1
                if (i == 0) {
                    indexData[elementsAdded] = ((short) 0);
                    indexData[elementsAdded + 1] = k2;
                    indexData[elementsAdded + 2] = ((short) (k2 + 1));
                    elementsAdded += 3;
                } else if (i == (NUM_VERTICAL_BANDS - 2)) {
                    indexData[elementsAdded] = k1;
                    indexData[elementsAdded + 1] = (short) (k1 + 1);
                    indexData[elementsAdded + 2] = ((short) (NUM_VERTICES - 1));
                    elementsAdded += 3;
                } else {
                    //first triangle
                    indexData[elementsAdded] = k1;
                    indexData[elementsAdded + 1] = k2;
                    indexData[elementsAdded + 2] = (short) (k1 + 1);

                    //second triangle
                    indexData[elementsAdded + 3] = (short) (k1 + 1);
                    indexData[elementsAdded + 4] = k2;
                    indexData[elementsAdded + 5] = (short) (k2 + 1);
                    elementsAdded += 6;
                }
            }
        }

        for (int i = 0; i < colorData.length; i += 4) {
            colorData[i] = 1.0f;
            colorData[i + 1] = 215f / 255f;
            colorData[i + 2] = 0.0f;
            colorData[i + 3] = 1.0f;
        }

        for (int i = 0; i < coordinateData.length; i++) {
            normalData[i] = 1.0f * coordinateData[i];
        }

        //generating texture data
        textureData[0] = 0.5f;
        textureData[1] = 0f;
        stepBand = 2f / (NUM_VERTICAL_BANDS - 1);
        //start from second bandPos and go all add coordinates until bandPos >-1
        bandPos = 1.0f - stepBand;
        for (int band = 1; band < (NUM_VERTICAL_BANDS - 1); band++, bandPos -= stepBand) {
            float sInitial = (float) (0.5f - Math.sqrt((0.5 * 0.5) - (bandPos * bandPos / 4)));
            float sFinal = (float) (0.5f + Math.sqrt((0.5 * 0.5) - (bandPos * bandPos / 4)));
            float step = 2 * (sFinal - sInitial) / NUM_STEPS_IN_BAND;
            int stepsInQuarter = NUM_STEPS_IN_BAND / 4;
            vertices_in_current_band = (((band - 1) * NUM_STEPS_IN_BAND) + 1) * 2;
            for (int i = 0; i < stepsInQuarter; i++) {
                //first quarter
                textureData[vertices_in_current_band + 2 * i] = 0.5f + (i * step);
                textureData[vertices_in_current_band + 2 * i + 1] = (1 - bandPos) / 2;
                //second quarter
                textureData[vertices_in_current_band + 2 * i + 2 * stepsInQuarter] = sFinal - (i * step);
                textureData[vertices_in_current_band + 2 * i + 1 + 2 * stepsInQuarter] = (1 - bandPos) / 2;
                //third quarter
                textureData[vertices_in_current_band + 2 * i + (2 * 2 * stepsInQuarter)] = 0.5f - (i * step);
                textureData[vertices_in_current_band + 2 * i + 1 + (2 * 2 * stepsInQuarter)] = (1 - bandPos) / 2;
                //fourth quarter
                textureData[vertices_in_current_band + 2 * i + (3 * 2 * stepsInQuarter)] = sInitial + (i * step);
                textureData[vertices_in_current_band + 2 * i + 1 + (3 * 2 * stepsInQuarter)] = (1 - bandPos) / 2;
            }
        }
        textureData[(2 * NUM_VERTICES) - 1] = 0.5f;
        textureData[(2 * NUM_VERTICES) - 2] = 1f;
        Log.e("TAG", Arrays.toString(textureData) + "  " + textureData.length);

        //Generating Normal Array data
        intializeVertexBuffer();
        intializeNormalBuffer();
        intializeColorBuffer();
        intializeIndexBuffer();
        intializeTextureDataBuffer();

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
        mSphereTextureDataHandle = SphereRenderer.loadTexture(context, R.drawable.texture);
        checkGLError(TAG, "Texture Loaded");

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTextureHandle);

        GLES20.glGenBuffers(5, bufferId, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexDataBuffer.capacity() * 4,
                vertexDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Vertex Buffer Binded");
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureDataBuffer.capacity() * 4,
                textureDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(mTextureHandle, 2,
                GLES20.GL_FLOAT, false, 8, 0);
        checkGLError(TAG, "texture Handle Enabled");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[2]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorDataBuffer.capacity() * 4,
                colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);
        checkGLError(TAG, "color Handle enabled");


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[3]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalDataBuffer.capacity() * 4,
                normalDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        checkGLError(TAG, "Normal Handle enabled");

        checkGLError(TAG, "Buffer Binding");

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferId[4]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer.capacity() * 2,
                mIndexBuffer, GLES20.GL_STATIC_DRAW);


    }

    private void intializeTextureDataBuffer() {
        textureDataBuffer = ByteBuffer
                .allocateDirect(textureData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureDataBuffer.put(textureData).position(0);

    }


    private void intializeVertexBuffer() {
        vertexDataBuffer = ByteBuffer
                .allocateDirect(coordinateData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(normalData).position(0);

    }

    private void intializeNormalBuffer() {
        normalDataBuffer = ByteBuffer
                .allocateDirect(coordinateData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        normalDataBuffer.put(coordinateData).position(0);
    }

    private void intializeColorBuffer() {
        colorDataBuffer = ByteBuffer
                .allocateDirect(colorData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorDataBuffer.put(colorData).position(0);

    }

    private void intializeIndexBuffer() {
        mIndexBuffer = ByteBuffer
                .allocateDirect(indexData.length * BYTES_PER_SHORT)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        mIndexBuffer.put(indexData).position(0);

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

        //Draw
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[0]);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[1]);
        GLES20.glVertexAttribPointer(mTextureHandle, 2,
                GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[2]);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[3]);
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSphereTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mUniformTextureHandle, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferId[4]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, SphereBox.NUM_INDICES, GLES20.GL_UNSIGNED_SHORT, 0);
        checkGLError(TAG, "Elements Drawn");
    }


}
