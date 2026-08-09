# Native integration notes

This folder contains a minimal placeholder native convolver implementation (time-domain) and a CMakeLists.txt.

Important:
- The current Gradle build file does not configure externalNativeBuild. To compile and link this native library as part of the app, you must add an externalNativeBuild configuration in app/build.gradle.kts and ensure the Android NDK is installed (via SDK manager).

Steps to enable:
1. Install NDK via SDK Manager in Android Studio (e.g., NDK r25b or later).
2. In app/build.gradle.kts, add:

android {
  defaultConfig {
    externalNativeBuild {
      cmake {
        cppFlags += "-std=c++17"
      }
    }
  }
  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
    }
  }
}

3. Call the JNI bridge functions from Kotlin and implement proper buffer handling.

Notes:
- This C++ convolver is time-domain and will be slow for long impulse responses. For production, implement FFT convolution and SIMD optimizations or use existing optimized libraries (e.g. FFTW via JNI, or KissFFT, or Intel IPP where supported).
