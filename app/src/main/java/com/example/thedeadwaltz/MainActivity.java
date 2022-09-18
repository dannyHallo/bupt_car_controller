//my reference: https://github.com/BoldizsarZopcsak/Android-Studio-code
//this reference is simple and helpful.

/*
* This project includes connect (in MainActivity), control (in Control),
* code input system (in Codepad), and Adjust turret system (in Adjust),
* settings page is blank, since I realized that I don't need that now...
*
* This app can only connect to other bluetooth device and send special
* commands to it. Commands will always end with lowercase z, that's easier
* to receive and cut them in my esp32 device.
*
*
* ESP32 CODE WITH ARDUINO IDE
********************************************************************************************
* #include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

TaskHandle_t Task1, Task2;

//PWMs
//Recommended PWM GPIO pins on the ESP32 include 2,4,12-19,21-23,25-27,32-33

//We use inner pwm control for led to control motor speed & servo
const int servoPin = 18;
const int LtMotorPin = 33;
const int RtMotorPin = 25;

//two motors
const int IN1 = 26;
const int IN2 = 27;
const int IN3 = 14;
const int IN4 = 12;

//pump & heater for fi  re prep
const int firePrep = 22;
const int readyIndicatorLed = 2;

//fire spark
const int sparky = 23;

//pwm setups
//i dono if the l298n is good in this freq.
//resolution: eg: 2^8 = 256, in every cycle, dutyCycle could be 0 - 256
const int freq = 50;
const int servoFreq = 50;
int LtMotorChannel = 0;
int RtMotorChannel = 1;
int servoChannel = 2;
int resolution = 8;
int servoResolution = 15;


//bluetooth
BluetoothSerial SerialBT;
char receivedChar;// received value will be stored as CHAR in this variable
String command = "";
String finalCommand = "";
String lastCommand = "";

//servo pos
float fairPos = 0;
float loPos = 0;
float hiPos = 180;
float myPos;

//0 - 100
int maxSpeed = 100;
int oriSpeed;
int mySpeed;

//logic
bool codeUpdated = false;
bool enteredLaunchPad = false;
bool fLF = false;
bool fLB = false;
bool fRF = false;
bool fRB = false;
bool fLFS = true;
bool fLBS = true;
bool fRFS = true;
bool fRBS = true;
bool breakFlag = false;
bool fireFlag = true;
bool readyToFire = false;

//bluetooth recieve special sign
const char stopSign ='z';

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32test"); //Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(firePrep, OUTPUT);
  pinMode(sparky, OUTPUT);
  pinMode(readyIndicatorLed, OUTPUT);




  xTaskCreatePinnedToCore(
    codeForTask1,
    "led1Task",
    1000,
    NULL,
    1,
    &Task1,
    0);
  delay(500);  // needed to start-up task1

  xTaskCreatePinnedToCore(
    codeForTask2,
    "led2Task",
    1000,
    NULL,
    1,
    &Task2,
    1);



  ledcSetup(LtMotorChannel, freq, resolution);
  ledcAttachPin(LtMotorPin, LtMotorChannel);

  ledcSetup(RtMotorChannel, freq, resolution);
  ledcAttachPin(RtMotorPin, RtMotorChannel);

  ledcSetup(servoChannel, servoFreq, servoResolution);
  ledcAttachPin(servoPin, servoChannel);

  //servo reset
  myPos = fairPos;
  ledcWrite(servoChannel, calcuPos(myPos));


  //mySpeed ranges 0 - 256
  oriSpeed = (int)maxSpeed * 2.56;
  mySpeed = oriSpeed;
}

void codeForTask1( void * parameter )
{
  for (;;) {
    //reverse sending
    if (Serial.available()) {
      SerialBT.write(Serial.read());

    }
    CheckSerialInput();
    CheckCode();
    CannonInit();
    CutDown();
    delay(20);
  }

}

void codeForTask2( void * parameter )
{
  bool forOnce = true;
  for (;;) {
    if(fireFlag){
      digitalWrite(firePrep, HIGH);
      delay(15000);
      if(forOnce){
        readyToFire = true;
        digitalWrite(readyIndicatorLed, HIGH);
        forOnce = false;
      }
      digitalWrite(firePrep, LOW);
      delay(30000);
    }else{
      digitalWrite(firePrep, LOW);
      delay(1000);
    }
  }
}





void CheckSerialInput(){
  if (SerialBT.available()) {
    //never output SerialBT.read() before store them!
    receivedChar =(char)SerialBT.read();
    //Serial.println(received);//print on serial monitor

    if(receivedChar != stopSign){
      command += (String)receivedChar;
    }else{
      finalCommand = command;
      Serial.println(finalCommand);
      codeUpdated = true;
      command = "";
    }
  }
}

void CheckCode(){
  if(finalCommand == lastCommand){
  }else{
   if(finalCommand == "FR" && fireFlag && readyToFire){
    fireFlag = false;
    readyToFire = false;
    delay(5);
    digitalWrite(readyIndicatorLed, LOW);
    digitalWrite(firePrep, LOW);
    delay(150);
    digitalWrite(sparky, HIGH);
    delay(400);
    digitalWrite(sparky, LOW);

  }else if(finalCommand == "FR"){
    //clear the command or it will fire once it is ready
    finalCommand = "IDLE";
    lastCommand = finalCommand;
  }
  if(finalCommand == "LF"){
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, HIGH);
    fLF = true;
    fLFS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "LB"){
    digitalWrite(IN1, HIGH);
    digitalWrite(IN2, LOW);
    fLB = true;
    fLBS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RF"){
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, HIGH);
    fRF = true;
    fRFS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RB"){
    digitalWrite(IN3, HIGH);
    digitalWrite(IN4, LOW);
    fRB = true;
    fRBS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "LFS"){
    fLF = false;
    fLFS = true;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "LBS"){
    fLB = false;
    fLBS = true;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RFS"){
    fRF = false;
    fRFS = true;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RBS"){
    fRB = false;
    fRBS = true;
    lastCommand = finalCommand;
    Smooth();
  }

  if(finalCommand == "LCH"){
    mySpeed = oriSpeed * 0.2;
  }

  if(finalCommand == "CTL"){
    mySpeed = oriSpeed;
  }
//myPos gradually adds 1, value under one will lead to bad performance for my servo(mg996r analog 180 degrees)
  if(finalCommand == "UP"){
    for (; myPos <= hiPos; myPos += 1) {
      ledcWrite(servoChannel, calcuPos(myPos));
      if(breakFlag){
        breakFlag = false;
        break;
      }
      for(int i = 0; i <= 10000; i++){
        CheckSerialInput();
        if(finalCommand == "UPS"){
          breakFlag = true;
          break;
        }
      }
    }
  }
  if(finalCommand == "DN"){
    for (; myPos >= loPos; myPos -= 1) {
      ledcWrite(servoChannel, calcuPos(myPos));
      if(breakFlag){
        breakFlag = false;
        break;
      }
      for(int i = 0; i <= 10000; i++){
        CheckSerialInput();
        if(finalCommand == "DNS"){
          breakFlag = true;
          break;
        }
      }
    }
  }
  if(finalCommand == "LT"){
    digitalWrite(IN1, HIGH);
    digitalWrite(IN2, LOW);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, HIGH);
    fLB = true;
    fLBS = false;
    fRF = true;
    fRFS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RT"){
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, HIGH);
    digitalWrite(IN3, HIGH);
    digitalWrite(IN4, LOW);
    fLF = true;
    fLFS = false;
    fRB = true;
    fRBS = false;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "LTS"){
    fLB = false;
    fLBS = true;
    fRF = false;
    fRFS = true;
    lastCommand = finalCommand;
    Smooth();
  }
  if(finalCommand == "RTS"){
    fLF = false;
    fLFS = true;
    fRB = false;
    fRBS = true;
    lastCommand = finalCommand;
    Smooth();
  }
  }


}

void CutDown(){
  if(fLFS && fLBS){
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
  }
  if(fRFS && fRBS){
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, LOW);
  }
}
void Smooth(){
  if(fLF || fLB || fRF || fRB){
    for(int dutyCycle = mySpeed * 0.4; dutyCycle <= mySpeed; dutyCycle++){
     ledcWrite(LtMotorChannel, dutyCycle);
     ledcWrite(RtMotorChannel, dutyCycle);
     delay(5);
     CheckSerialInput();
     if(finalCommand != lastCommand){
       ledcWrite(LtMotorChannel, 0);
       ledcWrite(RtMotorChannel, 0);
       break;
     }
     CutDo6wn();
    }
  }else{
    ledcWrite(LtMotorChannel, 0);
    ledcWrite(RtMotorChannel, 0);
  }

}

void CannonInit(){
}

int calcuPos(int pos){
  float k = 18.2 * pos + 729;
  int i = (int)k;
  return i;
}

void loop(){
  delay(1000);
}
**********************************************************************************************
*That is the receive side code.
*Esp32 cannot send data to android app.
*
* */


