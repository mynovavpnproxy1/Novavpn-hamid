package com.novavpn.app.engine

import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** Builds Xray JSON from supported share links without discarding transport parameters. */
object XrayConfigBuilder {
    fun build(raw: String): String {
        val input = raw.trim()
        require(input.isNotEmpty()) { "کانفیگ خالی است." }
        val outbound = when {
            input.startsWith("vless://", true) -> buildVless(input)
            input.startsWith("vmess://", true) -> buildVmess(input)
            input.startsWith("trojan://", true) -> buildTrojan(input)
            input.startsWith("ss://", true) -> buildShadowsocks(input)
            else -> throw IllegalArgumentException("این نوع کانفیگ در موتور Xray پشتیبانی نمی‌شود.")
        }
        val tun = JSONObject().put("tag", "tun").put("protocol", "tun")
            .put("settings", JSONObject().put("name", "xray0").put("MTU", 1500).put("userLevel", 8))
        val direct = JSONObject().put("tag", "direct").put("protocol", "freedom")
        val block = JSONObject().put("tag", "block").put("protocol", "blackhole")
        val routing = JSONObject().put("domainStrategy", "AsIs").put("rules", JSONArray())
        return JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("inbounds", JSONArray().put(tun))
            .put("outbounds", JSONArray().put(outbound).put(direct).put(block))
            .put("routing", routing)
            .put("dns", JSONObject().put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8")))
            .toString()
    }

    private fun buildVless(raw: String): JSONObject {
        val uri = Uri.parse(raw)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("آدرس سرور VLESS معتبر نیست.")
        val port = uri.port.takeIf { it in 1..65535 } ?: error("پورت VLESS معتبر نیست.")
        val uuid = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("UUID کانفیگ VLESS وجود ندارد.")
        val network = uri.getQueryParameter("type")?.ifBlank { "tcp" } ?: "tcp"
        val security = uri.getQueryParameter("security")?.ifBlank { "none" } ?: "none"
        val user = JSONObject().put("id", uuid).put("encryption", "none")
        uri.getQueryParameter("flow")?.takeIf { it.isNotBlank() }?.let { user.put("flow", it) }
        val settings = JSONObject().put("vnext", JSONArray().put(
            JSONObject().put("address", host).put("port", port).put("users", JSONArray().put(user))
        ))
        return JSONObject().put("tag", "proxy").put("protocol", "vless")
            .put("settings", settings).put("streamSettings", buildStreamSettings(uri, network, security))
    }

    private fun buildVmess(raw: String): JSONObject {
        val encoded = raw.substringAfter("://").trim()
        val decoded = decodeBase64(encoded) ?: error("اطلاعات VMess قابل رمزگشایی نیست.")
        val source = JSONObject(decoded)
        val host = source.optString("add").takeIf { it.isNotBlank() } ?: error("آدرس VMess وجود ندارد.")
        val port = source.optInt("port", 0).takeIf { it in 1..65535 } ?: error("پورت VMess معتبر نیست.")
        val uuid = source.optString("id").takeIf { it.isNotBlank() } ?: error("UUID مربوط به VMess وجود ندارد.")
        val user = JSONObject().put("id", uuid).put("alterId", source.optInt("aid", 0))
            .put("security", source.optString("scy", "auto"))
        val settings = JSONObject().put("vnext", JSONArray().put(
            JSONObject().put("address", host).put("port", port).put("users", JSONArray().put(user))
        ))
        val network = source.optString("net", "tcp").ifBlank { "tcp" }
        val security = if (source.optString("tls") == "tls") "tls" else "none"
        val vmessHost = source.optString("host")
        val vmessSni = source.optString("sni").ifBlank { vmessHost.ifBlank { host } }
        val vmessPath = source.optString("path")
        val query = Uri.parse("nova://import?type=${network}&security=${security}&sni=${Uri.encode(vmessSni)}&path=${Uri.encode(vmessPath)}&serviceName=${Uri.encode(vmessPath)}&host=${Uri.encode(vmessHost)}")
        return JSONObject().put("tag", "proxy").put("protocol", "vmess")
            .put("settings", settings).put("streamSettings", buildStreamSettings(query, network, security))
    }

    private fun buildTrojan(raw: String): JSONObject {
        val uri = Uri.parse(raw)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("آدرس سرور Trojan معتبر نیست.")
        val port = uri.port.takeIf { it in 1..65535 } ?: 443
        val password = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("رمز عبور Trojan وجود ندارد.")
        val settings = JSONObject().put("servers", JSONArray().put(
            JSONObject().put("address", host).put("port", port).put("password", password)
        ))
        val network = uri.getQueryParameter("type")?.ifBlank { "tcp" } ?: "tcp"
        val security = uri.getQueryParameter("security")?.ifBlank { "tls" } ?: "tls"
        return JSONObject().put("tag", "proxy").put("protocol", "trojan")
            .put("settings", settings).put("streamSettings", buildStreamSettings(uri, network, security))
    }

    private fun buildShadowsocks(raw: String): JSONObject {
        val uri = Uri.parse(raw)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("آدرس سرور Shadowsocks معتبر نیست.")
        val port = uri.port.takeIf { it in 1..65535 } ?: error("پورت Shadowsocks معتبر نیست.")
        val userInfo = uri.userInfo ?: error("اطلاعات رمزگذاری Shadowsocks وجود ندارد.")
        val decoded = if (userInfo.contains(":")) userInfo else decodeBase64(userInfo) ?: error("اطلاعات Shadowsocks نامعتبر است.")
        val method = decoded.substringBefore(":").takeIf { it.isNotBlank() } ?: error("روش رمزگذاری Shadowsocks وجود ندارد.")
        val password = decoded.substringAfter(":", "").takeIf { it.isNotBlank() } ?: error("رمز Shadowsocks وجود ندارد.")
        val server = JSONObject().put("address", host).put("port", port).put("method", method).put("password", password)
        return JSONObject().put("tag", "proxy").put("protocol", "shadowsocks")
            .put("settings", JSONObject().put("servers", JSONArray().put(server)))
    }

    private fun buildStreamSettings(uri: Uri, network: String, security: String): JSONObject {
        val stream = JSONObject().put("network", network).put("security", security)
        when (security) {
            "tls" -> {
                val tlsServerName = uri.getQueryParameter("sni")
                    ?.takeIf { it.isNotBlank() }
                    ?: uri.getQueryParameter("host")?.takeIf { it.isNotBlank() }
                    ?: uri.host.orEmpty()
                val tls = JSONObject().put("serverName", tlsServerName).put("allowInsecure", false)
                uri.getQueryParameter("fp")?.takeIf { it.isNotBlank() }?.let { tls.put("fingerprint", it) }
                uri.getQueryParameter("alpn")?.takeIf { it.isNotBlank() }?.let { tls.put("alpn", JSONArray(it.split(",").map { item -> item.trim() })) }
                stream.put("tlsSettings", tls)
            }
            "reality" -> {
                val realitySni = uri.getQueryParameter("sni")?.takeIf { it.isNotBlank() }
                    ?: error("پارامتر SNI در کانفیگ REALITY وجود ندارد.")
                val realityPublicKey = uri.getQueryParameter("pbk")?.takeIf { it.isNotBlank() }
                    ?: error("کلید عمومی REALITY (pbk) وجود ندارد.")
                val reality = JSONObject().put("serverName", realitySni)
                    .put("fingerprint", uri.getQueryParameter("fp") ?: "chrome")
                    .put("publicKey", realityPublicKey)
                    .put("shortId", uri.getQueryParameter("sid").orEmpty())
                    .put("spiderX", uri.getQueryParameter("spx") ?: "/")
                stream.put("realitySettings", reality)
            }
            "none" -> Unit
            else -> error("نوع امنیت پشتیبانی نشده است: " + security)
        }
        when (network) {
            "tcp" -> Unit
            "ws" -> {
                val headers = JSONObject()
                uri.getQueryParameter("host")?.takeIf { it.isNotBlank() }?.let { headers.put("Host", it) }
                stream.put("wsSettings", JSONObject().put("path", uri.getQueryParameter("path") ?: "/").put("headers", headers))
            }
            "grpc" -> stream.put("grpcSettings", JSONObject()
                .put("serviceName", uri.getQueryParameter("serviceName") ?: uri.getQueryParameter("service") ?: "")
                .put("multiMode", uri.getQueryParameter("mode") == "multi"))
            "httpupgrade" -> stream.put("httpupgradeSettings", JSONObject()
                .put("host", uri.getQueryParameter("host").orEmpty()).put("path", uri.getQueryParameter("path") ?: "/"))
            else -> error("نوع انتقال پشتیبانی نشده است: " + network)
        }
        return stream
    }

    private fun decodeBase64(value: String): String? {
        val normalized = value.substringBefore("#").replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return try { String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8) } catch (_: Exception) { null }
    }
}
