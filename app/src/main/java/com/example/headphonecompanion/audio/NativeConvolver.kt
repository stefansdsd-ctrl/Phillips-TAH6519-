package com.example.headphonecompanion.audio

object NativeConvolver {
    init {
        // library not yet built into the Android build; this is a placeholder for the JNI library
        try {
            System.loadLibrary("native_convolver")
        } catch (e: Throwable) {
            // library not available yet
        }
    }

    // Placeholder JNI wrapper. Implement JNI functions in native layer when integrating.
    external fun convolve(input: FloatArray, ir: FloatArray): FloatArray
}
