package com.example.headphonecompanion.audio

/**
 * Kotlin wrapper that calls the Java JNI bridge. Returns null if native library is unavailable.
 */
object NativeConvolver {
    fun convolve(input: FloatArray, ir: FloatArray): FloatArray? {
        return try {
            com.example.headphonecompanion.audio.NativeConvolverBridge.convolve(input, ir)
        } catch (t: Throwable) {
            null
        }
    }
}
