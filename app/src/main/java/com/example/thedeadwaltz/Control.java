/*
 * The move control system is special. User can control left and right side movement independently.
 * That's comfortable and satisfying ... at least for controlling tanks. They are slow and they just
 * have two motors, each on each side.
 *
 * */

package com.example.thedeadwaltz;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class Control extends AppCompatActivity implements SensorEventListener {
    //MAC Address of Bluetooth Module
    private final String DEVICE_ADDRESS = "8C:AA:B5:93:2F:CA";
    //serial special UUID between the phone and bluetooth, no need to change
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;

    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;

    private int throttleSliderCommandSlot = -1;

    Button
            power_lvl_0_btn, power_lvl_1_btn, power_lvl_2_btn, power_lvl_3_btn, power_lvl_4_btn,
            cam_config_btn, mRightForward_btn, mRightBack_btn, mBack_btn, mTakecontrol_btn,
            mStopcontrol_btn, mLower_fuel_btn, mAdd_fuel_btn;

    String command; //string variable that will store value to be transmitted to the bluetooth module

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        //In this activity, I just take the pairing data out from MainActivity, no need to connect again,
        //in other words, if you lose connection, you have to go back to connect
        device = MainActivity.device;
        socket = MainActivity.socket;
        outputStream = MainActivity.outputStream;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        //set to full screen
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        power_lvl_0_btn = (Button) findViewById(R.id.power_lvl_0_btn);
        power_lvl_1_btn = (Button) findViewById(R.id.power_lvl_1_btn);
        power_lvl_2_btn = (Button) findViewById(R.id.power_lvl_2_btn);
        power_lvl_3_btn = (Button) findViewById(R.id.power_lvl_3_btn);
        power_lvl_4_btn = (Button) findViewById(R.id.power_lvl_4_btn);

        cam_config_btn = (Button) findViewById(R.id.cam_config_btn);

        mTakecontrol_btn = (Button) findViewById(R.id.takecontrol_btn);
        mStopcontrol_btn = (Button) findViewById(R.id.stopcontrol_btn);

        bindButton(power_lvl_0_btn, AllCommands.COMMAND_POWER_LEVEL_0_ACTIVE);
        bindButton(power_lvl_1_btn, AllCommands.COMMAND_POWER_LEVEL_1_ACTIVE);
        bindButton(power_lvl_2_btn, AllCommands.COMMAND_POWER_LEVEL_2_ACTIVE);
        bindButton(power_lvl_3_btn, AllCommands.COMMAND_POWER_LEVEL_3_ACTIVE);
        bindButton(power_lvl_4_btn, AllCommands.COMMAND_POWER_LEVEL_4_ACTIVE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindButton(Button btn, final int command) {
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent mSensorEvent) {
        float rotateAxis = mSensorEvent.values[2]; // 90 -> -90
        int angleRearranged = 90 - Math.round(rotateAxis); // 0 -> 180
        if(angleRearranged < 0 || angleRearranged > 180) angleRearranged = -1 - AllCommands.BIAS_TURNING_START; // Invalid num

        angleRearranged += AllCommands.BIAS_TURNING_START; // BIAS_TURNING_START -> BIAS_TURNING_START + 180 OR -1
        Log.d("ANGLE = ", String.valueOf(angleRearranged));

        try {
            outputStream.write(angleRearranged);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
