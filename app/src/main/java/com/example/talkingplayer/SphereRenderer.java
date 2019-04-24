package com.example.talkingplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SphereRenderer implements GLSurfaceView.Renderer {
    public final int[] vertexBuffers = new int[1];
    public final int[] indexBuffers = new int[1];
    public final int[] normalBuffers = new int[1];
    public final int[] colorBuffers = new int[1];
    public final int[] textureBuffers = new int[1];
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

    private final int mBytesPerFloat = 4;
    private final int mStrideBytes = 3 * mBytesPerFloat;
    private final int mPositionOffset = 0;
    private final int mPositionDataSize = 3;
    private final int mColorOffset = 3;
    private final int mColorDataSize = 4;
    public Context context;
    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private int programHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureHandle;
    private int mTextureDataHandle;
    private int mUniformTextureHandle;
    private int mNormalHandle;
    private int mLightPosHandle;
    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] lightSourcePosition = new float[3];

    private SphereBox sphereBox;

    public SphereRenderer(Context context) {
        lightSourcePosition[0] = 1.5f;
        lightSourcePosition[1] = 1.5f;
        lightSourcePosition[2] = 1.5f;

        this.context = context;
    }

    public static int loadTexture(final Context context, final int resourceId) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_DITHER);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        sphereBox = new SphereBox();

        loadVertexShader();
        checkGLError("Vertex Shader Attached");

        loadFragmentShader();
        checkGLError("Fragment Shader Attached");

        linkVertexAndFragmentShaders();
        checkGLError("Shaders Linked");


        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
        mTextureHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoordinate");
        mUniformTextureHandle = GLES20.glGetUniformLocation(programHandle, "u_Texture");
        mNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");
        mLightPosHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos");
        checkGLError("Handles Created");
        mTextureDataHandle = loadTexture(context, R.drawable.texture);
        checkGLError("Texture Loaded");
        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle);

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        if (ratio > 1) {
            final float left = -ratio;
            final float right = ratio;
            final float bottom = -1.0f;
            final float top = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;
            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        } else {
            final float bottom = -(1 / ratio);
            final float top = 1 / ratio;
            final float left = -1.0f;
            final float right = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;
            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        }
        checkGLError("Projection Matrix Setup");
        setUpViewMatrix();
        checkGLError("View Matrix Setup");

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

//         Bind Attributes


        GLES20.glGenBuffers(1, vertexBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sphereBox.vertexDataBuffer.capacity() * 4,
                sphereBox.vertexDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError("Vertex Buffer Binded");
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        checkGLError("position Handle enabled");


        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mUniformTextureHandle, 0);


        GLES20.glGenBuffers(1, textureBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sphereBox.textureDataBuffer.capacity() * 4,
                sphereBox.textureDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(mTextureHandle, 2,
                GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(mTextureHandle);
        checkGLError("texture Handle Enabled");

        GLES20.glGenBuffers(1, colorBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sphereBox.colorDataBuffer.capacity() * 4,
                sphereBox.colorDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError("Color Buffer Binded");
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT,
                false, 16, 0);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        checkGLError("color Handle enabled");


        GLES20.glGenBuffers(1, normalBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sphereBox.normalDataBuffer.capacity() * 4,
                sphereBox.normalDataBuffer, GLES20.GL_STATIC_DRAW);
        checkGLError("Normal Buffer Binded");
        GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT,
                false, mStrideBytes, 0);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        checkGLError("Normal Handle enabled");


        //Draw
        GLES20.glGenBuffers(1, indexBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, sphereBox.mIndexBuffer.capacity() * 2,
                sphereBox.mIndexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, SphereBox.NUM_INDICES, GLES20.GL_UNSIGNED_SHORT, 0);
        checkGLError("Elements Drawn");
    }

    private void setUpViewMatrix() {
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -1.5f;
        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f;
        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);
        GLES20.glUniform3f(mLightPosHandle, lightSourcePosition[0], lightSourcePosition[1], lightSourcePosition[2]);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    }

    private void loadVertexShader() {
        // Load in the vertex shader.
        vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;

            }

            if (vertexShaderHandle == 0) {
                throw new RuntimeException("Error creating vertex shader.");
            }

        }
    }

    private void loadFragmentShader() {
        // Load in the vertex shader.
        fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

    }

    private void linkVertexAndFragmentShaders() {
        // Create a program object and store the handle to it.
        programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            GLES20.glBindAttribLocation(programHandle, 2, "a_Normal");
            GLES20.glBindAttribLocation(programHandle, 3, "a_TexCoordinate");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }
    }

    public void checkGLError(String where) {
        int errorCode = GLES20.glGetError();
        if (errorCode == 0)
            Log.d("TAG", where + ": " + errorCode);
        else {
            Log.e("TAG", where + ": " + errorCode + GLU.gluErrorString(errorCode));
        }
    }
}
