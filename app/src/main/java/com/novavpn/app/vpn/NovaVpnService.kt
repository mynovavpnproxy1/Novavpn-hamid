package com.novavpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.novavpn.app.R

class NovaVpnService : android.net.VpnService() {
    companion object {
        private const val TAG = "NovaVpnService"
        const val ACTION_START = "com.novavpn.app.START_VPN"
        const val ACTION_STOP = "com.novavpn.app.STOP_VPN"
        const val CHANNEL_ID = "nova_vpn_foreground"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serverName: String = "NovaVPN"
    private var serverHost: String = "10.8.0.2"
    private var serverPort: Int = 443

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "VPN service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpnSession()
            stopSelf()
            return START_NOT_STICKY
        }

        serverName = intent?.getStringExtra("selected_server_name") ?: serverName
        serverHost = intent?.getStringExtra("selected_server_host") ?: serverHost
        serverPort = intent?.getIntExtra("selected_server_port", serverPort) ?: serverPort

        startForeground(101, buildForegroundNotification())
        ensureVpnSession()
        Log.i(TAG, "VPN service started in foreground mode for $serverName ($serverHost:$serverPort)")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopVpnSession()
        Log.i(TAG, "VPN service destroyed")
        super.onDestroy()
    }

    private fun ensureVpnSession() {
        if (vpnInterface != null) {
            return
        }

        try {
            val builder = Builder()
                .setSession("NovaVPN :: $serverName")
                .setMtu(1500)
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")

            vpnInterface = builder.establish()
            Log.i(TAG, "VPN interface established successfully")
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to establish VPN interface", throwable)
        }
    }

    private fun stopVpnSession() {
        try {
            vpnInterface?.close()
        } catch (throwable: Throwable) {
            Log.w(TAG, "VPN interface close failed", throwable)
        } finally {
            vpnInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Connected: $serverName • $serverHost:$serverPort")
            .setSmallIcon(android.R.drawable.stat_sys_data_saver)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_service_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
