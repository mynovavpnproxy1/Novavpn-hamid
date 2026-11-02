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
        const val REQ_CODE_VPN = 1001
    }

    fun prepareVpn(): Boolean {
        return try {
            val intent = VpnService.prepare(activity)
            if (intent != null) {
                Log.i(TAG, "VPN permission request opened")
                activity.startActivityForResult(intent, REQ_CODE_VPN)
                false
            } else {
                Log.i(TAG, "VPN permission already granted")
                true
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "VPN permission request failed", throwable)
            false
        }
    }

    fun openVpnSettings() {
        try {
            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
            activity.startActivity(intent)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unable to open VPN settings", throwable)
        }
    }
}
