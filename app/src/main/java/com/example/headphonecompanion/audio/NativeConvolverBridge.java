package com.example.headphonecompanion.audio;

public class NativeConvolverBridge {
    static {
        try {
            System.loadLibrary("native_convolver");
        } catch (Throwable t) {
            // library may not be available during early testing
        }
    }

    // JNI bridge to native FFT convolver
    public static native float[] convolve(float[] input, float[] ir);
}
