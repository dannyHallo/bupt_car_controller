/*
 * The move control system is special. User can control left and right side movement independently.
 * That's comfortable and satisfying ... at least for controlling tanks. They are slow and they just
 * have two motors, each on each side.
 *
 * */

package com.example.thedeadwaltz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;

public class Control extends AppCompatActivity implements SensorEventListener {
    private OutputStream outputStream;

    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;

    private int angleToBeSentThisFrame = -1;
    private int angleSentLastFrame = -1;
    private int angleSendThreadDelay = 50;
    private Handler handler = null;

    Button
            power_lvl_0_btn, power_lvl_1_btn, power_lvl_2_btn, boost_btn,
            camera_config_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        outputStream = MainActivity.outputStream;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        handler = new Handler();

        //set to full screen
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        power_lvl_0_btn = (Button) findViewById(R.id.power_lvl_0_btn);
        power_lvl_1_btn = (Button) findViewById(R.id.power_lvl_1_btn);
        power_lvl_2_btn = (Button) findViewById(R.id.power_lvl_2_btn);

        camera_config_btn = (Button) findViewById(R.id.camera_config_btn);
        boost_btn = (Button) findViewById(R.id.boost_btn);

        bindButtonCommand(power_lvl_0_btn, AllCommands.BIAS_POWER_LEVEL_START + AllCommands.COMMAND_POWER_LEVEL_0_ACTIVE);
        bindButtonCommand(power_lvl_1_btn, AllCommands.BIAS_POWER_LEVEL_START + AllCommands.COMMAND_POWER_LEVEL_1_ACTIVE);
        bindButtonCommand(power_lvl_2_btn, AllCommands.BIAS_POWER_LEVEL_START + AllCommands.COMMAND_POWER_LEVEL_2_ACTIVE);
        bindButtonCommand(boost_btn, AllCommands.COMMAND_BOOST);
        bindButtonCameraDebug(camera_config_btn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendAngleThread.run();
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(sendAngleThread);
    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindButtonCommand(Button btn, final int command) {
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn.setBackgroundColor(getResources().getColor(R.color.colorDown));

                    try {
                        outputStream.write(command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

                    try {
                        outputStream.write(command + 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return false;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindButtonCameraDebug(Button btn) {
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn.setBackgroundColor(getResources().getColor(R.color.colorDown));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

                    Intent intent = new Intent();
                    intent.setClass(Control.this, CameraDebug.class);
                    startActivity(intent);
                }

                return false;
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent mSensorEvent) {
        float rotateAxis = mSensorEvent.values[2]; // 90 -> -90
        angleToBeSentThisFrame = 90 - Math.round(rotateAxis); // 0 -> 180
        if (angleToBeSentThisFrame < 0 || angleToBeSentThisFrame > 180)
            angleToBeSentThisFrame = -1 - AllCommands.BIAS_TURNING_START; // Invalid num

        angleToBeSentThisFrame += AllCommands.BIAS_TURNING_START; // BIAS_TURNING_START -> BIAS_TURNING_START + 180 OR -1
    }

    Runnable sendAngleThread = new Runnable() {
        @Override
        public void run() {
            try {
                if (angleSentLastFrame != angleToBeSentThisFrame && angleToBeSentThisFrame != -1) {
                    Log.d("SENDING ANGLE: ", String.valueOf(angleToBeSentThisFrame));
                    outputStream.write(angleToBeSentThisFrame);
                    angleSentLastFrame = angleToBeSentThisFrame;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handler.postDelayed(sendAngleThread, angleSendThreadDelay);
            }
        }
    };
}
