package com.novavpn.app.config

import android.util.Base64
import org.json.JSONObject

class SubscriptionConfigParser {
    fun parseSubscription(text: String): List<ParsedServerConfig> {
        val entries = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { entry ->
                val result = ServerConfigParser().parse(entry)
                when (result) {
                    is ConfigParseResult.Success -> result.server
                    is ConfigParseResult.Failure -> null
                }
            }
            .toList()

        return entries
    }

    fun parseBase64Config(raw: String): ConfigParseResult {
        return try {
            val decoded = String(Base64.decode(raw, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            ServerConfigParser().parse(decoded)
        } catch (t: Throwable) {
            ConfigParseResult.Failure("Invalid base64 configuration payload: ${t.message}")
        }
    }

    fun parseJsonConfig(raw: String): ConfigParseResult {
        return try {
            val json = JSONObject(raw)
            val type = json.optString("protocol", "")
            val host = json.optString("host")
            val port = json.optInt("port")
            val name = json.optString("name", type)

            if (host.isBlank() || port <= 0) {
                return ConfigParseResult.Failure("JSON config is missing host or port")
            }

            val parsed = ParsedServerConfig(
                id = "json_${host}_$port",
                type = when (type.lowercase()) {
                    "vless" -> ProtocolType.VLESS
                    "vmess" -> ProtocolType.VMESS
                    "trojan" -> ProtocolType.TROJAN
                    "shadowrocket", "ss", "shadowsocks" -> ProtocolType.SHADOWSOCKS
                    "wireguard" -> ProtocolType.WIRE_GUARD
                    else -> ProtocolType.UNKNOWN
                },
                name = name,
                host = host,
                port = port,
                userId = json.optString("uuid"),
                password = json.optString("password"),
                raw = raw
            )

            ConfigValidator().validate(parsed)
        } catch (t: Throwable) {
            ConfigParseResult.Failure("Invalid JSON config payload: ${t.message}")
        }
    }
}
