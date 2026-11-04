package com.novavpn.app.config

import android.content.Context
import androidx.core.content.edit

class ConnectionStateManager(context: Context) {
    private val prefs = context.getSharedPreferences("nova_vpn_connection_state", Context.MODE_PRIVATE)

    fun saveSelectedServer(server: SavedServerProfile) {
        prefs.edit {
            putString(KEY_SELECTED_SERVER_ID, server.id)
            putString(KEY_SELECTED_SERVER_NAME, server.name ?: server.host)
            putString(KEY_SELECTED_SERVER_TYPE, server.type)
            putString(KEY_SELECTED_SERVER_HOST, server.host)
            putInt(KEY_SELECTED_SERVER_PORT, server.port)
            putString(KEY_SELECTED_SERVER_USERNAME, server.username)
            putString(KEY_SELECTED_SERVER_PROTOCOL, server.protocol)
            putString(KEY_SELECTED_SERVER_RAW_CONFIG, server.rawConfig)
        }
    }

    fun clearSelectedServer() {
        prefs.edit { clear() }
    }

    fun getSelectedServer(): SavedServerProfile? {
        val id = prefs.getString(KEY_SELECTED_SERVER_ID, null) ?: return null
        val name = prefs.getString(KEY_SELECTED_SERVER_NAME, null)
        val type = prefs.getString(KEY_SELECTED_SERVER_TYPE, "Host") ?: "Host"
        val host = prefs.getString(KEY_SELECTED_SERVER_HOST, null) ?: return null
        val port = prefs.getInt(KEY_SELECTED_SERVER_PORT, 443)

        return SavedServerProfile(
            id = id,
            type = type,
            host = host,
            port = port,
            username = prefs.getString(KEY_SELECTED_SERVER_USERNAME, null),
            protocol = prefs.getString(KEY_SELECTED_SERVER_PROTOCOL, null),
            name = name ?: host,
            rawConfig = prefs.getString(KEY_SELECTED_SERVER_RAW_CONFIG, null)
        )
    }

    fun getSelectedServerLabel(): String = getSelectedServer()?.name ?: getSelectedServer()?.host ?: "No server selected"

    fun setConnected(connected: Boolean) {
        prefs.edit { putBoolean(KEY_CONNECTED, connected) }
    }

    fun isConnected(): Boolean = prefs.getBoolean(KEY_CONNECTED, false)

    companion object {
        private const val KEY_SELECTED_SERVER_ID = "selected_server_id"
        private const val KEY_SELECTED_SERVER_NAME = "selected_server_name"
        private const val KEY_SELECTED_SERVER_TYPE = "selected_server_type"
        private const val KEY_SELECTED_SERVER_HOST = "selected_server_host"
        private const val KEY_SELECTED_SERVER_PORT = "selected_server_port"
        private const val KEY_SELECTED_SERVER_USERNAME = "selected_server_username"
        private const val KEY_SELECTED_SERVER_PROTOCOL = "selected_server_protocol"
        private const val KEY_SELECTED_SERVER_RAW_CONFIG = "selected_server_raw_config"
        private const val KEY_CONNECTED = "vpn_connected"
    }
}
