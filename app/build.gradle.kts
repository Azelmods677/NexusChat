plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.Azelmods.App"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.Azelmods.App"
        minSdk = 31  // Android 12 - Compatibilidad con dispositivos más antiguos
        targetSdk = 36  // Android 16 - Compatibilidad absoluta con Redmi 15 5G
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: replace with release signing config before publishing to production
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            // Explicitly disable shrinking for faster debug builds
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    // ── App Bundle language/density/ABI splits ─────────────
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    // ── Per-ABI APK splits (smaller download size) ─────────
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {
    // ── App Startup (lazy/ordered initialization) ──────────
    implementation("androidx.startup:startup-runtime:1.2.0")

    // ── Security (TamperDetection / EncryptedSharedPrefs) ──
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ── Core Android 2026 ──────────────────────────────────
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    // ── Compose BOM 2026 ───────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3-window-size-class")

    // ── Navigation 2025 ────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── Lifecycle 2025 ─────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // ── Hilt 2025 ──────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ── Firebase BOM 2026 ──────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ── Google Auth 2025 (Credential Manager) ──────────────
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ── Coroutines 2025 ────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // ── Coil 3.x (2026) ────────────────────────────────────
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    implementation("io.coil-kt.coil3:coil-video:3.1.0")
    implementation("io.coil-kt.coil3:coil-gif:3.1.0")

    // ── Media3 ExoPlayer 2025 ──────────────────────────────
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.1")

    // ── DataStore 2025 ─────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── WorkManager 2025 ───────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ── Networking 2025 ────────────────────────────────────
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0") // SSE para streaming

    // ── Serialization 2025 ─────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.json:json:20240303")

    // ── Emoji Support ──────────────────────────────────────
    implementation("com.vanniktech:emoji-google:0.18.0")
    implementation("com.vanniktech:emoji:0.18.0")

    // ── Accompanist (SOLO permissions, NO systemuicontroller) ─
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ── WebRTC ─────────────────────────────────────────────
    implementation("io.getstream:stream-webrtc-android:1.1.3")

    // ── Security: Tor Integration ──────────────────────────
    implementation("info.guardianproject.netcipher:netcipher-webkit:2.1.0")

    // ── Security: Payload Generator ────────────────────────
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("org.smali:dexlib2:2.5.2")
    implementation("org.smali:baksmali:2.5.2")

    // ── Cryptography 2025 ──────────────────────────────────
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    
    // ── Signal Protocol (E2EE) ─────────────────────────────
    implementation("org.signal:libsignal-android:0.40.1")

    // ── Zoomable (fullscreen photos) ───────────────────────
    implementation("net.engawapg.lib:zoomable:1.6.2")

    // ── CameraX and ML Kit (QR Scanner) ────────────────────
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // ── libsu 6.0.0 (ROOT) ─────────────────────────────────
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")
    
    // ── Terminal Emulator Libraries ────────────────────────
    // Android Terminal Emulator (Real PTY support)
    implementation("com.github.termux:termux-app:v0.118.1")
    
    // Alternative lightweight terminal
    implementation("com.github.jackpal:Android-Terminal-Emulator:v1.0.70")

    // ── Sora Editor 0.23.5 ─────────────────────────────────
    val soraVersion = "0.23.5"
    implementation("io.github.Rosemoe.sora-editor:editor:$soraVersion")
    implementation("io.github.Rosemoe.sora-editor:language-java:$soraVersion")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:$soraVersion")

    // ── Testing ────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Desugaring ─────────────────────────────────────────
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // ── Performance ────────────────────────────────────────
    implementation("androidx.metrics:metrics-performance:1.0.0-beta01")

    // ── Splash Screen ──────────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")
}
