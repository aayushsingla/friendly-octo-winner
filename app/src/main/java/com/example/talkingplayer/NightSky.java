package com.example.talkingplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_RGB;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES30.GL_TEXTURE_WRAP_R;
import static com.example.talkingplayer.SphereRenderer.checkGLError;
import static com.example.talkingplayer.SphereRenderer.linkVertexAndFragmentShaders;
import static com.example.talkingplayer.SphereRenderer.loadFragmentShader;
import static com.example.talkingplayer.SphereRenderer.loadVertexShader;
import static javax.microedition.khronos.opengles.GL11ExtensionPack.GL_TEXTURE_CUBE_MAP;

public class NightSky {
    private final static String TAG = NightSky.class.getSimpleName();
    private static final int BYTES_PER_FLOAT = 4;
    private static final short BYTES_PER_SHORT = 2;
    private final int mBytesPerFloat = 4;
    private final int mStrideBytes = 3 * mBytesPerFloat;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int[] ints = new int[5];

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
    final String fragmentShader =
            "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a
                    // precision in the fragment shader.
                    + "uniform vec3 u_LightPos;       \n"        // The position of the light in eye space.
                    + "uniform samplerCube cubemap;   \n"       // The input texture.

                    + "varying vec3 v_Position;		  \n"        // Interpolated position for this fragment.
                    + "varying vec4 v_Color;          \n"        // This is the color from the vertex shader interpolated across the
                    // triangle per fragment.
                    + "varying vec3 v_Normal;         \n"        // Interpolated normal for this fragment.
                    + "varying vec3 v_TexCoordinate;  \n"       // Interpolated texture coordinate per fragment.
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
                    + "gl_FragColor = v_Color * diffuse * textureCube(cubemap, v_TexCoordinate); \n"
                    + "}                                                                     \n";
    private final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                    + "uniform mat4 u_MVMatrix;       \n"        // A constant representing the combined model/view matrix.

                    + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                    + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.
                    + "attribute vec3 a_Normal;       \n"        // Per-vertex normal information we will pass in.
                    + "attribute vec3 a_TexCoordinate;\n"       // Per-vertex texture coordinate information we will pass in.

                    + "varying vec3 v_Position;       \n"        // This will be passed into the fragment shader.
                    + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.
                    + "varying vec3 v_Normal;         \n"        // This will be passed into the fragment shader.
                    + "varying vec3 v_TexCoordinate;  \n"       // This will be passed into the fragment shader.
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


    public NightSky(Context context) {

        GLES20.glEnable(GLES20.GL_TEXTURE_CUBE_MAP);
        final float[] coordinateData = new float[]{
                5.0f, 5.0f, -5.0f,
                -5.0f, 5.0f, -5.0f,
                -5.0f, -5.0f, -5.0f,
                5.0f, -5.0f, -5.0f,
                5.0f, 5.0f, +5.0f,
                -5.0f, 5.0f, +5.0f,
                -5.0f, -5.0f, +5.0f,
                5.0f, -5.0f, +5.0f
        };

        final short[] indicesData = new short[]{
                (short) 1, (short) 2, (short) 0,
                (short) 0, (short) 2, (short) 3,
                (short) 0, (short) 3, (short) 4,
                (short) 4, (short) 3, (short) 7,
                (short) 4, (short) 7, (short) 5,
                (short) 5, (short) 7, (short) 6,
                (short) 5, (short) 6, (short) 1,
                (short) 1, (short) 6, (short) 2,
                (short) 1, (short) 5, (short) 0,
                (short) 0, (short) 5, (short) 4,
                (short) 2, (short) 6, (short) 3,
                (short) 3, (short) 6, (short) 7
        };

        final float[] normalData = new float[]{
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f
        };

        final float[] colorData = new float[]{
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f
        };

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
        mUniformTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "cubemap");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        checkGLError(TAG, "Handles Created");
        int[] images = new int[]{
                R.drawable.night_sky1,
                R.drawable.night_sky1,
                R.drawable.night_sky1,
                R.drawable.night_sky1,
                R.drawable.night_sky1,
                R.drawable.night_sky1};
        mSkyTextureDataHandle = loadCubeMapTexture(context, images);
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
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        checkGLError(TAG, "position Handle enabled");


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, coordinateDataBuffer.capacity() * 4,
                coordinateDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glEnableVertexAttribArray(mTextureHandle);
        checkGLError(TAG, "texture Handle Enabled");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[2]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalDataBuffer.capacity() * 4,
                normalDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Normal Buffer Binded");
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        checkGLError(TAG, "Normal Handle enabled");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[3]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorDataBuffer.capacity() * 4,
                colorDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError(TAG, "Color Buffer Binded");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        checkGLError(TAG, "color Handle enabled");

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ints[4]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesDataBuffer.capacity() * 2,
                indicesDataBuffer, GLES20.GL_STATIC_DRAW);

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
        GLES20.glVertexAttribPointer(mTextureHandle, 3,
                GLES20.GL_FLOAT, false, 12, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[2]);
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ints[3]);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, mSkyTextureDataHandle);
        GLES20.glUniform1i(mUniformTextureHandle, 0);

        //Draw
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ints[4]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesDataBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
        checkGLError(TAG, "Elements Drawn");
    }

    private int loadCubeMapTexture(Context context, int[] resourceIds) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureHandle[0]);
            // Set filtering
            GLES20.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            for (int i = 0; i < resourceIds.length; i++) {
                // Read in the resource
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceIds[i], options);
                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bitmap, 0);
                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }
            glGenerateMipmap(GL_TEXTURE_CUBE_MAP);


        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }


}
