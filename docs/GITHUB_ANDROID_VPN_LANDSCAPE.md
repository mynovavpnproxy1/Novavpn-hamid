# GitHub survey: Android VPN applications and client implementations

Survey date: 2026-11-04

## Search scope and counts

I ran 19 GitHub repository searches using variations of Android VPN app/client/VpnService, OpenVPN, WireGuard, SoftEther, Outline, Shadowsocks, Clash, V2Ray, Xray, sing-box, VLESS, Russian-language VPN terms, and Nova VPN.

The search API returned **431 distinct repository names** and **414 distinct owner logins** across those result sets.

These are discovery counts, NOT a verified count of 414 individual human developers or 431 complete VPN implementations. GitHub search includes forks, organizations, demos, libraries, stale projects, and false-positive matches. One owner may be an organization or team; one developer may own multiple repositories. This is a broad search map, not a people census.

## Important NovaVPN finding

### Ron4i/NovaVPN
- App repository: https://github.com/Ron4i/NovaVPN
- Source repository: https://github.com/Ron4i/novavpn-source
- The source repository README explicitly says: Source code is closed.
- The Russian-language NovaVPN you mentioned appears to be this project, but its Android implementation is not publicly inspectable. Its product README can be studied, not its internal code.
- A separate result, https://github.com/03hitalin21/NovaFreeVPN, returns a README identifying itself as v2rayNG and describing Xray/v2fly support. Treat it as a v2rayNG-derived/fork-style candidate, not as the Ron4i NovaVPN source.

## Curated projects worth studying

### Closest technical matches for NovaVPN (Xray/VLESS)
1. https://github.com/2dust/v2rayNG — established Android V2Ray/Xray client; service lifecycle, VPN TUN, engine start/stop, notifications.
2. https://github.com/Q7DF1/XrayFA — Kotlin Multiplatform Xray client; Xray native bridge, hev tun2socks, profile/subscription imports, Android VpnService.
3. https://github.com/SuOracle/GRoute — Android Xray client with Persian/English UI; VLESS/VMess/Trojan/Shadowsocks/Hysteria2/WireGuard/IKEv2, subscriptions, routing and diagnostics.
4. https://github.com/MatsuriDayo/NekoBoxForAndroid — sing-box Android client; broad protocol coverage and subscription parsing.
5. https://github.com/KOKosaaaa/NetGuard — Xray-based Android client; authenticated local SOCKS bridge, TUN forwarding, network switching, service checks, auto server selection.
6. https://github.com/V2Ray-Android/Actinium — V2Ray Android client project.
7. https://github.com/Begzar/BegzarApp — Android proxy/VPN client candidate; inspect current source and build state before adopting.
8. https://github.com/WhiteDNS/WhiteDNS-Android — Android proxy/VPN project candidate; verify current protocol and lifecycle implementation in source.
9. https://github.com/03hitalin21/NovaFreeVPN — repository README identifies it as v2rayNG and references Xray/v2fly; likely a derivative/fork candidate.

### Other complete Android VPN products
10. https://github.com/ProtonVPN/android-app — production VPN app; build instructions, Kotlin/Java, CI and tests.
11. https://github.com/ivpn/android-app — OpenVPN + WireGuard, kill switch, multi-hop, trusted networks and custom DNS.
12. https://github.com/schwabe/ics-openvpn — OpenVPN for Android; Android VpnService implementation and explicit GPL obligations.
13. https://github.com/OutlineFoundation/outline-apps — cross-platform Outline/Shadowsocks client and server manager.
14. https://github.com/SagerNet/sing-box-for-android — sing-box Android application.
15. https://github.com/gayanvoice/android-vpn-client-ics-openvpn — Android OpenVPN-based client candidate.
16. https://github.com/Shoolife/ToBeVPN-Android — Russian-language proprietary Android VPN client with subscription, server list, per-app split tunneling, speed test and encrypted local storage; source is not freely reusable.
17. https://github.com/WhiteDNS/WhiteVPN — additional VPN product candidate from repository search.

## What is reusable as engineering knowledge

For NovaVPN's current VLESS/Xray product, the most relevant source-level references are v2rayNG, XrayFA, GRoute and NetGuard. Their common architecture is:
- Android VpnService owns the TUN interface.
- Xray/sing-box runs as a distinct engine/native component.
- TUN traffic is forwarded to a local proxy through tun2socks.
- Profile import/normalization is separate from engine configuration generation.
- Service lifecycle and notifications are owned by the service, not inferred from a UI Boolean.
- Startup failure, stop, network changes, and duplicate start commands require explicit resource cleanup and state transitions.
- Latency/availability checks should be bounded and cancellable; successful import or local engine launch is not proof that a remote server works.

## Recommended order for deeper source review

1. v2rayNG: exact VPN service, engine controller, notification actions, startup locking, teardown.
2. XrayFA: current Kotlin/native bridge and TUN architecture; compare its dependency/build pipeline with NovaVPN.
3. GRoute: Persian-language feature set, import paths, subscription handling and routing features.
4. NetGuard: auto server selection, authenticated local bridge, network handover and server/service reachability testing.
5. NekoBox: profile normalization and sing-box support as a possible future separate engine adapter.

## License warning

Do not copy source until the exact license and obligations are checked. Open-source projects may require preserving notices, publishing derivative source, or complying with copyleft terms. Russian NovaVPN (Ron4i) and ToBeVPN explicitly describe proprietary restrictions; do not decompile or copy their application code.
