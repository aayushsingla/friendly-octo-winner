package com.example.talkingplayer;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;

public class SphereBox {
    private static final short FLOATS_PER_VERTEX = 3;
    private static final short NUM_VERTICAL_BANDS = 36;
    private static final short NUM_STEPS_IN_BAND = 36;
    //first and last step will have just 1 vertices
    public static final int NUM_VERTICES = ((NUM_VERTICAL_BANDS - 2) * NUM_STEPS_IN_BAND) + 2;
    //First and last steps will have just 1 triangles.
    // So both combine to form one complete step.
    //VERTICES_FOR_TRIANGLE * NUMBER_OF_TRIANGLES_IN)SQUARE
    // * (NUMBER_OF_STEP*(NUMBER_OF_VERTICAL_BANDS-1-1))
    public static final int NUM_INDICES = 3 * 2 * ((NUM_VERTICAL_BANDS - 2) * NUM_STEPS_IN_BAND);
    private static final short BYTES_PER_FLOAT = 4;
    private static final short BYTES_PER_SHORT = 2;
    private final float[] coordinateData = new float[FLOATS_PER_VERTEX * NUM_VERTICES];
    private final short[] indexData = new short[NUM_INDICES];
    private final float[] colorData = new float[4 * NUM_VERTICES];
    public ShortBuffer mIndexBuffer = null;
    public FloatBuffer vertexDataBuffer = null;
    public FloatBuffer colorDataBuffer = null;


    public SphereBox() {
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
        for (int i = 0; i < colorData.length; i++) {
            colorData[i] = (float) Math.random();
        }
        Log.d("TAG", "Size of indicesArray= " + elementsAdded);
        intializeVertexBuffer();
        intializeColorBuffer();
        intializeIndexBuffer();
    }

    private void intializeVertexBuffer() {
        vertexDataBuffer = ByteBuffer
                .allocateDirect(coordinateData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(coordinateData).position(0);

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
}
