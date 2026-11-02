# Sing-box / libbox evaluation notes

## Stage 3: modular VPN engine integration

This project is intentionally structured so that the app does not claim live VPN support before the engine has been verified.

### Official evaluation

The official Android integration path for sing-box is the `libbox` project, which wraps the native sing-box engine for Android and is designed to work with Android `VpnService` and TUN-based networking.

### Compatibility and licensing check

Before a production release, the following must be verified:

- Android SDK compatibility and minimum Android API support
- Gradle and native build compatibility for the final app target
- JNI / native library packaging and distribution requirements
- Whether the distribution model complies with the GPL-3.0 licensing terms of sing-box/libbox
- Whether the app can legally ship with a GPL-linked engine or whether a different licensing/architecture is required

### Important rule

This project does not claim support for any protocol until actual connectivity has been verified with real traffic tests.

The current implementation is intentionally conservative:

- engine abstraction is present
- sing-box and WireGuard placeholders are created at the architectural level only
- actual tunnel start calls fail with clear, explicit messages rather than reporting fake success

### Remaining stages

The next work will focus on:

1. real configuration import and validation
2. server management and parsing
3. traffic verification and the actual tunnel lifecycle
4. UI polish and the ad flow
5. final security and release review
