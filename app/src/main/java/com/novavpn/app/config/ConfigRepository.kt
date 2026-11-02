package com.novavpn.app.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "nova_vpn_servers"

data class SavedServerProfile(
    val id: String,
    val type: String,
    val host: String,
    val port: Int,
    val username: String? = null,
    val protocol: String? = null,
    val name: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("host", host)
        put("port", port)
        put("username", username)
        put("protocol", protocol)
        put("name", name)
    }
}

class ConfigRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(profile: SavedServerProfile) {
        val current = loadAll().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            current[existingIndex] = profile
        } else {
            current.add(profile)
        }
        prefs.edit().putString("servers", JSONArray(current.map { it.toJson() }).toString()).apply()
    }

    fun loadAll(): List<SavedServerProfile> {
        val raw = prefs.getString("servers", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SavedServerProfile(
                            id = obj.optString("id", "server-$i"),
                            type = obj.optString("type", "Host"),
                            host = obj.optString("host", ""),
                            port = obj.optInt("port", 443),
                            username = obj.optString("username", "").ifBlank { null },
                            protocol = obj.optString("protocol", "").ifBlank { null },
                            name = obj.optString("name", "").ifBlank { null }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun remove(id: String) {
        val updated = loadAll().filterNot { it.id == id }
        prefs.edit().putString("servers", JSONArray(updated.map { it.toJson() }).toString()).apply()
    }

    fun addFromConfig(config: ServerConfig): SavedServerProfile {
        val profile = SavedServerProfile(
            id = "${config.type}-${config.host}-${config.port}-${System.currentTimeMillis()}",
            type = config.type,
            host = config.host,
            port = config.port,
            username = config.username,
            protocol = config.protocol,
            name = config.name ?: config.host
        )
        save(profile)
        return profile
    }
}
