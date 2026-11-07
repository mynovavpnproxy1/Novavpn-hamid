package com.novavpn.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import go.Seq;
import libv2ray.CoreCallbackHandler;
import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class NovaVpnService extends VpnService {
    private static final String TAG = "NOVA VPN";
    private static final String CHANNEL_ID = "nova_vpn_channel";
    private static final int NOTIFICATION_ID = 1001;

    private ParcelFileDescriptor vpnInterface;
    private CoreController coreController;
    private static volatile boolean engineRunning = false;
    private static volatile String lastError = "";

    public static boolean isEngineRunning() { return engineRunning; }
    public static String getLastError() { return lastError; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if ("CONNECT".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification("در حال راه‌اندازی موتور VPN"));
            String vlessUri = intent.getStringExtra("vlessUri");
            startTunnel(vlessUri);
        } else if ("DISCONNECT".equals(action)) {
            disconnectVpn();
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startTunnel(String vlessUri) {
        try {
            if (vlessUri == null || vlessUri.trim().isEmpty()) {
                throw new IllegalArgumentException("ابتدا لینک VLESS را وارد کنید");
            }
            stopCore();
            closeInterface();

            Builder builder = new Builder();
            builder.setSession("NOVA VPN");
            builder.setMtu(1500);
            builder.addAddress("10.10.10.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.addAddress("fd00:10:10::2", 128);
            builder.addRoute("::", 0);
            // Exclude NOVA itself so Xray's upstream sockets do not loop into its own TUN.
            builder.addDisallowedApplication(getPackageName());

            vpnInterface = builder.establish();
            if (vpnInterface == null) throw new IllegalStateException("ساخت رابط VPN توسط اندروید ناموفق بود");

            Seq.setContext(getApplicationContext());
            Libv2ray.initCoreEnv(getFilesDir().getAbsolutePath(), "");
            String config = buildXrayConfig(vlessUri);
            coreController = Libv2ray.newCoreController(new CoreCallbackHandler() {
                @Override public long startup() {
                    engineRunning = true;
                    lastError = "";
                    updateNotification("اتصال موتور Xray برقرار شد");
                    Log.i(TAG, "Xray core started");
                    return 0;
                }
                @Override public long shutdown() {
                    engineRunning = false;
                    Log.i(TAG, "Xray core stopped");
                    return 0;
                }
                @Override public long onEmitStatus(long status, String message) {
                    Log.i(TAG, "Xray status " + status + ": " + message);
                    return 0;
                }
            });
            coreController.startLoop(config, vpnInterface.getFd());
            if (!coreController.getIsRunning()) {
                throw new IllegalStateException("موتور Xray اجرا نشد");
            }
            engineRunning = true;
            updateNotification("موتور Xray فعال است");
        } catch (Exception e) {
            lastError = e.getMessage() == null ? "خطای نامشخص در راه‌اندازی VPN" : e.getMessage();
            engineRunning = false;
            Log.e(TAG, "VPN startup failed", e);
            stopCore();
            closeInterface();
            stopForeground(true);
            stopSelf();
        }
    }

    private String buildXrayConfig(String shareUri) throws Exception {
        URI uri = new URI(shareUri.trim());
        if (!"vless".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("فعلاً فقط لینک‌های VLESS پشتیبانی می‌شوند");
        }
        String uuid = uri.getUserInfo();
        String host = uri.getHost();
        int port = uri.getPort();
        if (uuid == null || uuid.isEmpty() || host == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("ساختار لینک VLESS معتبر نیست");
        }

        Map<String, String> q = parseQuery(uri.getRawQuery());
        String network = q.getOrDefault("type", "tcp").toLowerCase();
        String security = q.getOrDefault("security", "none").toLowerCase();
        if (!network.equals("tcp") && !network.equals("ws") && !network.equals("grpc")) {
            throw new IllegalArgumentException("نوع انتقال فعلاً پشتیبانی نمی‌شود: " + network);
        }
        if (!security.equals("none") && !security.equals("tls") && !security.equals("reality")) {
            throw new IllegalArgumentException("نوع امنیت لینک پشتیبانی نمی‌شود: " + security);
        }

        JSONObject user = new JSONObject();
        user.put("id", uuid);
        user.put("encryption", q.getOrDefault("encryption", "none"));
        if (q.containsKey("flow") && !q.get("flow").isEmpty()) user.put("flow", q.get("flow"));

        JSONObject server = new JSONObject();
        server.put("address", host);
        server.put("port", port);
        server.put("users", new JSONArray().put(user));

        JSONObject outbound = new JSONObject();
        outbound.put("tag", "proxy");
        outbound.put("protocol", "vless");
        outbound.put("settings", new JSONObject().put("vnext", new JSONArray().put(server)));

        JSONObject stream = new JSONObject();
        stream.put("network", network);
        stream.put("security", security);

        if ("tls".equals(security)) {
            JSONObject tls = new JSONObject();
            tls.put("serverName", q.getOrDefault("sni", host));
            tls.put("fingerprint", q.getOrDefault("fp", "chrome"));
            tls.put("allowInsecure", false);
            if (q.containsKey("alpn")) tls.put("alpn", new JSONArray(q.get("alpn").split(",")));
            stream.put("tlsSettings", tls);
        } else if ("reality".equals(security)) {
            String publicKey = q.get("pbk");
            if (publicKey == null || publicKey.isEmpty()) {
                throw new IllegalArgumentException("پارامتر کلید عمومی Reality (pbk) در لینک وجود ندارد");
            }
            JSONObject reality = new JSONObject();
            reality.put("serverName", q.getOrDefault("sni", host));
            reality.put("fingerprint", q.getOrDefault("fp", "chrome"));
            reality.put("publicKey", publicKey);
            reality.put("shortId", q.getOrDefault("sid", ""));
            reality.put("spiderX", q.getOrDefault("spx", "/"));
            stream.put("realitySettings", reality);
        }

        if ("ws".equals(network)) {
            JSONObject ws = new JSONObject();
            ws.put("path", q.getOrDefault("path", "/"));
            String wsHost = q.getOrDefault("host", "");
            if (!wsHost.isEmpty()) ws.put("headers", new JSONObject().put("Host", wsHost));
            stream.put("wsSettings", ws);
        } else if ("grpc".equals(network)) {
            JSONObject grpc = new JSONObject();
            grpc.put("serviceName", q.getOrDefault("serviceName", ""));
            grpc.put("multiMode", "multi".equalsIgnoreCase(q.getOrDefault("mode", "")));
            stream.put("grpcSettings", grpc);
        }
        outbound.put("streamSettings", stream);

        JSONObject direct = new JSONObject().put("tag", "direct").put("protocol", "freedom")
            .put("settings", new JSONObject());
        JSONObject block = new JSONObject().put("tag", "block").put("protocol", "blackhole")
            .put("settings", new JSONObject());

        JSONObject tunSettings = new JSONObject();
        tunSettings.put("name", "nova-tun");
        tunSettings.put("mtu", 1500);
        JSONObject tun = new JSONObject().put("tag", "tun-in").put("protocol", "tun")
            .put("settings", tunSettings);
        JSONObject sniffing = new JSONObject().put("enabled", true)
            .put("destOverride", new JSONArray().put("http").put("tls").put("quic"));
        tun.put("sniffing", sniffing);

        JSONArray rules = new JSONArray();
        rules.put(new JSONObject().put("type", "field")
            .put("ip", new JSONArray().put("geoip:private")).put("outboundTag", "direct"));

        JSONObject config = new JSONObject();
        config.put("log", new JSONObject().put("loglevel", "warning"));
        config.put("dns", new JSONObject().put("servers", new JSONArray().put("1.1.1.1").put("8.8.8.8")));
        config.put("inbounds", new JSONArray().put(tun));
        config.put("outbounds", new JSONArray().put(outbound).put(direct).put(block));
        config.put("routing", new JSONObject().put("domainStrategy", "AsIs").put("rules", rules));
        return config.toString();
    }

    private Map<String, String> parseQuery(String raw) throws Exception {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isEmpty()) return values;
        for (String item : raw.split("&")) {
            String[] pair = item.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()) : "";
            values.put(key, value);
        }
        return values;
    }

    private void stopCore() {
        if (coreController != null) {
            try { coreController.stopLoop(); }
            catch (Exception e) { Log.w(TAG, "Xray stop failed", e); }
            coreController = null;
        }
        engineRunning = false;
    }

    private void closeInterface() {
        if (vpnInterface != null) {
            try { vpnInterface.close(); }
            catch (Exception e) { Log.w(TAG, "TUN close failed", e); }
            vpnInterface = null;
        }
    }

    private void disconnectVpn() {
        stopCore();
        closeInterface();
        lastError = "";
    }

    private Notification createNotification(String content) {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NOVA VPN")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, createNotification(content));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "NOVA VPN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        disconnectVpn();
        super.onDestroy();
    }
}
