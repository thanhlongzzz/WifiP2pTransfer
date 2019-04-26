package com.thanhlong.wifip2ptransfer;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.thanhlong.wifip2ptransfer.Listener.OnFileTransfer;
import com.thanhlong.wifip2ptransfer.Listener.OnP2PListener;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WifiP2PControl implements WifiP2pManager.PeerListListener, WifiP2pManager.ChannelListener, WifiP2pManager.ConnectionInfoListener {
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1100;
    private static final String TAG = "Wifip2p";


    private OnP2PListener onP2PListener;
    public static OnFileTransfer onFileTransfer;
    private boolean isSender = true;
    private ProgressDialog progressDialog = null;
    private String receiverFolder;

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean isWifiP2pReady = false;
    private boolean retryChannel = false;

    private WifiP2PDevicePIN deviceSelected;
    private WifiP2PDevicePIN connectedDevice;
    private WifiP2pInfo info;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private Activity context;
    private Bundle bundle;
    private ArrayList<WifiP2PDevicePIN> listDeviceSearched;
    private HashMap<String, String> listDeviceCheck;
    private String pin = "";
    private String connectedIP;
    private boolean connectOnce = false;
    private SharedPreferences sharedPreferences;

    public String getConnectedIP() {
        return connectedIP;
    }

    public String getReceiverFolder() {
        return receiverFolder;
    }

    public void setReceiverFolder(String receiverFolder) {
        this.receiverFolder = receiverFolder;
    }

    public boolean isSender() {
        return isSender;
    }

    public WifiP2PDevicePIN getConnectedDevice() {
        return connectedDevice;
    }

    private void setConnectedDevice(WifiP2PDevicePIN connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public void setOnFileTransfer(OnFileTransfer onFileTransfer) {
        this.onFileTransfer = onFileTransfer;
    }
    public OnFileTransfer getOnFileTransfer() {
        return this.onFileTransfer;
    }
    public void setSender(boolean sender) {
        isSender = sender;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public IntentFilter getIntentFilter() {
        return intentFilter;
    }

    public BroadcastReceiver getReceiver() {
        return receiver;
    }

    public void setReceiver(BroadcastReceiver receiver) {
        this.receiver = receiver;
    }

    public void setOnP2PListener(OnP2PListener onP2PListener) {
        this.onP2PListener = onP2PListener;
    }

    public WifiP2PControl(Activity context) {
        init(context);
    }

    public WifiP2PControl(Activity context, boolean isSender, OnP2PListener onP2PListener) {
        this.onP2PListener = onP2PListener;
        this.isSender = isSender;
        init(context);
    }

    public WifiP2PControl(Activity context, OnP2PListener onP2PListener) {
        this.onP2PListener = onP2PListener;
        init(context);
    }

    private void init(Activity context) {
        sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
        connectedIP = sharedPreferences.getString("connectedIP",null);
        isWifiP2pReady = false;
        retryChannel = false;
        this.context = context;
        bundle = new Bundle();
        listDeviceCheck = new HashMap<>();
        listDeviceSearched = new ArrayList<>();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            context.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
    }


    public void init() {
        if (!isWifiP2pEnabled) {
            if (onP2PListener != null) {
                onP2PListener.onConnectFailed(12);
            }
//            Toast.makeText(context, R.string.p2p_off_warning,
//                    Toast.LENGTH_SHORT).show();
            return;
        }
        onInitiateDiscovery();
        manager = null;
        channel = null;
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);
        Map<String, String> record = new HashMap<String, String>();
        record.put("pin", pin);
        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("wifi", "p2p._tcp", record);
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {
                isWifiP2pReady = false;
                if (onP2PListener != null) {
                    onP2PListener.onConnectFailed(reason);
                }
            }
        });

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            /* Callback includes:
             * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
             * record: TXT record dta as a map of key/value pairs.
             * deviceSelected: The deviceSelected running the advertised service.
             */

            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                if (record != null)
                    Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
//                        bundle.putString(deviceSelected.deviceAddress, record.get("test").toString());
                try {
                    if (record.containsKey("pin")) {
                        pin = record.get("pin");
                        bundle.putString(device.deviceAddress, pin);
                    }
                } catch (Exception e) {

                }
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the deviceSelected name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
//                        resourceType.deviceName = bundle
//                                .containsKey(resourceType.deviceAddress) ? bundle
//                                .get(resourceType.deviceAddress) : resourceType.deviceName;

                if (!listDeviceCheck.containsKey(resourceType.deviceAddress)) {
                    WifiP2PDevicePIN device = new WifiP2PDevicePIN(resourceType);
                    if (bundle.containsKey(resourceType.deviceAddress)) {
                        device.setPIN(pin);
                    }
                    listDeviceCheck.put(resourceType.deviceAddress, resourceType.deviceName);
                    listDeviceSearched.add(device);
                    if (onP2PListener != null) {
                        onP2PListener.onFoundADevice(device);
                    }
                    Log.d(TAG, "onBonjourServiceAvailable " + resourceType.deviceName);
                }
                // resourceType.deviceName = instanceName;
                // Add to the custom adapter defined specifically for showing
                // wifi devices.

            }
        };

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel,
                serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                        //Toast.makeText(context, "Discovery Service Initiated",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        isWifiP2pReady = false;
                        if (onP2PListener != null) {
                            onP2PListener.onConnectFailed(code);
                        }
                    }
                });

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
        //searchDevice();
    }

    public void searchDevice() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                init();
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // Success!
                        isWifiP2pReady = true;


                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                            Log.d(TAG, "P2P isn't supported on this deviceSelected.");
                        }
                        if (onP2PListener != null) {
                            onP2PListener.onConnectFailed(code);
                        }
                        isWifiP2pReady = false;
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "FAILED to clear service requests ");
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // Success!
                        isWifiP2pReady = true;


                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                            Log.d(TAG, "P2P isn't supported on this deviceSelected.");
                        }
                        if (onP2PListener != null) {
                            onP2PListener.onConnectFailed(code);
                        }
                        isWifiP2pReady = false;
                    }
                });
            }
        });


    }

    public void connect(final WifiP2PDevicePIN device) {
        this.selectThisDevice(device);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        //config.groupOwnerIntent = 15;
        if (!isSender) {
            config.groupOwnerIntent = 15;
        }
        if (onP2PListener != null) {
            onP2PListener.onConnecting(device);
        }
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                setConnectedDevice(device);
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                if (onP2PListener != null) {
                    onP2PListener.onConnectFailed(reason);
                }
            }
        });

    }

    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            if (deviceSelected == null
                    || deviceSelected.status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (deviceSelected.status == WifiP2pDevice.AVAILABLE
                    || deviceSelected.status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
//                        Toast.makeText(context, "Aborting connection",
//                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
//                        Toast.makeText(context,
//                                "Connect abort request failed. Reason Code: " + reasonCode,
//                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

    public void disconnect() {
        resetData();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                if (onP2PListener != null) {
                    onP2PListener.onDisconnected(false);
                }
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                if (onP2PListener != null) {
                    onP2PListener.onDisconnected(true);
                }
            }

        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        //searched
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
        if (onP2PListener != null) {
            onP2PListener.onSearchCompleted(listDeviceSearched);
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //connected
        this.info = info;
        new FileServerAsyncTask(context,receiverFolder).execute();
        if (info.groupFormed && info.isGroupOwner) {
            setSender(false);

            //new FileServerAsyncTask(context,receiverFolder).execute();
        } else if (info.groupFormed) {
            setSender(true);

            if(!connectOnce){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectOnce = true;
                        connectToClient();
                    }
                },100);
            }
            // The other device acts as the client. In this case, we enable the
            // get file button.

        }
        if (onP2PListener != null) {
            if (connectedDevice != null) {
                onP2PListener.onConnected(connectedDevice);
            } else {
                WifiP2PDevicePIN devicePIN = new WifiP2PDevicePIN();
                devicePIN.setPIN(pin);
                devicePIN.deviceName = info.groupOwnerAddress.getHostAddress();

                onP2PListener.onConnected(devicePIN);
            }
        }


