import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val secretProperties = Properties()
val secretPropertiesFile = rootProject.file(".secrets/release-signing.properties")
if (secretPropertiesFile.exists()) {
    secretProperties.load(FileInputStream(secretPropertiesFile))
}

val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
    ?: secretProperties.getProperty("RELEASE_STORE_FILE")
    ?: ""
val storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
    ?: secretProperties.getProperty("RELEASE_STORE_PASSWORD")
    ?: ""
val keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
    ?: secretProperties.getProperty("RELEASE_KEY_ALIAS")
    ?: ""
val keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
    ?: secretProperties.getProperty("RELEASE_KEY_PASSWORD")
    ?: ""
val hasSigningInfo = storeFilePath.isNotBlank() && storePassword.isNotBlank() && keyAlias.isNotBlank() && keyPassword.isNotBlank()

android {
    namespace = "com.app.novavpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.novavpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 30071366
        versionName = "4.6.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (hasSigningInfo) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigningInfo) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
