package com.thanhlong.wifip2ptransfer.Listener;

import com.thanhlong.wifip2ptransfer.WifiP2PDevicePIN;

import java.util.ArrayList;

public interface OnP2PListener {
    void onSearching();
    void onFoundADevice(WifiP2PDevicePIN device);
    void onSearchCompleted(ArrayList<WifiP2PDevicePIN> listDeviceSearched);
    void onConnecting(WifiP2PDevicePIN anotherDevice);
    void onConnected(WifiP2PDevicePIN anotherDevice);
    void onConnectFailed(int codeReason);
    void onDisconnected(boolean isSuccess);
}