//        if (!isSender) {
//            //server receive file
//
//        } else {
//            //client send file
//
//        }


    }
    public void sendFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (onFileTransfer != null) {
                onFileTransfer.onCopying(0);
            }
            Log.d(TAG, "Intent----------- " + path);
            Intent serviceIntent = new Intent(context, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, Uri.fromFile(file).toString());
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_NAME, file.getName());
            serviceIntent.putExtra(FileTransferService.EXTRAS_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {
                        int percent = resultData.getInt("percent");
                        if (onFileTransfer != null) {
                            onFileTransfer.onCopying(percent);
                        }

                    } else {
                        if (onFileTransfer != null) {
                            onFileTransfer.onCopyFailed("IO exception or Socket can not create.");
                        }
                        Log.i(TAG, "+++++++++++++RESULT_NOT_OK++++++++++++");
                    }
                }
            });
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            if(isSender) {
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        info.groupOwnerAddress.getHostAddress());
                context.startService(serviceIntent);
            }else {
                if(connectedIP==null){

                    connectedIP = sharedPreferences.getString("connectedIP",null);
                }
                if(connectedIP!=null){
                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                            connectedIP);
                    context.startService(serviceIntent);
                }else {
                    Toast.makeText(context, "App had been forced close! Please reconnect again!", Toast.LENGTH_SHORT).show();
                }
            }


        } else {
            if (onFileTransfer != null) {
                onFileTransfer.onCopyFailed("File not found.");
            }
        }

    }
    private void connectToClient() {
        try {
            Intent serviceIntent = new Intent(context, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "request");
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_NAME, "connecting");
            serviceIntent.putExtra(FileTransferService.EXTRAS_FIRST_CONNECT, true);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            serviceIntent.putExtra(FileTransferService.EXTRAS_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {
                        Log.i(TAG, "+++++++++++++Connect to client success++++++++++++");
                    } else {
                        Log.i(TAG, "+++++++++++++Connect fail to client++++++++++++");
                    }
                }
            });
            context.startService(serviceIntent);
