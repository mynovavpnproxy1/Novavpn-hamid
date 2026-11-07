package com.novavpn.app;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NovaVpn")
public class VpnPlugin extends Plugin {

    private static final int VPN_REQUEST_CODE = 1001;
    private PluginCall pendingCall;

    @PluginMethod
    public void connect(PluginCall call) {
        Intent permissionIntent = VpnService.prepare(getContext());

        if (permissionIntent != null) {
            pendingCall = call;
            startActivityForResult(call, permissionIntent, "vpnPermissionResult");
        } else {
            startVpn(call);
        }
    }

    @com.getcapacitor.annotation.ActivityCallback
    private void vpnPermissionResult(
        PluginCall call,
        com.getcapacitor.ActivityResult result
    ) {
        if (result.getResultCode() == android.app.Activity.RESULT_OK) {
            startVpn(call);
        } else {
            call.reject("اجازه اتصال VPN داده نشد");
        }
    }

    private void startVpn(PluginCall call) {
        Intent intent = new Intent(getContext(), NovaVpnService.class);
        intent.setAction("CONNECT");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(intent);
        } else {
            getContext().startService(intent);
        }

        JSObject result = new JSObject();
        result.put("started", true);
        result.put("message", "VPN service started; tunnel engine pending");
        call.resolve(result);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        Intent intent = new Intent(getContext(), NovaVpnService.class);
        intent.setAction("DISCONNECT");
        getContext().startService(intent);

        call.resolve();
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject result = new JSObject();
        result.put("connected", false);
        result.put("engineReady", false);
        call.resolve(result);
    }
}
