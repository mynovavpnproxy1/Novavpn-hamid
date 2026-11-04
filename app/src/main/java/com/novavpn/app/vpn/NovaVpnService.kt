package com.novavpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.novavpn.app.config.ConnectionStateManager
import com.novavpn.app.engine.XrayRuntime
import androidx.core.app.NotificationCompat

class NovaVpnService : android.net.VpnService() {
    companion object {
        private const val TAG = "NovaVpnService"
        const val ACTION_START = "com.novavpn.app.START_VPN"
        const val ACTION_STOP = "com.novavpn.app.STOP_VPN"
        const val CHANNEL_ID = "nova_vpn_foreground"
        const val ACTION_STATE = "com.novavpn.app.VPN_STATE"
        const val EXTRA_CONNECTED = "connected"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serverName: String = "NovaVPN"
    private var serverHost: String = ""
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
            ConnectionStateManager(this).setConnected(false)
            broadcastState(false)
            stopSelf()
            return START_NOT_STICKY
        }

        serverName = intent?.getStringExtra("selected_server_name") ?: serverName
        serverHost = intent?.getStringExtra("selected_server_host") ?: serverHost
        serverPort = intent?.getIntExtra("selected_server_port", serverPort) ?: serverPort

        startForeground(101, buildForegroundNotification())
        val rawConfig = intent?.getStringExtra("selected_server_config").orEmpty()
        val started = ensureVpnSession(rawConfig)
        if (!started) {
            ConnectionStateManager(this).setConnected(false)
            broadcastState(false)
            stopVpnSession()
            stopSelf()
            return START_NOT_STICKY
        }
        ConnectionStateManager(this).setConnected(true)
        broadcastState(true)
        Log.i(TAG, "Xray core started for $serverName ($serverHost:$serverPort)")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onRevoke() {
        stopVpnSession()
        ConnectionStateManager(this).setConnected(false)
        broadcastState(false)
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpnSession()
        ConnectionStateManager(this).setConnected(false)
        broadcastState(false)
        Log.i(TAG, "VPN service destroyed")
        super.onDestroy()
    }

    private fun ensureVpnSession(rawConfig: String): Boolean {
        if (rawConfig.isBlank()) {
            Log.e(TAG, "Cannot start VPN: selected profile has no original share link")
            return false
        }

        return try {
            val builder = Builder()
                .setSession("NovaVPN :: $serverName")
                .setMtu(1500)
                .addAddress("10.8.0.2", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")

            val established = builder.establish() ?: error("Android refused to establish the VPN interface")
            vpnInterface = established
            XrayRuntime.start(this, rawConfig, established.fd)
            Log.i(TAG, "Android TUN and native Xray core started")
            true
        } catch (throwable: Throwable) {
            Log.e(TAG, "Xray VPN startup failed", throwable)
            try { vpnInterface?.close() } catch (_: Throwable) {}
            vpnInterface = null
            false
        }
    }

    private fun broadcastState(connected: Boolean) {
        sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra(EXTRA_CONNECTED, connected))
    }

    private fun stopVpnSession() {
        try {
            XrayRuntime.stop()
        } catch (throwable: Throwable) {
            Log.w(TAG, "Xray core stop failed", throwable)
        }
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
            .setContentTitle("NovaVPN")
            .setContentText("Connected: $serverName • $serverHost:$serverPort")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NovaVPN connection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
