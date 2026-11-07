package com.novavpn.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class NovaVpnService extends VpnService {

    private static final String CHANNEL_ID = "nova_vpn_channel";
    private static final int NOTIFICATION_ID = 1001;

    private ParcelFileDescriptor vpnInterface;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();

        if ("CONNECT".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification());
            establishVpnInterface();
        } else if ("DISCONNECT".equals(action)) {
            disconnectVpn();
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void establishVpnInterface() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }

            Builder builder = new Builder();
            builder.setSession("NOVA VPN");
            builder.setMtu(1500);
            builder.addAddress("10.10.10.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");

            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                Log.e("NOVA VPN", "VPN interface creation failed");
                stopSelf();
                return;
            }

            // هسته واقعی Xray هنوز به این رابط متصل نشده است.
            Log.i("NOVA VPN", "TUN interface created; Xray core pending");

        } catch (Exception e) {
            Log.e("NOVA VPN", "VPN startup failed", e);
            disconnectVpn();
            stopSelf();
        }
    }

    private void disconnectVpn() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.e("NOVA VPN", "VPN close failed", e);
            }
            vpnInterface = null;
        }
    }

    private Notification createNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NOVA VPN")
            .setContentText("VPN service is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NOVA VPN",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        disconnectVpn();
        super.onDestroy();
    }
}
