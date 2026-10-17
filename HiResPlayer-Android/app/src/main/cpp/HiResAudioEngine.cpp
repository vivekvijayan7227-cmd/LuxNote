#include "HiResAudioEngine.h"
#include <cstring>

namespace hires {

HiResAudioEngine::HiResAudioEngine() {
    for (int i = 0; i < FFT_SIZE; ++i) {
        m_hannWindow[i] = 0.5f * (1.0f - std::cos(2.0f * PI * static_cast<float>(i) / (FFT_SIZE - 1)));
    }
    setSampleRate(44100.0f);
}

void HiResAudioEngine::setSampleRate(float sampleRate) {
    m_sampleRate = std::max(8000.0f, sampleRate);
    for (int i = 0; i < NUM_EQ_BANDS; ++i) {
        recomputeBiquad(i);
    }
}

void HiResAudioEngine::setGraphicBandGain(int band, float gainDb) {
    if (band >= 0 && band < NUM_EQ_BANDS) {
        m_bandGainsDb[band] = gainDb;
        recomputeBiquad(band);
    }
}

void HiResAudioEngine::setPreampGain(float gainDb) {
    m_preampGainLinear = std::pow(10.0f, gainDb / 20.0f);
}

void HiResAudioEngine::setVocalFocus(int mode, float amount) {
    m_vocalMode = mode;
    m_vocalAmount = std::clamp(amount, 0.0f, 1.0f);
}

void HiResAudioEngine::setDspEnabled(bool enabled) {
    m_dspEnabled.store(enabled, std::memory_order_relaxed);
}

void HiResAudioEngine::recomputeBiquad(int band) {
    float f0 = m_bandFrequencies[band];
    float gainDb = m_bandGainsDb[band];
    auto& c = m_eqCoeffs[band];

    if (std::abs(gainDb) < 0.01f) {
        c.b0 = 1.0f; c.b1 = 0.0f; c.b2 = 0.0f;
        c.a1 = 0.0f; c.a2 = 0.0f;
        return;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float w0 = 2.0f * PI * f0 / m_sampleRate;
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float Q = 1.4142f; // ~1-octave constant Q bandwidth
    float alpha = sin_w0 / (2.0f * Q);

    if (band == 0) {
        // Low Shelf for sub-bass
        float b0 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 + 2.0f * std::sqrt(A) * alpha);
        float b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cos_w0);
        float b2 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 - 2.0f * std::sqrt(A) * alpha);
        float a0 = (A + 1.0f) + (A - 1.0f) * cos_w0 + 2.0f * std::sqrt(A) * alpha;
        float a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cos_w0);
        float a2 = (A + 1.0f) + (A - 1.0f) * cos_w0 - 2.0f * std::sqrt(A) * alpha;

        c.b0 = b0 / a0; c.b1 = b1 / a0; c.b2 = b2 / a0;
        c.a1 = a1 / a0; c.a2 = a2 / a0;
    } else if (band == NUM_EQ_BANDS - 1) {
        // High Shelf for air
        float b0 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 + 2.0f * std::sqrt(A) * alpha);
        float b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cos_w0);
        float b2 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 - 2.0f * std::sqrt(A) * alpha);
        float a0 = (A + 1.0f) - (A - 1.0f) * cos_w0 + 2.0f * std::sqrt(A) * alpha;
        float a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cos_w0);
        float a2 = (A + 1.0f) - (A - 1.0f) * cos_w0 - 2.0f * std::sqrt(A) * alpha;

        c.b0 = b0 / a0; c.b1 = b1 / a0; c.b2 = b2 / a0;
        c.a1 = a1 / a0; c.a2 = a2 / a0;
    } else {
        // Peaking EQ for parametric bands
        float b0 = 1.0f + alpha * A;
        float b1 = -2.0f * cos_w0;
        float b2 = 1.0f - alpha * A;
        float a0 = 1.0f + alpha / A;
        float a1 = -2.0f * cos_w0;
        float a2 = 1.0f - alpha / A;

        c.b0 = b0 / a0; c.b1 = b1 / a0; c.b2 = b2 / a0;
        c.a1 = a1 / a0; c.a2 = a2 / a0;
    }
}

