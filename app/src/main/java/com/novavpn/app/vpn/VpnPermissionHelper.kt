package com.novavpn.app.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import android.util.Log

class VpnPermissionHelper(
    private val activity: Activity
) {
    companion object {
        private const val TAG = "VpnPermissionHelper"
    }

    fun prepareVpn(): Boolean {
        return try {
            val intent = VpnService.prepare(activity)
            if (intent != null) {
                activity.startActivityForResult(intent, 1001)
                Log.i(TAG, "VPN permission requested")
                false
            } else {
                Log.i(TAG, "VPN permission already granted")
                true
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "VPN permission preparation failed", throwable)
            false
        }
    }

    fun openVpnSettings() {
        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
        activity.startActivity(intent)
    }
}
