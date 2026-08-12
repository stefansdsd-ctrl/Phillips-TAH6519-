plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.headphonecompanion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.headphonecompanion"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        // Configure externalNativeBuild (CMake) to allow building the native convolver when NDK is installed.
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O3"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.android.exoplayer:exoplayer:2.19.0")
}
