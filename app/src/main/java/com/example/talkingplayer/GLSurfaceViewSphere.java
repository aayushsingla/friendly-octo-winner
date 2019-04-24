package com.example.talkingplayer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GLSurfaceViewSphere extends GLSurfaceView {
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private ScaleGestureDetector mScaleDetector;
    private SphereRenderer sphereRenderer;
    private float previousX;
    private float previousY;
    private float mScaleFactor = 1.0f;

    public GLSurfaceViewSphere(Context context) {
        super(context);
        setEGLContextClientVersion(2);

        sphereRenderer = new SphereRenderer(context);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(sphereRenderer);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        float x = e.getX();
        float y = e.getY();
        mScaleDetector.onTouchEvent(e);
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;
                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }
                sphereRenderer.setAngle(sphereRenderer.getAngle() - ((dx + dy) * TOUCH_SCALE_FACTOR));
                requestRender();
                break;
            //case MotionEvent.:
        }

        previousX = x;
        previousY = y;
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            sphereRenderer.setZoom(mScaleFactor);
            requestRender();
            invalidate();
            return true;
        }

    }

}