//            Socket socket = new Socket();
//            socket.bind(null);
//            socket.connect((new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), 8988)), 5000);
        }catch (Exception e){

        }

    }

    private void notifySendFileSuccess() {
        try {
            Intent serviceIntent = new Intent(context, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "sendFile");
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_NAME, "success");
            serviceIntent.putExtra(FileTransferService.EXTRAS_MODE_SEND_SUCCESS, true);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    connectedIP);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            serviceIntent.putExtra(FileTransferService.EXTRAS_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {
                        Log.i(TAG, "+++++++++++++Notify to client success++++++++++++");
                    } else {
                        Log.i(TAG, "+++++++++++++Notify fail to client++++++++++++");
                    }
                }
            });
            context.startService(serviceIntent);
//            Socket socket = new Socket();
//            socket.bind(null);
//            socket.connect((new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), 8988)), 5000);
        }catch (Exception e){

        }

    }

    @Override
    public void onChannelDisconnected() {

        if (manager != null && !retryChannel) {
            Toast.makeText(context, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(context, context.getMainLooper(), this);
        } else {
            Toast.makeText(context,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void onInitiateDiscovery() {
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
//        progressDialog = ProgressDialog.show(context, context.getString(R.string.searching), context.getString(R.string.press_back_to_cancel), true,
//                true, new DialogInterface.OnCancelListener() {
//
//                    @Override
//                    public void onCancel(DialogInterface dialog) {
//
//                    }
//                });

        if (onP2PListener != null) {
            onP2PListener.onSearching();
        }
    }

    private void selectThisDevice(WifiP2PDevicePIN device) {
        this.deviceSelected = device;
    }

    public void resetData() {
        sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        connectOnce = false;
        connectedIP = null;
        deviceSelected = null;
        isWifiP2pReady = false;
        listDeviceSearched = new ArrayList<>();
        bundle = new Bundle();
        listDeviceCheck = new HashMap<>();
        if(onP2PListener!=null){
            onP2PListener.onDisconnected(true);
        }
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public void setManager(WifiP2pManager manager) {
        this.manager = manager;
    }

    public boolean isWifiP2pEnabled() {
        return isWifiP2pEnabled;
    }

    public void setWifiP2pEnabled(boolean wifiP2pEnabled) {
        isWifiP2pEnabled = wifiP2pEnabled;
    }


    public class FileServerAsyncTask extends AsyncTask<Void, Integer, String> {

        Context context;
        private String receiveFolder = "";
        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
        }

        public FileServerAsyncTask(Context context, OnFileTransfer onFileTransfer) {
            this.context = context;
            try {
                receiveFolder = context.getExternalFilesDir("received").getAbsolutePath();
            } catch (Exception e) {

            }

        }

        public FileServerAsyncTask(Context context, String receiveFolder) {
            this.context = context;
            this.receiveFolder = receiveFolder;

        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                File dirs = new File(receiveFolder);
                if (!dirs.exists())
                    dirs.mkdirs();
            } catch (Exception e) {
                receiveFolder = context.getExternalFilesDir("received").getAbsolutePath();
                File dirs = new File(receiveFolder);
                if (!dirs.exists())
                    dirs.mkdirs();
            }

            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(TAG, "Server: connection done");
                Log.d(TAG, "Client: IP "+client.getRemoteSocketAddress());
                connectedIP = (((InetSocketAddress) client.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
                sharedPreferences.edit().putString("connectedIP",connectedIP).apply();
                //InputStream inputStream = client.getInputStream();

                BufferedInputStream in = new BufferedInputStream(client.getInputStream());
                DataInputStream inputStream = new DataInputStream(in);
                String fileName = inputStream.readUTF();
                long length = inputStream.readLong();
                String fromDevice = inputStream.readUTF();
                boolean sendSuccess = inputStream.readBoolean();
                WifiP2PDevicePIN from = new WifiP2PDevicePIN();
                from.deviceName = fromDevice;
                selectThisDevice(from);
                final File f = new File(this.receiveFolder,
                        fileName);
                if(sendSuccess){
                    Log.e(TAG, "File received success ");
                    return "sendSuccess";
                }


                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Log.d(TAG, "server: copying files " + f.toString());
                if (onFileTransfer != null) {
                    onFileTransfer.onCopying(0);
                }
                OutputStream out = new FileOutputStream(f);
                //  int length = inputStream.available();
                byte buf[] = new byte[1024];
                int len;
                float per = 0f;
                int percent = 0;
                try {
                    while ((len = inputStream.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        per+=len;
                        int percentNew =(Math.round((((float)per/(float)length))*100f));
                        if(percentNew>percent){
                            percent = percentNew;
                            if (WifiP2PControl.onFileTransfer != null) {
                                WifiP2PControl.onFileTransfer.onCopying(percentNew);
                            }
                           // publishProgress(percentNew);
                        }
                    }
                    out.close();
                    inputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                    if (WifiP2PControl.onFileTransfer != null) {
                        WifiP2PControl.onFileTransfer.onCopyFailed(e.getMessage());
                    }
                }

                serverSocket.close();
                return f.getAbsolutePath();
            } catch (Exception e) {
                //Log.e(TAG, e.getMessage());
                if (WifiP2PControl.onFileTransfer != null) {
                    WifiP2PControl.onFileTransfer.onCopyFailed(e.getMessage());
                }
                return null;
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.e(TAG, "File copied - " + result);
                if (WifiP2PControl.onFileTransfer != null) {
                    WifiP2PControl.onFileTransfer.onCopied();
                }
                if(result.equals("sendSuccess")){
                    notifySendFileSuccess();
                }

                new FileServerAsyncTask(context,receiverFolder)
                        .execute();
//                File recvFile = new File(result);
//                Uri fileUri = FileProvider.getUriForFile(
//                        context,
//                        "com.thanhlong.wifip2p.fileprovider",
//                        recvFile);
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setDataAndType(fileUri, "image/*");
//                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(intent);
            }


        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }
}
