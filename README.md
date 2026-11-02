# NOVA VPN

A production-oriented Android VPN architecture project prepared for the `mynovavpnproxy1/Novavpn-hamid` repository.

## Current status

This repository was previously empty. The project is now initialized as a native Android application using Kotlin and Jetpack Compose.

## Architecture goals

- Android native app in Kotlin
- Jetpack Compose UI
- Android VpnService foundation
- Modular engine architecture for future VPN/proxy engines
- Preparation for sing-box/libbox integration and other compatible engines
- Persian and English support
- Dark navy visual identity for NOVA VPN

## Important notes

- This stage establishes the project skeleton only.
- Actual protocol support and live tunnel connectivity are not claimed yet.
- sing-box/libbox integration will be evaluated in the following stage with licensing and compatibility review, because the official sing-box Android integration is GPL-3.0 based and must be reviewed before distribution.

## Project stages

1. Repository inspection and architecture review
2. Native Android project setup
3. VPN engine integration
4. Configuration import and server management
5. Real connection and traffic verification
6. NOVA user interface
7. Advertisement integration and five-second close behavior
8. Two-hour automatic disconnect
9. Security review, testing, and release build

## Repository layout

- `app/` — Android application module
- `build.gradle.kts` — root Gradle settings
- `settings.gradle.kts` — project configuration
- `gradle.properties` — Gradle tuning

## Next step

The next stage is to implement the engine abstraction and evaluate the official sing-box/libbox integration with actual compatibility and licensing checks.
