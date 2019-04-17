// Copyright 2011 Google Inc. All Rights Reserved.

package com.thanhlong.wifip2ptransfer;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    public static final String EXTRAS_LISTENER = "listener_transfer";
    public static final String EXTRAS_FILE_NAME = "file_name";
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.thanhlong.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    private static final String TAG = "FileTransfer";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String filename = intent.getExtras().getString(EXTRAS_FILE_NAME);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            ResultReceiver receiver = intent.getParcelableExtra(EXTRAS_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putInt("percent", 0);
            receiver.send(Activity.RESULT_OK, bundle);

            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(TAG, "Client socket - " + socket.isConnected());
                bundle = new Bundle();
                bundle.putInt("percent", 0);
                receiver.send(Activity.RESULT_OK, bundle);
                //OutputStream stream = socket.getOutputStream();
                BufferedOutputStream stream = new BufferedOutputStream(socket.getOutputStream());
                DataOutputStream out = new DataOutputStream(stream);


                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                    out.writeUTF(filename);
                    out.writeLong(is.available());
                } catch (FileNotFoundException e) {
                    Log.d(TAG, e.toString());
                }
                if(is!=null){
                    byte buf[] = new byte[1024];
                    int len;
                    int length = is.available();
                    float per = 0f;
                    int percent = 0;
                    try {
                        while ((len = is.read(buf)) != -1) {
                            out.write(buf, 0, len);
                            per+=len;
                            int percentNew =(Math.round((((float)per/(float)length))*100f));
                            if(percentNew>percent){
                                percent = percentNew;
                                bundle.putInt("percent",percentNew);
                                receiver.send(Activity.RESULT_OK, bundle);
                            }

                        }
                        out.close();
                        is.close();
                    } catch (IOException e) {
                        Log.d(TAG, e.toString());
                    }

                }else {
                    receiver.send(Activity.RESULT_CANCELED, bundle);
                }
                Log.d(TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                bundle = new Bundle();
                bundle.putInt("percent", 0);
                receiver.send(Activity.RESULT_CANCELED, bundle);
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}
