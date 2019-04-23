package com.example.talkingplayer;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SensorsActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    @BindView(R.id.fab_sensors)
    FloatingActionButton fab_sensors;
    private SensorManager sensorManager;
    private float[] accelerometerReading;
    private float[] magnetometerReading;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_sensors);
        ButterKnife.bind(this);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        fab_sensors.setOnClickListener(this);
    }

    private void addDeviceOrientationSensor() {
        int id = getResources().getIdentifier("orientation_status", "id", getPackageName());
        TextView tvStatus = findViewById(id);

        if (tvStatus != null) {
            if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
                    && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && tvStatus != null) {
                tvStatus.setText("status: Available");
                tvStatus.setTextColor(getResources().getColor(R.color.green));
            } else {
                tvStatus.setText("status: Unavailable");
                tvStatus.setTextColor(getResources().getColor(R.color.red));
            }
        } else throw new IllegalStateException("Orientation status Textview found null");

    }


    @Override
    protected void onResume() {
        super.onResume();
        addSensor(Sensor.TYPE_MAGNETIC_FIELD);
        addSensor(Sensor.TYPE_GYROSCOPE);
        addSensor(Sensor.TYPE_ACCELEROMETER);
        addDeviceOrientationSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void addSensor(int sensorType) {
        String sensorName = getSensorName(sensorType);
        int id = getResources().getIdentifier(sensorName + "_status", "id", getPackageName());
        TextView tvStatus = findViewById(id);
        if (tvStatus != null) {
            if (checkSensorAvailability(sensorType)) {
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                tvStatus.setText("status: Available");
                tvStatus.setTextColor(getResources().getColor(R.color.green));
            } else {
                tvStatus.setText("status: Unavailable");
                tvStatus.setTextColor(getResources().getColor(R.color.red));
            }
        }
    }


    private boolean checkSensorAvailability(int sensorType) {
        if (sensorManager != null)
            return sensorManager.getDefaultSensor(sensorType) != null;
        else throw new IllegalStateException("Sensor Manager is not initiated");
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int numberOfValuesReturned = sensorEvent.values.length;
        if (numberOfValuesReturned != 0) {
            for (int i = 0; i < numberOfValuesReturned; i++) {
                String name = getSensorName(sensorEvent.sensor.getType());
                int id = getResources().getIdentifier(name + "_value_" + i, "id", getPackageName());
                TextView tv = findViewById(id);
                if (tv != null)
                    tv.setText(String.valueOf(sensorEvent.values[i]));
            }
        }

        if (sensorEvent.sensor.getType() == 1) {
            accelerometerReading = sensorEvent.values;
            calculateOrientation();
        } else if (sensorEvent.sensor.getType() == 2) {
            magnetometerReading = sensorEvent.values;
            calculateOrientation();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        String name = getSensorName(sensor.getType());
        int id = getResources().getIdentifier(name + "_accuracy", "id", getPackageName());
        TextView tv = findViewById(id);
        if (tv != null)
            tv.setText("Current Accuracy: " + getSensorAccuracy(i));
    }

    private String getSensorName(int sensorType) {
        switch (sensorType) {
            case 1:
                return "accelerometer";
            case 2:
                return "magnetometer";
            case 4:
                return "gyroscope";
            default:
                return null;
        }
    }


    private String getSensorAccuracy(int sensorAccuracy) {
        switch (sensorAccuracy) {
            case 0:
                return "Unreliable";
            case 1:
                return "Low";
            case 2:
                return "Medium";
            case 3:
                return "High";
            default:
                return "Unknown";
        }
    }

    private void calculateOrientation() {
        if (accelerometerReading != null && magnetometerReading != null) {
            final float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);

            // Express the updated rotation matrix as three orientation angles.
            final float[] orientationAngles = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            for (int i = 0; i < 3; i++) {
                int id = getResources().getIdentifier("orientation_value_" + i, "id", getPackageName());
                TextView tv = findViewById(id);
                if (tv != null)
                    tv.setText(String.valueOf(orientationAngles[i]));
            }
        }
    }

    @Override
    public void onClick(View view) {
        //start Another Activity
//       startActivity(new Intent(this, CubeActivity.class));
        startActivity(new Intent(this, SphereActivity.class));
        finish();
    }
}