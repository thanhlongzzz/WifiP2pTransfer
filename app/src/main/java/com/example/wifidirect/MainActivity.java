package com.example.wifidirect;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.R;
import com.thanhlong.wifip2ptransfer.Listener.OnFileTransfer;
import com.thanhlong.wifip2ptransfer.Listener.OnP2PListener;
import com.thanhlong.wifip2ptransfer.WifiP2PControl;
import com.thanhlong.wifip2ptransfer.WifiP2PDevicePIN;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    Button btnSearch,btnDisconnect,btnInit,btnSend;
    EditText edPin,ed_path;
    ListView listViewDevice;
    WiFiListAdapter wiFiListAdapter;
    ArrayList<WifiP2PDevicePIN> arrDevice = new ArrayList<>();
    WifiP2PControl wifiControl;
    CheckBox cbIsSender;
    View ln_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSearch = findViewById(R.id.btnSearch);
        btnInit = findViewById(R.id.btnInit);
        cbIsSender = findViewById(R.id.cbIsSender);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        edPin = findViewById(R.id.ed_pin);
        ed_path = findViewById(R.id.ed_path);
        btnSend = findViewById(R.id.btnSend);
        ln_send = findViewById(R.id.ln_send);
        listViewDevice = findViewById(R.id.listDevice);
        wiFiListAdapter = new WiFiListAdapter(this,R.layout.row_devices,arrDevice);
        listViewDevice.setAdapter(wiFiListAdapter);
        wifiControl = new WifiP2PControl(MainActivity.this, new OnP2PListener() {
            @Override
            public void onSearching() {
                Toast.makeText(MainActivity.this, "searching !", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFoundADevice(WifiP2PDevicePIN device) {

            }

            @Override
            public void onSearchCompleted(ArrayList<WifiP2PDevicePIN> listDeviceSearched) {
                Toast.makeText(MainActivity.this, "searchDevice completed !"+listDeviceSearched.size(), Toast.LENGTH_SHORT).show();
                arrDevice = listDeviceSearched;
//                for(int i=0;i<listDeviceSearched.size();i++){
//                    if(listDeviceSearched.get(i).getPIN().equals(edPin.getText().toString())){
//                        arrDevice.add(listDeviceSearched.get(i));
//                    }
//                }
                wiFiListAdapter = new WiFiListAdapter(MainActivity.this,R.layout.row_devices,arrDevice);
                listViewDevice.setAdapter(wiFiListAdapter);
            }

            @Override
            public void onConnecting(WifiP2PDevicePIN anotherDevice) {
                Toast.makeText(MainActivity.this, "Connecting !", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnected(WifiP2PDevicePIN anotherDevice) {
                Toast.makeText(MainActivity.this, "Connected! "+anotherDevice.deviceName, Toast.LENGTH_SHORT).show();
                updateView();
            }


            @Override
            public void onConnectFailed(int codeReason) {
                Log.e(TAG, "onConnectFailed: "+codeReason );
            }

            @Override
            public void onDisconnected(boolean isSuccess) {
                if(isSuccess){
                    Log.e(TAG, "onDisconnected: true");
                }else{
                    Log.e(TAG, "onDisconnected: false");
                }

            }
        });


        btnInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiControl.resetData();
                wifiControl.setSender(cbIsSender.isChecked());
                wifiControl.setReceiverFolder(Environment.getExternalStorageDirectory() + "/BackupAndRestore/received");
                wifiControl.setPin(edPin.getText().toString());
                wifiControl.init();
                wifiControl.setOnFileTransfer(new OnFileTransfer() {
                    @Override
                    public void onCopying(Integer percent) {
                        Log.e(TAG, "onCopying: "+percent+"%" );
                    }

                    @Override
                    public void onCopied() {

                    }

                    @Override
                    public void onCopyFailed(String mess) {

                    }
                });
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiControl.searchDevice();
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiControl.disconnect();
            }
        });
        listViewDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //wifiControl.cancelDisconnect();
                wifiControl.connect(arrDevice.get(position));
            }
        });


    }
    private void updateView() {
        if(wifiControl.isSender()){
            ln_send.setVisibility(View.VISIBLE);
        }else {
            ln_send.setVisibility(View.GONE);
        }
        String path = Environment.getExternalStorageDirectory() + "/BackupAndRestore/abc.txt";
        ed_path.setText(path);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiControl.sendFile(ed_path.getText().toString());
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(wifiControl!=null){
            registerReceiver(wifiControl.getReceiver(), wifiControl.getIntentFilter());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(wifiControl!=null){
            unregisterReceiver(wifiControl.getReceiver());
        }
    }

    public class WiFiListAdapter extends ArrayAdapter<WifiP2PDevicePIN> {

        private ArrayList<WifiP2PDevicePIN> items;
        Context context;
        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiListAdapter(Context context, int textViewResourceId,
                                   ArrayList<WifiP2PDevicePIN> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2PDevicePIN device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status)+ "  PIN: "+device.getPIN());
                }
            }

            return v;

        }
    }

    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }
}
