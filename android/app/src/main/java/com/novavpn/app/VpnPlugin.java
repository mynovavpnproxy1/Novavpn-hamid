package com.novavpn.app;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NovaVpn")
public class VpnPlugin extends Plugin {

    @PluginMethod
    public void connect(PluginCall call) {
        String vlessUri = call.getString("config");
        if (vlessUri == null || vlessUri.trim().isEmpty()) {
            call.reject("لینک VLESS را وارد کنید");
            return;
        }

        Intent permissionIntent = VpnService.prepare(getContext());
        if (permissionIntent != null) {
            startActivityForResult(call, permissionIntent, "vpnPermissionResult");
        } else {
            startVpn(call, vlessUri);
        }
    }

    @com.getcapacitor.annotation.ActivityCallback
    private void vpnPermissionResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == android.app.Activity.RESULT_OK) {
            String vlessUri = call.getString("config");
            startVpn(call, vlessUri);
        } else {
            call.reject("اجازه اتصال VPN داده نشد");
        }
    }

    private void startVpn(PluginCall call, String vlessUri) {
        Intent intent = new Intent(getContext(), NovaVpnService.class);
        intent.setAction("CONNECT");
        intent.putExtra("vlessUri", vlessUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(intent);
        } else {
            getContext().startService(intent);
        }

        JSObject result = new JSObject();
        result.put("started", true);
        result.put("message", "درخواست راه‌اندازی موتور Xray ارسال شد");
        call.resolve(result);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        Intent intent = new Intent(getContext(), NovaVpnService.class);
        getContext().stopService(intent);
        call.resolve();
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject result = new JSObject();
        result.put("connected", NovaVpnService.isEngineRunning());
        result.put("engineReady", NovaVpnService.isEngineRunning());
        result.put("error", NovaVpnService.getLastError());
        call.resolve(result);
    }
}
