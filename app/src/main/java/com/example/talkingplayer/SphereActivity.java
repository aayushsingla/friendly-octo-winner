package com.example.talkingplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SphereActivity extends Activity {
    private GLSurfaceViewSphere glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphere);
        ButterKnife.bind(this);
        glSurfaceView = new GLSurfaceViewSphere(this);

        setContentView(glSurfaceView);

    }

    @Override
    protected void onResume() {   // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {   // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        glSurfaceView.onPause();
    }

}