package com.example.thedeadwaltz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static BluetoothSocket socket;
    public static BluetoothDevice device;

    // MAC Address of Bluetooth Module, you might have to change this for your own device
    // download "serial bluetooth" in google store if you cannot find the MAC in settings
    private final String DEVICE_ADDRESS = "C0:49:EF:CB:BD:EA";

    //serial special UUID between the phone and bluetooth, no need to change
    //this special UUID is for sending serial data between two devices, the devices have to know
    //what they gonna do
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static OutputStream outputStream;

    Button bluetooth_connect_btn;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set to full screen
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        bluetooth_connect_btn = (Button) findViewById(R.id.bluetooth_connect_btn);

        bluetooth_connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BTinit()) {
                    BTconnect();
                } else {
                    showToast("target device didn't find");
                }
            }
        });
    }

    //Initializes bluetooth module
    public boolean BTinit() {
        boolean found = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            showToast("oh..your poor device doesn't support bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return false;
            }
            startActivityForResult(enableAdapter, 0);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if (bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            showToast("pair your device first, you don't have any!");
        } else {
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                    device = iterator;
                    found = true;
                    break;
                }
            }
            if (!found) {
                showToast("pair your target device first");
            }
        }

        return found;
    }

    public boolean BTconnect() {
        boolean connected = false;

        if(socket == null){
            try {
                //Creates a socket to handle the outgoing connection
                socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
                socket.connect();
                connected = true;
                showToast("Connected");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, Control.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("UUID did't paired, try again if you tapped twice");
            }
        }else{
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, Control.class);
            startActivity(intent);
        }

        if (connected) {
            try {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return connected;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }


}
