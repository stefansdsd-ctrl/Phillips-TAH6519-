// Native FIR Convolver scaffold

#ifndef CONVOLVER_H
#define CONVOLVER_H

#ifdef __cplusplus
extern "C" {
#endif

// Simple time-domain convolution (inefficient for long signals). This is a placeholder
// implementation useful for testing and verifying JNI linkage. For production, implement
// FFT-based convolution and optimized SIMD code in native layer.

// Convolve input array (float32) of length n with ir array of length m and write into out (length n+m-1)
void convolve(const float* input, int n, const float* ir, int m, float* out);

#ifdef __cplusplus
}
#endif

#endif // CONVOLVER_H
