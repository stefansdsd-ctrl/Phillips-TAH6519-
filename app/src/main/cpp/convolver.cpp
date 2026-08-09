#include "convolver.h"
#include <string.h>

void convolve(const float* input, int n, const float* ir, int m, float* out) {
    int outLen = n + m - 1;
    // zero output
    for (int i = 0; i < outLen; ++i) out[i] = 0.0f;

    for (int i = 0; i < n; ++i) {
        for (int j = 0; j < m; ++j) {
            out[i + j] += input[i] * ir[j];
        }
    }
}
