package com.novavpn.app.config

import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

private fun decodeBase64Payload(raw: String): String? {
    val cleaned = raw.trim().replace("-", "+").replace("_", "/")
    val padded = if (cleaned.length % 4 != 0) cleaned + "=".repeat(4 - (cleaned.length % 4)) else cleaned
    return try {
        String(Base64.decode(padded, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING), Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

data class ServerConfig(
    val type: String,
    val host: String,
    val port: Int,
    val username: String? = null,
    val protocol: String? = null,
    val name: String? = null
)

sealed class ConfigParseResult {
    data class Success(val server: ServerConfig) : ConfigParseResult()
    data class Failure(val message: String) : ConfigParseResult()
}

class ServerConfigParser {
    fun parse(rawConfig: String): ConfigParseResult {
        val input = rawConfig.trim()
        if (input.isEmpty()) {
            return ConfigParseResult.Failure("Config is empty.")
        }

        return when {
            input.startsWith("vmess://", ignoreCase = true) -> parseVmess(input)
            input.startsWith("vless://", ignoreCase = true) -> parseVless(input)
            input.startsWith("trojan://", ignoreCase = true) -> parseTrojan(input)
            input.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(input)
            input.startsWith("wg://", ignoreCase = true) || input.contains("private_key=") -> parseWireGuard(input)
            input.contains("://") && input.contains("@") -> parseUrlLike(input)
            input.contains(":") -> parseHostPort(input)
            else -> ConfigParseResult.Failure("Unsupported config format. Use VLESS, VMess, Trojan, Shadowsocks, WireGuard, or host:port.")
        }
    }

    private fun parseVmess(raw: String): ConfigParseResult {
        val encoded = raw.removePrefix("vmess://", ignoreCase = true)
        val decoded = decodeBase64Payload(encoded) ?: return ConfigParseResult.Failure("VMess config is not valid Base64.")
        return try {
            val json = JSONObject(decoded)
            val host = json.optString("add", "").trim()
            val port = json.optInt("port", 443)
            if (host.isEmpty()) {
                return ConfigParseResult.Failure("VMess config is missing host.")
            }
            ConfigParseResult.Success(
                ServerConfig(
                    type = "VMess",
                    host = host,
                    port = port,
                    username = json.optString("id", null),
                    protocol = json.optString("net", "tcp"),
                    name = json.optString("ps", "VMess")
                )
            )
        } catch (_: Exception) {
            ConfigParseResult.Failure("VMess config could not be parsed.")
        }
    }

    private fun parseVless(raw: String): ConfigParseResult {
        return try {
            val uri = URI(raw)
            val host = uri.host ?: return ConfigParseResult.Failure("VLESS config is missing host.")
            val port = if (uri.port > 0) uri.port else 443
            val protocol = queryParam(uri.query, "type") ?: "tcp"
            val user = uri.userInfo ?: "vless-user"
            val name = uri.fragment ?: "VLESS"
            ConfigParseResult.Success(
                ServerConfig(
                    type = "VLESS",
                    host = host,
                    port = port,
                    username = user,
                    protocol = protocol,
                    name = name
                )
            )
        } catch (_: Exception) {
            ConfigParseResult.Failure("VLESS config could not be parsed.")
        }
    }

    private fun parseTrojan(raw: String): ConfigParseResult {
        return try {
            val uri = URI(raw)
            val host = uri.host ?: return ConfigParseResult.Failure("Trojan config is missing host.")
            val port = if (uri.port > 0) uri.port else 443
            val user = uri.userInfo ?: "trojan-user"
            val protocol = queryParam(uri.query, "type") ?: "tcp"
            val name = uri.fragment ?: "Trojan"
            ConfigParseResult.Success(
                ServerConfig(
                    type = "Trojan",
                    host = host,
                    port = port,
                    username = user,
                    protocol = protocol,
                    name = name
                )
            )
        } catch (_: Exception) {
            ConfigParseResult.Failure("Trojan config could not be parsed.")
        }
    }

    private fun parseShadowsocks(raw: String): ConfigParseResult {
        val withoutScheme = raw.removePrefix("ss://", ignoreCase = true)
        val atIndex = withoutScheme.lastIndexOf('@')
        if (atIndex <= 0) {
            return ConfigParseResult.Failure("Shadowsocks config is malformed.")
        }

        val encodedUserInfo = withoutScheme.substring(0, atIndex)
        val serverPart = withoutScheme.substring(atIndex + 1)

        val decodedUserInfo = decodeBase64Payload(encodedUserInfo) ?: return ConfigParseResult.Failure("Shadowsocks credentials are invalid.")
        val hostAndPort = serverPart.split(":")
        if (hostAndPort.size < 2) {
            return ConfigParseResult.Failure("Shadowsocks host or port is missing.")
        }

        val host = hostAndPort.first()
        val port = hostAndPort.last().toIntOrNull() ?: return ConfigParseResult.Failure("Shadowsocks port is invalid.")

        return ConfigParseResult.Success(
            ServerConfig(
                type = "Shadowsocks",
                host = host,
                port = port,
                username = decodedUserInfo,
                protocol = "ss",
                name = "Shadowsocks"
            )
        )
    }

    private fun parseWireGuard(raw: String): ConfigParseResult {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ConfigParseResult.Failure("WireGuard config is empty.")
        }

        val values = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.contains("=")) {
                val (key, value) = line.split("=", limit = 2)
                values[key.trim()] = value.trim()
            }
        }

        val host = values["address"]?.split("/")?.firstOrNull()?.trim() ?: values["endpoint"]?.substringBefore(":")
        val port = values["endpoint"]?.substringAfterLast(":")?.toIntOrNull() ?: 51820
        if (host.isNullOrBlank()) {
            return ConfigParseResult.Failure("WireGuard config is missing a valid endpoint or address.")
        }

        return ConfigParseResult.Success(
            ServerConfig(
                type = "WireGuard",
                host = host,
                port = port,
                username = values["public_key"] ?: "wireguard-user",
                protocol = "wireguard",
                name = "WireGuard"
            )
        )
    }

    private fun parseUrlLike(raw: String): ConfigParseResult {
        return try {
            val uri = URI(raw)
            val host = uri.host ?: return ConfigParseResult.Failure("URL host is missing.")
            val port = if (uri.port > 0) uri.port else 443
            val user = uri.userInfo ?: "user"
            ConfigParseResult.Success(
                ServerConfig(
                    type = uri.scheme?.uppercase() ?: "URL",
                    host = host,
                    port = port,
                    username = user,
                    protocol = uri.scheme,
                    name = uri.host
                )
            )
        } catch (_: Exception) {
            ConfigParseResult.Failure("Could not parse URL-like config.")
        }
    }

    private fun parseHostPort(raw: String): ConfigParseResult {
        val candidate = raw.trim()
        val hostPort = candidate.split(":")
        if (hostPort.size != 2) {
            return ConfigParseResult.Failure("Expected format: host:port")
        }
        val host = hostPort[0].trim()
        val port = hostPort[1].trim().toIntOrNull() ?: return ConfigParseResult.Failure("Port is not valid.")
        if (host.isEmpty()) {
            return ConfigParseResult.Failure("Host is empty.")
        }
        return ConfigParseResult.Success(
            ServerConfig(
                type = "Host",
                host = host,
                port = port,
                username = null,
                protocol = "tcp",
                name = host
            )
        )
    }

    private fun queryParam(query: String?, key: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split("&").firstNotNullOfOrNull { item ->
            val parts = item.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == key) URLDecoder.decode(parts[1], "UTF-8") else null
        }
    }
}
