# Open-source VPN project study for NovaVPN

Date: 2026-11-04

This document compares public Android VPN projects to identify implementation patterns that can be applied to NovaVPN. It is based on source files and project documentation actually inspected; it does not claim access to closed-source repositories.

## Projects inspected

### 1. Ron4i/NovaVPN (Russian-language NovaVPN)
Repository: https://github.com/Ron4i/NovaVPN

The README is Russian-language and describes:
- Automatic collection of public proxy configurations.
- Filtering/testing candidates and selecting fast servers using ping.
- Country selection and automatic server selection.
- User-imported custom configs/subscriptions.
- Android/iOS clients using V2Ray/Xray; Windows using sing-box.
- A public community pool of submitted configurations.

Important limitation: the repository README and LICENSE explicitly state that the application source is closed/proprietary. The README describes a roughly 158 MB Android APK, but there is no inspectable Android source in this repository. The sibling `Ron4i/novavpn-source` repository did not expose a README/source through the inspected public contents endpoint. Therefore the claims above are product documentation, not independently verified implementation details. Do not copy or decompile this app.

Useful product ideas only: server pool management, latency-based ranking, auto-select, country filters, and optional custom profile import. NovaVPN can implement these independently using its own code and permitted public sources.

### 2. 2dust/v2rayNG
Repository: https://github.com/2dust/v2rayNG

Inspected:
- `V2rayNG/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt`
- `V2rayNG/app/src/main/java/com/v2ray/ang/core/LauncherManager.kt`
- `V2rayNG/app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt`
- `V2rayNG/app/src/main/java/com/v2ray/ang/service/AGENTS.md`
- Project README.

Engineering patterns:
- The VPN service owns the Android TUN descriptor and its lifecycle.
- Setup is staged: foreground notification first, permission check, VPN builder configuration, TUN establishment, tun2socks setup, then core loop start.
- IPv4/IPv6 routing, DNS, MTU, bypass-LAN and per-app routing are explicit configuration concerns rather than hidden assumptions.
- Startup is guarded against duplicate concurrent starts with an atomic lock.
- Failures release partial resources and stop the service instead of leaving a half-started VPN.
- Stop ordering is deliberate: prevent new work, stop tun2socks/core, then close the VPN interface; cleanup is guarded against double close.
- Android system restarts (null intent / SERVICE_INTERFACE) are handled separately from app-initiated starts.
- The foreground notification includes explicit Stop and Restart actions.
- UI does not decide that the native core is running using a cached Boolean; daemon/service state is the authority and commands/results go through a service-control layer.
- The project documents idempotency, service-owned coroutine cancellation, explicit acknowledgements, and physical-device validation for lifecycle changes.

This is the closest direct reference for NovaVPN's current Xray/VLESS architecture.

### 3. MatsuriDayo/NekoBoxForAndroid
Repository: https://github.com/MatsuriDayo/NekoBoxForAndroid

The README identifies it as a sing-box-based universal proxy client for Android. It documents support for SOCKS, HTTP(S), SSH, Shadowsocks, VMess, Trojan, VLESS, AnyTLS, ShadowTLS, TUIC, Hysteria, WireGuard and plugin-based protocols. It supports common subscription formats (including Clash Meta and v2rayN) and sing-box outbound definitions.

Useful architectural/product direction:
- A single Android client can support multiple protocols through a unified engine abstraction, provided profile parsing and engine translation are explicit.
- Subscription import should normalize profiles into a common internal model, then translate to the selected engine format.
- Protocol support must be stated per engine/plugin; do not claim that accepting a link means the engine can execute it.
- NekoBox is GPL-3.0. Reusing its code requires license compliance; architecture can be studied without copying implementation.

## What these findings mean for NovaVPN

### Keep
- Existing Android app and user-facing workflow.
- Existing Xray/VLESS compatibility and imported profile data.
- A single service as owner of VPN interface, engine process, notification, and lifecycle.

### Fix first
1. Do not set CONNECTED immediately after Xray accepts a start request. That only proves local startup was accepted, not that a remote proxy is reachable or user traffic passes.
2. Introduce explicit states: IDLE, CONNECTING, CONNECTED, STOPPING, STOPPED, FAILED.
3. Drive state changes from authoritative Xray/core callbacks where available. If callbacks do not provide a meaningful readiness signal, use a bounded, cancellable health probe through the proxy and label the UI state accurately.
4. Make start idempotent: duplicate taps/commands must not create multiple cores, TUN descriptors, notifications, or workers.
5. Make stop idempotent and ordered: invalidate pending startup, stop core, close TUN once, clear persisted state, remove notification, broadcast STOPPED.
6. Handle onRevoke, onDestroy, null restart intents, and startup failure with the same resource-ownership rules.
7. Ensure notification Stop/Restart actions send explicit service commands and reflect the actual service state.
8. Add structured, non-secret lifecycle logs for each phase and failure.

### Later, after the connection lifecycle is reliable
- Add optional server latency testing and auto-selection, as described by the Russian NovaVPN README.
- Add subscription refresh and normalized profile import only after validating protocol-specific conversion.
- Consider sing-box as a separate engine adapter for protocols not covered by the current Xray integration. Do not replace Xray until the current VLESS path remains covered and a tested migration exists.

## Source links

- Russian NovaVPN product README: https://github.com/Ron4i/NovaVPN/blob/main/README.md
- Russian NovaVPN proprietary license: https://github.com/Ron4i/NovaVPN/blob/main/LICENSE
- v2rayNG VPN service: https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt
- v2rayNG service lifecycle guide: https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/service/AGENTS.md
- v2rayNG launcher: https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/core/LauncherManager.kt
- v2rayNG notification manager: https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt
- NekoBox README: https://github.com/MatsuriDayo/NekoBoxForAndroid/blob/main/README.md

## Scope and licensing note

This is an architectural study, not a copy of third-party source. v2rayNG and NekoBox have open-source license obligations (check current repository license files before reusing code). The Russian NovaVPN project is explicitly proprietary; do not copy, decompile, or create derivative work from its application.
