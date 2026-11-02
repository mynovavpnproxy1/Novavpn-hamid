# Hidden signing and ownership configuration

This folder is intentionally hidden and meant for local release metadata only.

Do not commit real secrets or keys to Git.
The files in this folder are ignored by Git via `.gitignore`.

Use this folder for:
- app owner / company name
- developer contact
- Google Play account info
- keystore alias and passwords
- release configuration values

Recommended flow:
1. Open `release-signing.properties.example`
2. Copy its contents into a new file named `release-signing.properties`
3. Fill in the real values locally
4. Keep the actual `.jks` keystore file in this folder as well

Important:
- `release-signing.properties` must stay local
- `*.jks` must stay local
- never upload these to GitHub

Fields you should fill later:
- APP_PACKAGE_NAME
- APP_OWNER_NAME
- APP_DEVELOPER_EMAIL
- GOOGLE_PLAY_ACCOUNT
- KEYSTORE_FILE_NAME
- KEYSTORE_ALIAS
- STORE_PASSWORD
- KEY_PASSWORD
- RELEASE_STORE_FILE
- RELEASE_STORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD
