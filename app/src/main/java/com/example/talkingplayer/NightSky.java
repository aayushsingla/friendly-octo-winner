package com.example.talkingplayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class NightSky {
    private static final short FLOATS_PER_VERTEX = 3;
    private static final short NUM_VERTICES = 4;
    private static final int BYTES_PER_FLOAT = 4;
    public FloatBuffer textureDataBuffer;
    public FloatBuffer coordinateDataBuffer;
    public ShortBuffer indicesDataBuffer;
    public FloatBuffer normalDataBuffer;
    public FloatBuffer colorDataBuffer;

    public NightSky() {

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
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f
        };

        final float[] textureCoordinateData = new float[]{
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        };

        final float[] colorData = new float[]{
                1.0f, 1.0f, 1.0f, 0.4f,
                1.0f, 1.0f, 1.0f, 0.4f,
                1.0f, 1.0f, 1.0f, 0.4f,
                1.0f, 1.0f, 1.0f, 0.4f,
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

    }


}
