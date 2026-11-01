plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// RetroArch package names, tried in this order (Play Store, 64-bit/Plus, 32-bit)
val targetPackages = listOf("com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32")

android {
    namespace = "se.majid.archlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "se.majid.archlauncher"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String[]", "TARGET_PACKAGES",
            targetPackages.joinToString(prefix = "{", postfix = "}") { "\"$it\"" }
        )
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        // Debug key so the release APK is installable without extra setup.
        getByName("debug")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
