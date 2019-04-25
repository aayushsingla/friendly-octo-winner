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
    private final static String TAG = SphereRenderer.class.getSimpleName();
    public Context context;
    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private volatile float mAngle;
    private volatile float mScaleFactor = 1.0f;
    private float ratio;
    private SphereBox sphereBox;
    private NightSky nightSky;

    public SphereRenderer(Context context) {
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
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        ratio = (float) width / height;

    }

    public static int loadVertexShader(String vertexShader) {
        // Load in the vertex shader.
        int mVertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (mVertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(mVertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(mVertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(mVertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(mVertexShaderHandle);
                mVertexShaderHandle = 0;

            }

            if (mVertexShaderHandle == 0) {
                throw new RuntimeException("Error creating vertex shader.");
            }
        }
        return mVertexShaderHandle;
    }

    public static int loadFragmentShader(String fragmentShader) {
        // Load in the vertex shader.
        int mFragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (mFragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(mFragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(mFragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(mFragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(TAG, GLES20.glGetShaderInfoLog(mFragmentShaderHandle));
                GLES20.glDeleteShader(mFragmentShaderHandle);
                mFragmentShaderHandle = 0;
            }
        }

        if (mFragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }
        return mFragmentShaderHandle;
    }

    public static int linkVertexAndFragmentShaders(int mVertexShaderHandle, int mFragmentShaderHandle) {
        // Create a program object and store the handle to it.
        int mProgramHandle = GLES20.glCreateProgram();

        if (mProgramHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(mProgramHandle, mVertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(mProgramHandle, mFragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(mProgramHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(mProgramHandle, 1, "a_Color");
            GLES20.glBindAttribLocation(mProgramHandle, 2, "a_Normal");
            GLES20.glBindAttribLocation(mProgramHandle, 3, "a_TexCoordinate");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(mProgramHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(mProgramHandle);
                mProgramHandle = 0;
            }
        }

        if (mProgramHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }
        return mProgramHandle;
    }

    public static void checkGLError(String TAG, String where) {
        int errorCode = GLES20.glGetError();
        if (errorCode == 0)
            Log.d(TAG, where + ": " + errorCode);
        else {
            Log.e(TAG, where + ": " + errorCode + GLU.gluErrorString(errorCode));
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_DITHER);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        nightSky = new NightSky(context);
        sphereBox = new SphereBox(context);
        checkGLError(TAG, "initiation sphere box");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

//         Bind Attributes
        setUpViewMatrix();
        checkGLError(TAG, "View Matrix Setup");
        nightSky.draw(mModelMatrix, mViewMatrix, mProjectionMatrix);
        sphereBox.draw(mModelMatrix, mViewMatrix, mProjectionMatrix);
    }

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float mAngle) {
        this.mAngle = mAngle;
    }

    public void setZoom(float mScaleFactor) {
        this.mScaleFactor = mScaleFactor;
    }

    private void setUpViewMatrix() {
        if (ratio > 1) {
            final float left = -ratio / mScaleFactor;
            final float right = ratio / mScaleFactor;
            final float bottom = -1.0f / mScaleFactor;
            final float top = 1.0f / mScaleFactor;
            final float near = 1.0f;
            final float far = 10.0f;
            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        } else {
            final float top = 1 / (ratio * mScaleFactor);
            final float bottom = -1 * (top);
            final float left = -1.0f / mScaleFactor;
            final float right = 1.0f / mScaleFactor;
            final float near = 1.0f;
            final float far = 10.0f;
            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        }
        checkGLError(TAG, "Projection Matrix Setup");


        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 3.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = +2.0f;
        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setRotateM(mModelMatrix, 0, mAngle, 0.0f, 1.0f, 0.0f);
    }


}
