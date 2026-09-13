plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace  = "dev.kaleve.nonius"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.kaleve.nonius"
        // One supported release, the current one. Every API in here is stable on
        // Android 16, so there are no version guards anywhere in the source.
        minSdk        = 36
        targetSdk     = 36
        versionCode   = 2
        versionName   = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // F-Droid rebuilds the APK and compares it byte for byte. The dependency
    // blob AGP adds is signed with a Google key and differs on every machine.
    dependenciesInfo {
        includeInApk    = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    // Used directly by sensor/ and data/, so it is declared rather than taken
    // on loan from lifecycle. This is the version the graph already resolved,
    // so declaring it changes nothing in the APK except the honesty.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Everything under core/ is plain Kotlin: no Android import, no device, no
    // Robolectric. That is where the arithmetic of every instrument lives.
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}
