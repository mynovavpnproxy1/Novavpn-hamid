package com.novavpn.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Register native plugins before Capacitor creates the Bridge.
        registerPlugin(VpnPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
