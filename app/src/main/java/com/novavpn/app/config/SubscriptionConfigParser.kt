package com.novavpn.app.config

import android.util.Base64
import org.json.JSONObject

class SubscriptionConfigParser {
    fun parseSubscription(text: String): List<ServerConfig> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { entry ->
                when (val result = ServerConfigParser().parse(entry)) {
                    is ConfigParseResult.Success -> result.server
                    is ConfigParseResult.Failure -> null
                }
            }
            .toList()
    }

    fun parseBase64Config(raw: String): ConfigParseResult {
        return try {
            val decoded = String(
                Base64.decode(raw, Base64.NO_WRAP or Base64.URL_SAFE),
                Charsets.UTF_8
            )
            val firstLine = decoded.lineSequence().firstOrNull { it.isNotBlank() } ?: decoded
            ServerConfigParser().parse(firstLine)
        } catch (t: Throwable) {
            ConfigParseResult.Failure("Invalid base64 configuration payload: ${t.message}")
        }
    }

    fun parseJsonConfig(raw: String): ConfigParseResult {
        return try {
            val json = JSONObject(raw)
            val type = json.optString("protocol", json.optString("type", "Unknown"))
            val host = json.optString("host", json.optString("server", ""))
            val port = json.optInt("port", 0)
            val name = json.optString("name", type)
            val user = json.optString("uuid", json.optString("password", ""))

            if (host.isBlank() || port !in 1..65535) {
                return ConfigParseResult.Failure("JSON config is missing a valid host or port")
            }

            ConfigParseResult.Success(
                ServerConfig(
                    type = type,
                    host = host,
                    port = port,
                    username = user.ifBlank { null },
                    protocol = type.lowercase(),
                    name = name
                )
            )
        } catch (t: Throwable) {
            ConfigParseResult.Failure("Invalid JSON config payload: ${t.message}")
        }
    }
}
