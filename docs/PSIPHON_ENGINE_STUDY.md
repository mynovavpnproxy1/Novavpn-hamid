# Psiphon engine study for NovaVPN

## Source of truth
- Android client: https://github.com/Psiphon-Inc/psiphon-android
- Tunnel engine: https://github.com/Psiphon-Labs/psiphon-tunnel-core
- Android library integration guide: https://github.com/Psiphon-Labs/psiphon-tunnel-core/blob/master/MobileLibrary/Android/README.md

## Findings
The official Android client does not treat "connected" as successful merely because a local TUN interface or core loop started. Its service owns the full lifecycle:
1. Create a `PsiphonTunnel` instance using `PsiphonTunnel.newPsiphonTunnel(hostService)`.
2. Start the engine with provisioned Psiphon server entries using `startTunneling(...)`.
3. Receive engine connection-state callbacks and only publish CONNECTED when the engine reports its actual network state.
4. Route Android VPN traffic through the tunnel's local SOCKS proxy via the VPN manager.
5. Keep the service, tunnel state, foreground notification, and activity state synchronized through service IPC.
6. Stop the engine through its own lifecycle and wait for shutdown before removing the foreground service.

The official Android app uses `TunnelVpnService` + `TunnelManager` + `TunnelServiceInteractor`, and embeds the Psiphon Android library (`ca.psiphon.aar`). The library is built from `psiphon-tunnel-core` with gomobile; it is not just a Java dependency or a generic VLESS parser.

## Important protocol boundary
Psiphon is its own relay network and uses Psiphon server entries/configuration. It is **not** a VLESS/VMess/Trojan client and cannot consume the VLESS share links currently imported by NovaVPN. Replacing NovaVPN's Xray engine with Psiphon would therefore remove compatibility with those imported profiles unless the product intentionally changes to a Psiphon-network client. The two engines must not be conflated.

## NovaVPN migration direction
- Preserve the existing NovaVPN application and UI.
- Keep the current Xray/VLESS path for user-imported VLESS profiles.
- Adopt the proven Psiphon lifecycle principles: authoritative engine callbacks, explicit CONNECTING/CONNECTED/STOPPING/STOPPED/FAILED states, service-owned TUN lifetime, ordered shutdown, and notification actions that command the service rather than merely changing UI state.
- Do not report CONNECTED based solely on `startLoop()` returning. Confirm actual outbound tunnel readiness/traffic through the engine's supported callbacks or a bounded health check.
- Any future Psiphon mode must be a separate, explicit connection type backed by provisioned Psiphon server entries and the official tunnel-core library.
- Before distributing Psiphon-derived or Psiphon-linked code, preserve GPLv3 notices and verify NovaVPN's license/distribution compatibility.

## Current NovaVPN defect to address
`NovaVpnService.onStartCommand()` currently marks the VPN connected immediately after `XrayRuntime.start()` returns. That proves the core accepted startup, not that a remote proxy connection is working or that user traffic is being forwarded. The connection state must be driven by a real engine readiness signal, and shutdown must clear the foreground notification and persisted state exactly once.
