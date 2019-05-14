# WifiP2pTransfer 
[![](https://jitpack.io/v/thanhlongzzz/WifiP2pTransfer.svg)](https://jitpack.io/#thanhlongzzz/WifiP2pTransfer)

Transfer a file from send device to receive device using Wifi direct, custom a PIN to connect
```java
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

...

dependencies {
	        implementation 'com.github.thanhlongzzz:WifiP2pTransfer:1.0.2'
	}
```

1. In your activity
```java
WifiP2PControl wifiControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                arrDevice = listDeviceSearched;
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
                wifiControl.setSender(cbIsSender.isChecked()); //if this device is a sender set true else set false
                wifiControl.setReceiverFolder(Environment.getExternalStorageDirectory() + "/received"); //path to receive file
                wifiControl.setPin(edPin.getText().toString()); //a PIN adv to another device
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
```

2. You must register receiver to use Wifi direct

```java
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

```


3. Don't forget add permission

```java
 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> //need this permission to search another device quickly
```