void HiResAudioEngine::processInterleavedStereo(float* buffer, int frameCount) {
    float maxL = 0.0f;
    float maxR = 0.0f;
    bool clipped = false;
    bool dsp = m_dspEnabled.load(std::memory_order_relaxed);

    int sampleCount = frameCount * 2;
    for (int i = 0; i < sampleCount; i += 2) {
        float inL = buffer[i];
        float inR = buffer[i + 1];

        if (dsp) {
            // 1. Preamp Stage
            inL *= m_preampGainLinear;
            inR *= m_preampGainLinear;

            // 2. Direct Form II Transposed 10-Band EQ
            for (int b = 0; b < NUM_EQ_BANDS; ++b) {
                const auto& c = m_eqCoeffs[b];
                auto& s = m_eqStates[b];

                float yL = c.b0 * inL + s.z1_l;
                s.z1_l = c.b1 * inL - c.a1 * yL + s.z2_l;
                s.z2_l = c.b2 * inL - c.a2 * yL;
                inL = yL;

                float yR = c.b0 * inR + s.z1_r;
                s.z1_r = c.b1 * inR - c.a1 * yR + s.z2_r;
                s.z2_r = c.b2 * inR - c.a2 * yR;
                inR = yR;
            }

            // 3. Mid/Side Center Channel Vocal & Instrumental Separation
            if (m_vocalMode != 0 && m_vocalAmount > 0.001f) {
                float mid = 0.5f * (inL + inR);
                float side = 0.5f * (inL - inR);

                if (m_vocalMode == 1) {
                    // Vocal Focus: Boost Center channel formant range
                    mid *= (1.0f + m_vocalAmount * 0.8f);
                } else if (m_vocalMode == 2) {
                    // Instrumental Focus: Center vocal attenuation
                    mid *= std::max(0.05f, 1.0f - m_vocalAmount * 0.95f);
                }

                inL = mid + side;
                inR = mid - side;
            }

            // 4. Studio Soft Limiter (Smooth Cubic Saturation prevents hard digital clipping)
            if (std::abs(inL) > 1.0f || std::abs(inR) > 1.0f) {
                clipped = true;
            }

            if (inL > 0.95f) inL = 0.95f + 0.05f * std::tanh((inL - 0.95f) / 0.05f);
            else if (inL < -0.95f) inL = -0.95f + 0.05f * std::tanh((inL + 0.95f) / 0.05f);

            if (inR > 0.95f) inR = 0.95f + 0.05f * std::tanh((inR - 0.95f) / 0.05f);
            else if (inR < -0.95f) inR = -0.95f + 0.05f * std::tanh((inR + 0.95f) / 0.05f);

            buffer[i] = inL;
            buffer[i + 1] = inR;
        }

        // Tap for FFT Circular Buffer
        float mono = 0.5f * (inL + inR);
        m_fftBuffer[m_fftBufferIndex] = mono;
        m_fftBufferIndex = (m_fftBufferIndex + 1) % FFT_SIZE;

        maxL = std::max(maxL, std::abs(inL));
        maxR = std::max(maxR, std::abs(inR));
    }

    if (clipped) {
        m_hasClipped.store(true, std::memory_order_relaxed);
    }

    m_peakL.store(maxL, std::memory_order_relaxed);
    m_peakR.store(maxR, std::memory_order_relaxed);
}

void HiResAudioEngine::computeFftBands(float* outBands, int bandCount) {
    int start = m_fftBufferIndex;
    for (int i = 0; i < FFT_SIZE; ++i) {
        int idx = (start + i) % FFT_SIZE;
        m_fftReal[i] = m_fftBuffer[idx] * m_hannWindow[i];
        m_fftImag[i] = 0.0f;
    }

    performFft(m_fftReal, m_fftImag, FFT_SIZE);

    int halfFft = FFT_SIZE / 2;
    float minFreq = 20.0f;
    float maxFreq = std::min(20000.0f, m_sampleRate * 0.48f);
    float logMin = std::log(minFreq);
    float logMax = std::log(maxFreq);
    float binRes = m_sampleRate / static_cast<float>(FFT_SIZE);

    for (int b = 0; b < bandCount; ++b) {
        float fLow = std::exp(logMin + (logMax - logMin) * (static_cast<float>(b) / bandCount));
        float fHigh = std::exp(logMin + (logMax - logMin) * (static_cast<float>(b + 1) / bandCount));

        int binStart = std::clamp(static_cast<int>(fLow / binRes), 0, halfFft - 1);
        int binEnd = std::clamp(static_cast<int>(fHigh / binRes), binStart, halfFft - 1);

        float maxMag = 0.0f;
        for (int bin = binStart; bin <= binEnd; ++bin) {
            float r = m_fftReal[bin];
            float im = m_fftImag[bin];
            float mag = std::sqrt(r * r + im * im) / (FFT_SIZE / 4.0f);
            maxMag = std::max(maxMag, mag);
        }

        float db = (maxMag > 1e-5f) ? 20.0f * std::log10(maxMag) : -60.0f;
        float norm = std::clamp((db + 60.0f) / 60.0f, 0.0f, 1.0f);

        // Exponential ballistic attack/decay
        if (norm > m_smoothedBands[b]) {
            m_smoothedBands[b] = norm;
        } else {
            m_smoothedBands[b] = m_smoothedBands[b] * 0.82f + norm * 0.18f;
        }

        outBands[b] = m_smoothedBands[b];
    }
}

void HiResAudioEngine::performFft(float* re, float* im, int n) {
    int j = 0;
    for (int i = 0; i < n - 1; ++i) {
        if (i < j) {
            std::swap(re[i], re[j]);
            std::swap(im[i], im[j]);
        }
        int k = n / 2;
        while (k <= j) {
            j -= k;
            k /= 2;
        }
        j += k;
    }

    for (int len = 2; len <= n; len <<= 1) {
        int halfLen = len >> 1;
        float angle = -2.0f * PI / static_cast<float>(len);
        float wStepR = std::cos(angle);
        float wStepI = std::sin(angle);

        for (int i = 0; i < n; i += len) {
            float wR = 1.0f;
            float wI = 0.0f;
            for (int k = 0; k < halfLen; ++k) {
                float uR = re[i + k];
                float uI = im[i + k];
                float vR = re[i + k + halfLen] * wR - im[i + k + halfLen] * wI;
                float vI = re[i + k + halfLen] * wI + im[i + k + halfLen] * wR;

                re[i + k] = uR + vR;
                im[i + k] = uI + vI;
                re[i + k + halfLen] = uR - vR;
                im[i + k + halfLen] = uI - vI;

                float nextWR = wR * wStepR - wI * wStepI;
                float nextWI = wR * wStepI + wI * wStepR;
                wR = nextWR;
                wI = nextWI;
            }
        }
    }
}

} // namespace hires
