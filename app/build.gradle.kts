plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.devclip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devclip.app"
        // 24, not something newer. The overlay and clipboard code branches on
        // API level in several places and those branches are real; raising the
        // floor would quietly delete fallbacks that have not been tested
        // without.
        minSdk = 24
        targetSdk = 35
        // Bumped per release, by hand. versionCode is what Android compares
        // when deciding whether an install is an update; versionName is what
        // the APK is named after and what Settings shows, so the file on the
        // phone and the file on the release page can be told apart without
        // opening either.
        versionCode = 6
        versionName = "1.3.1"
    }

    buildTypes {
        release {
            /*
             * Without this, AGP emits an UNSIGNED release APK and Android
             * refuses to install it at all — "package appears to be invalid",
             * with nothing to suggest the signature is what is missing.
             *
             * The Expo-generated build file carried `signingConfigs.debug`
             * here and it was not obvious why until it was gone. It is the
             * fallback that makes a build with no keystore installable: the
             * key is throwaway and regenerated per CI run, so such a build
             * will not install over a previous one, but it does install.
             *
             * `-Pandroid.injected.signing.*` overrides this entirely, so a
             * build with the real keystore in secrets is signed with that and
             * this line never applies.
             */
            signingConfig = signingConfigs.getByName("debug")

            // Off for now. The accessibility service, the overlay service and
            // the boot receiver are all reached by name from the system rather
            // than from code, and turning shrinking on without keep rules
            // first is how those stop being found.
            isMinifyEnabled = false
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
}

dependencies {
    // Pulled in by the floating windows, which predate Compose and stay in
    // views: NotificationCompat, NotificationManagerCompat and
    // RoundedBitmapDrawableFactory.
    implementation("androidx.core:core-ktx:1.13.1")

    // Used directly by DevClipEvents, so declared rather than inherited from
    // lifecycle's transitives.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // The BOM decides every Compose artifact's version, so they cannot drift
    // apart from each other.
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
