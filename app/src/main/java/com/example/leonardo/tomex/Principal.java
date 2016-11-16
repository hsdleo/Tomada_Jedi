package com.example.leonardo.tomex;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by leonardo.monteiro on 23/12/2015.
 */
public class Principal extends AppCompatActivity implements SensorEventListener{

    private ProgressDialog progressDialog;
    private Button sabre;
    private LinearLayout fundo;
    private boolean flag = true;
    MediaPlayer audio_sabre,audio_sabre_off,audio_shake1,audio_shake2,audio_shake3,audio_shake4;

    int som = 1;
    //BLUETOOTH
    final int handlerState = 0;
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private StringBuilder recDataString = new StringBuilder();
    private BluetoothConnection bluetoothConnection;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private static final String address = "98:D3:31:F6:1A:38";
    private static String address = "98:D3:31:30:38:9B";

    private SensorManager senSensorManager;
    private Sensor acelerometro;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_principal);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acelerometro = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, acelerometro , SensorManager.SENSOR_DELAY_NORMAL);

        sabre = (Button)findViewById(R.id.botao_ligar);
        fundo = (LinearLayout)findViewById(R.id.ll_principal);
        audio_sabre = MediaPlayer.create(this,R.raw.audio_on);
        audio_sabre_off = MediaPlayer.create(this,R.raw.audio_off);
        audio_shake1 = MediaPlayer.create(this,R.raw.audio_shake1);
        audio_shake2 = MediaPlayer.create(this,R.raw.audio_shake2);
        audio_shake3 = MediaPlayer.create(this,R.raw.audio_shake3);
        audio_shake4 = MediaPlayer.create(this,R.raw.audio_shake4);
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getStringExtra("MAC") != null) {
                address = intent.getStringExtra("MAC");
                Log.d("MAC",address);
            }
        }
        setProgressBarIndeterminateVisibility(true);
        progressDialog = ProgressDialog.show(Principal.this,
                "Conectando", "Aguarde!");




        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                            // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("\n");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string

                        }

                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        //Metodo_classificacao(false);
                    }
                }

        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
       bluetoothConnection = new BluetoothConnection(device);

    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
                Log.i("porra",String.valueOf(speed));
                if (speed > SHAKE_THRESHOLD) {
                    if (!flag) {
                        switch (som) {

                            case 1:
                                audio_shake1.start();
                                som = 2;
                                break;
                            case 2:
                                audio_shake2.start();
                                som = 3;
                                break;
                            case 3:
                                audio_shake3.start();
                                som = 4;
                                break;
                            case 4:
                                audio_shake1.start();
                                som = 1;
                                break;
                        }
                    }
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class BluetoothConnection extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        // Unique UUID for this application, you may use different

        private final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        public BluetoothConnection(BluetoothDevice device) {

            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;

            //now make the socket connection in separate thread to avoid FC
            Thread connectionThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Always cancel discovery because it will slow down a connection
                    //btAdapter.cancelDiscovery();
                    // Make a connection to the BluetoothSocket
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        mmSocket.connect();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getBaseContext(), "May The Force Be With You", Toast.LENGTH_SHORT).show();
                            }
                        });


                    } catch (IOException e) {
                        //connection to device failed so close the socket
                        try {
                            mmSocket.close();
                            Intent intent = new Intent(Principal.this, MainActivity.class);
                            intent.putExtra("erro_bt", "1");
                            startActivity(intent);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    } finally {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //Loading.setVisibility(View.GONE);
                                setProgressBarIndeterminateVisibility(false);
                                progressDialog.dismiss();
                            }
                        });
                    }
                    bluetoothConnection.start();
                }
            });
            connectionThread.start();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {

                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void send(String message) {
            if (mmSocket != null && mmSocket.isConnected()) {

                byte[] msgBuffer = message.getBytes();
                try {
                    mmOutStream.write(msgBuffer);
                    Log.e("logE", "Int" +msgBuffer);
                } catch (IOException e) {
                    Log.d("logE", "...Error data send: " + e.getMessage() + "...");
                }
            }

        }



        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void Ligar(View view) {

        //Toast.makeText(getBaseContext(), "Ativou a Tomada", Toast.LENGTH_SHORT).show();
        bluetoothConnection.send("L");
        if(flag){
           // sabre.setBackgroundResource(R.mipmap.light_on);
            fundo.setBackgroundResource(R.mipmap.yoda4);
            audio_sabre.start();
            Toast.makeText(getBaseContext(), "You Have The Force", Toast.LENGTH_SHORT).show();
            flag = false;
        }
        else{
            //sabre.setBackgroundResource(R.mipmap.light_off);
            fundo.setBackgroundResource(R.mipmap.yoda3);
            audio_sabre_off.start();
            flag = true;
        }


    }


    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        bluetoothConnection.cancel();
        Intent a = new Intent(Principal.this,MainActivity.class);
        startActivity(a);

    }

}
