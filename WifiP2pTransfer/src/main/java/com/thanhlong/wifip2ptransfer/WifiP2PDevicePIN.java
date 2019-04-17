package com.thanhlong.wifip2ptransfer;

import android.net.wifi.p2p.WifiP2pDevice;

public class WifiP2PDevicePIN extends WifiP2pDevice {
    String PIN = "";

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
    }
}
