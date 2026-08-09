#include <jni.h>
#include <vector>
#include <complex>
#include <cmath>
#include <algorithm>

using namespace std;

static void fft(vector<complex<double>>& a, bool invert) {
    int n = (int)a.size();
    for (int i = 1, j = 0; i < n; ++i) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1)
            j ^= bit;
        j ^= bit;
        if (i < j)
            swap(a[i], a[j]);
    }
    for (int len = 2; len <= n; len <<= 1) {
        double ang = 2 * M_PI / len * (invert ? -1 : 1);
        complex<double> wlen(cos(ang), sin(ang));
        for (int i = 0; i < n; i += len) {
            complex<double> w(1);
            for (int j = 0; j < len / 2; ++j) {
                complex<double> u = a[i + j];
                complex<double> v = a[i + j + len / 2] * w;
                a[i + j] = u + v;
                a[i + j + len / 2] = u - v;
                w *= wlen;
            }
        }
    }
    if (invert) {
        for (int i = 0; i < n; ++i)
            a[i] /= n;
    }
}

static vector<float> convolve_fft(const vector<float>& a, const vector<float>& b) {
    if (a.empty() || b.empty()) return vector<float>();
    int n = 1;
    while (n < (int)(a.size() + b.size() - 1)) n <<= 1;

    vector<complex<double>> fa(n);
    for (size_t i = 0; i < a.size(); ++i) fa[i] = a[i];
    for (size_t i = a.size(); i < (size_t)n; ++i) fa[i] = 0.0;

    vector<complex<double>> fb(n);
    for (size_t i = 0; i < b.size(); ++i) fb[i] = b[i];
    for (size_t i = b.size(); i < (size_t)n; ++i) fb[i] = 0.0;

    fft(fa, false);
    fft(fb, false);
    for (int i = 0; i < n; ++i) fa[i] *= fb[i];
    fft(fa, true);

    int outLen = (int)(a.size() + b.size() - 1);
    vector<float> out(outLen);
    for (int i = 0; i < outLen; ++i) out[i] = (float)(fa[i].real());
    return out;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_headphonecompanion_audio_NativeConvolver_convolve(JNIEnv* env, jclass clazz, jfloatArray inputArray, jfloatArray irArray) {
    if (inputArray == nullptr || irArray == nullptr) return nullptr;

    jsize inLen = env->GetArrayLength(inputArray);
    jsize irLen = env->GetArrayLength(irArray);
    if (inLen == 0 || irLen == 0) return nullptr;

    vector<float> input(inLen);
    vector<float> ir(irLen);

    env->GetFloatArrayRegion(inputArray, 0, inLen, input.data());
    env->GetFloatArrayRegion(irArray, 0, irLen, ir.data());

    vector<float> out = convolve_fft(input, ir);

    jfloatArray result = env->NewFloatArray((jsize)out.size());
    if (result == nullptr) return nullptr;
    env->SetFloatArrayRegion(result, 0, (jsize)out.size(), out.data());
    return result;
}
