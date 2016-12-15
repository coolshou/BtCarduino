package tw.idv.coolshou.btcarduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import tw.idv.coolshou.btcarduino.Console;


import app.akexorcist.bluetotohspp.library.BluetoothSPP;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BTCarDuino";
    private BluetoothAdapter BTAdapter = null;
    public BluetoothSocket BTSocket = null;
    private BluetoothDevice BTDevice = null;
    public OutputStream outStream = null;
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Serial Port Profile(SPP) profile UUID

    //private static String address = "00:00:00:00:00:00";     // Insert your server's MAC address
    private static String address = "98:D3:31:FD:32:D0"; //<==hardcode your server's MAC address
    //public BluetoothGatt BTGatt;
    private Button btnStart;
    private Button btnStop;
    private JoystickView joystick;
    private ArrayList<DeviceItem> deviceItemList;

    public TextView tvAngle;
    public TextView tvStrength;

    private int RADIUS = 100;
    /*
    90~180  |  0~90
    --------|----------
    180~270 | 270~360
    iAngle => -Left +Right  : +foward | -backward

    * */
    public int iAngle;
    public int iStrength;

    public boolean scanning;
    public Handler handler;
    public Console console;

    public String DeviceName = "SensorTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.buttonStart);
        btnStart.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String stringToConvert = "US;";
                try {
                    byte[] theByteArray = stringToConvert.getBytes();
                    outStream.write(theByteArray);
                } catch (IOException e) {
                    Log.e(TAG, "ON btnStart click: ", e);
                }
            }
        });

        btnStop = (Button) findViewById(R.id.buttonStop);
        btnStop.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String stringToConvert = "SS;";
                try {
                    byte[] theByteArray = stringToConvert.getBytes();
                    outStream.write(theByteArray);
                } catch (IOException e) {
                    Log.e(TAG, "ON btnStop click: ", e);
                }
            }
        });

        console = new Console((TextView) findViewById(R.id.console));

        handler = new Handler();
        //handler.post(runnable);

        //JoystickView
        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setFixedCenter(true); //fix at center
        tvAngle = (TextView) findViewById(R.id.textViewAngle);
        tvStrength = (TextView) findViewById(R.id.textViewStrength);

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            private int oldAngle=0;
            private int oldStrength=0;

            @Override
            public void onMove(int angle, int strength) {
                // do whatever you want
                if (oldAngle != angle) {
                    //tvAngle.setText(angle);
                    oldAngle = angle;
                    String t="angle:" + angle ;
                    output(t);
                    iAngle = angle;
                }
                if (oldStrength != strength) {
                    //tvStrength.setText(strength);
                    oldStrength = strength;
                    String t=" strength:" + strength;
                    output(t);
                    iStrength = strength;
                }
                //handler.postDelayed(runnable, 100);
                handler.postDelayed(runnable, 50);

            }
        });

        //初始化Bluetooth adapter，透過BluetoothManager得到一個參考Bluetooth adapter
        //BluetoothManager BTManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //BTAdapter = BTManager.getAdapter();

        //初始化Bluetooth adapter
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();
        /*
        //Obtaining a List of Paired Devices
        //Log.d("DEVICELIST", "Super called for DeviceListFragment onCreate\n");
        deviceItemList = new ArrayList<DeviceItem>();
        // If there are no devices, add an item that states so. It will be handled in the view.
        if(deviceItemList.size() == 0) {
            deviceItemList.add(new DeviceItem("No Devices", "", "false"));
        }

        Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                DeviceItem newDevice= new DeviceItem(device.getName(),device.getAddress(),"false");
                deviceItemList.add(newDevice);
            }
        }
        //TODO: discover bluetooth device
        BTScan();
//        scanning = false;
*/
    }

    final Runnable runnable = new Runnable() {
        public void run() {
            // 需要背景作的事
            tvAngle.setText(Integer.toString(iAngle));
            tvStrength.setText(Integer.toString(iStrength));

            String sFB="";
            String sRL="";
            int iForward = (int) (RADIUS * Math.sin(Math.toRadians(iAngle)));
            int iRight = (int) (RADIUS * Math.cos(Math.toRadians(iAngle)));
            //output("Forward:" + iForward + " Right:"+ iRight);
            if (BTSocket.isConnected()){
                if (outStream != null) {
                    if (iForward>=0) {
                        sFB="F";
                    } else {
                        sFB="B";
                    }
                    if (iRight>=0) {
                        sRL="R";
                    } else {
                        sRL="L";
                    }
                    // forwardBack:RightLeft:iStrength;
                    String cmd = sFB+Math.abs(iForward)+":"+sRL+Math.abs(iRight)+":"+iStrength+";";
                    output(cmd);
                    try {
                        byte[] theByteArray = cmd.getBytes();
                        outStream.write(theByteArray);
                    } catch (IOException e) {
                        Log.e(TAG, "runnable send command: ", e);
                    }
                }
            }
        }
    };

    public void onDestroy() {
        super.onDestroy();
    }
    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        // Phone does not support Bluetooth so let the user know and exit.
        if(BTAdapter==null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (BTAdapter.isEnabled()) {
                output("\n...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BTAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }
    public void AlertBox( String title, String message ){
        new AlertDialog.Builder(this)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }

    public void onStart() {
        super.onStart();
        output("\n...In onStart()...");
        /*
        if(!bt.isBluetoothEnable()) {
            // Do somthing if bluetooth is disable
        } else {
            // Do something if bluetooth is already enable
        }
        */
    };
    @Override
    public void onResume() {
        super.onResume();
        if (address == "00:00:00:00:00:00") {
            AlertBox("ERROR","connect to Bluetooth device first");
        } else {
            output("\n...In onResume...\n...Attempting client connect...");

            // Set up a pointer to the remote node using it's address.
            BluetoothDevice device = BTAdapter.getRemoteDevice(address);

            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the
            //     UUID for SPP.
            try {
                BTSocket = device.createRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException e) {
                AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            BTAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            try {
                BTSocket.connect();
                output("\n...Connection established and data link opened...");
            } catch (IOException e) {
                try {
                    BTSocket.close();
                } catch (IOException e2) {
                    AlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            // Create a data stream so we can talk to server.
            output("\n...Sending message to server...");

            try {
                outStream = BTSocket.getOutputStream();
            } catch (IOException e) {
                AlertBox("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (BTSocket.isConnected()) {
            output("\n...In onPause()...");
            if (outStream != null) {
                try {
                    outStream.flush();
                } catch (IOException e) {
                    AlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
                }
            }

            try {
                BTSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
            }
        }
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }
    //回報由手機設備掃描過程中發現的LE設備
    public BluetoothAdapter.LeScanCallback DeviceLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if (DeviceName.equals(device.getName())) {
                if (BTDevice == null) {
                    BTDevice = device;
                    //TODO
                    //BTGatt = BTDevice.connectGatt(getApplicationContext(), false, GattCallback); // 連接GATT
                } else {
                    if (BTDevice.getAddress().equals(device.getAddress())) {
                        return;
                    }
                }
                output("*<small> " + device.getName() + ":" + device.getAddress() + ", rssi:" + rssi + "</small>");
            }
        }
    };
    public void BTScan() {
        //檢查設備上是否支持藍牙
        if (BTAdapter == null) {
            output("No Bluetooth Adapter");
            return;
        }

        if (!BTAdapter.isEnabled()) {
            BTAdapter.enable();
        }

        //搜尋BLE藍牙裝置
        if (scanning == false) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    scanning = false;
                    BTAdapter.stopLeScan(DeviceLeScanCallback);
                    output("Stop scanning");
                }
            }, 2000);

            scanning = true;
            BTDevice = null;

            BTAdapter.startLeScan(DeviceLeScanCallback);
            output("Start scanning");
        }
    };
    //訊息輸出到TextView
    public void output(String msg) {
        console.output(msg);
    };
    //清除TextView
    public void clear() {
        console.clear();
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        //respond to menu item selection
        switch (item.getItemId()) {
            case R.id.about:
                //TODO
                //startActivity(new Intent(this, About.class));
                output("startActivity About");
                return true;
            case R.id.option:
                //TODO
                //startActivity(new Intent(this, Option.class));
                output("startActivity Option");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
};
