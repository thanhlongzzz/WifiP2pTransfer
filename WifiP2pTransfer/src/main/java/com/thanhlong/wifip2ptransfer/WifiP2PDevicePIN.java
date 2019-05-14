package com.thanhlong.wifip2ptransfer;

import android.net.wifi.p2p.WifiP2pDevice;

public class WifiP2PDevicePIN extends WifiP2pDevice {
    String PIN = "";
    WifiP2pDevice source;

    public WifiP2pDevice getSource() {
        return source;
    }

    public void setSource(WifiP2pDevice source) {
        this.source = source;
    }

    public String getPIN() {
        return PIN;
    }

    public void setPIN(String PIN) {
        this.PIN = PIN;
    }

    public WifiP2PDevicePIN() {
        super();
    }
    public WifiP2PDevicePIN(String PIN) {
        super();
        this.PIN = PIN;
    }

    public WifiP2PDevicePIN(WifiP2pDevice source) {
        super(source);
        this.source = source;
    }
    public WifiP2PDevicePIN(String PIN,WifiP2pDevice source) {
        super(source);
        this.source = source;
        this.PIN = PIN;
    }
}
