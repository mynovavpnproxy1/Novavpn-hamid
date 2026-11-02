# Google Play Publishing Readiness Checklist

This project is being developed with Google Play compliance in mind, but publication still requires final review by the developer and Google Play policy team.

## Core publishing principles followed in this project

- No fake connection states or misleading status indicators
- No secret keys or credentials embedded in source code
- No deceptive ad behavior or forced ad flow without user consent
- No network interception outside the user-approved VPN flow
- Android VpnService is used only when the user explicitly grants permission
- Foreground service and notification handling are implemented for stable background VPN behavior
- Sensitive configuration is separated from app logic and will be stored securely when live servers are added

## Major risks that must be resolved before release

### 1. Licensing risk for sing-box/libbox

The official sing-box Android integration path (libbox) is GPL-3.0 licensed. That licensing model is not automatically compatible with closed-source commercial APK distribution. Before publishing, the project must decide either:

- keep the app open source and GPL-compatible, or
- avoid GPL-linked distribution and use a different engine architecture or licensing path

This is a material release blocker for many closed-source app release plans.

### 2. Ad SDK rules

If an advertising SDK is used:

- ads must not be deceptive
- close/dismiss flows must respect platform policies
- user consent and privacy settings must be honored
- no clickjacking or fake close button behavior

### 3. VPN app policy and user trust

VPN apps on Google Play must avoid:

- misleading claims
- malware-like behavior
- unauthorized traffic interception
- bypassing platform restrictions or ad policy
- collecting user data without a clear privacy policy and consent flow

### 4. Permissions and service behavior

The app must:

- request VPN permission only when needed
- handle permission denial gracefully
- call `VpnService.prepare()` with correct user flow
- use foreground service + notification channel for long-running VPN state
- avoid leaking traffic when the VPN is disconnected

## Repository-level intent

This app is intentionally structured to avoid false success claims. It is not shipping a fake "connected" state and does not promise protocol support before real connectivity is verified.

## Required pre-release actions

1. Review the final license model for all included engines.
2. Verify whether the app will be closed-source or open-source.
3. Confirm the ad SDK and consent model.
4. Run a final security review for tokens, keys, and route handling.
5. Test the app on real devices with actual connectivity checks.
6. Submit a production build to Play Console only after the above checks are completed.

## Conclusion

This project is now aligned with a safer Google Play direction, but final Google Play publication approval is not guaranteed until the app is completed, tested, and reviewed under Google Play policy and licensing constraints.
