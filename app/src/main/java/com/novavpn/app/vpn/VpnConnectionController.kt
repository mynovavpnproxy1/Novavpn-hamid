package com.novavpn.app.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.novavpn.app.config.ConnectionStateManager
import com.novavpn.app.config.SavedServerProfile

class VpnConnectionController(
    private val activity: Activity,
    private val connectionStateManager: ConnectionStateManager
) {
    fun startSelectedConnection(): Boolean {
        val selected = connectionStateManager.getSelectedServer() ?: return false
        if (selected.rawConfig.isNullOrBlank()) {
            android.widget.Toast.makeText(
                activity,
                "این سرور قدیمی اطلاعات کامل اتصال را ندارد؛ لطفاً کانفیگ را دوباره وارد کنید.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return false
        }
        val prepared = VpnPermissionHelper(activity).prepareVpn()
        if (!prepared) {
            return false
        }

        val intent = Intent(activity, NovaVpnService::class.java).apply {
            action = NovaVpnService.ACTION_START
            putExtra("selected_server_id", selected.id)
            putExtra("selected_server_name", selected.name ?: selected.host)
            putExtra("selected_server_type", selected.type)
            putExtra("selected_server_host", selected.host)
            putExtra("selected_server_port", selected.port)
            putExtra("selected_server_config", selected.rawConfig)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(activity, intent)
        } else {
            activity.startService(intent)
        }

        // Connection state is set by NovaVpnService only after native core startup succeeds.
        return true
    }

    fun stopConnection() {
        // stopService invokes NovaVpnService.onDestroy(), which stops Xray and closes the TUN.
        activity.stopService(Intent(activity, NovaVpnService::class.java))
        connectionStateManager.setConnected(false)
    }

    fun selectedServerLabel(): String = connectionStateManager.getSelectedServerLabel()
}
