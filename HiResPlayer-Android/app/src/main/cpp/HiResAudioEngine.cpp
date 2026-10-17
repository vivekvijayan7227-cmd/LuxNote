#include "HiResAudioEngine.h"
#include <cstring>

namespace hires {

HiResAudioEngine::HiResAudioEngine() {
    for (int i = 0; i < FFT_SIZE; ++i) {
        m_hannWindow[i] = 0.5f * (1.0f - std::cos(2.0f * PI * static_cast<float>(i) / (FFT_SIZE - 1)));
    }
    
    // Initialize default parametric frequencies
    for (int i = 0; i < NUM_PARAMETRIC_BANDS; ++i) {
        m_staging.paramConfigs[i].frequency = std::clamp(100.0f * std::pow(2.0f, static_cast<float>(i)), 20.0f, 20000.0f);
        m_staging.paramConfigs[i].gainDb = 0.0f;
        m_staging.paramConfigs[i].q = 0.707f;
        m_staging.paramConfigs[i].type = FilterType::Peaking;
        m_staging.paramConfigs[i].enabled = false;
    }

    setSampleRate(44100.0f);
}

void HiResAudioEngine::setSampleRate(float sampleRate) {
    std::lock_guard<std::mutex> lock(m_stageMutex);
    m_sampleRate = std::max(8000.0f, sampleRate);
    for (int i = 0; i < NUM_GRAPHIC_BANDS; ++i) {
        recomputeGraphic(i, m_staging);
    }
    for (int i = 0; i < NUM_PARAMETRIC_BANDS; ++i) {
        recomputeParametric(i, m_staging);
    }
    recomputeVocal(m_staging);
    commitSnapshot();
}

void HiResAudioEngine::setGraphicBandGain(int band, float gainDb) {
    if (band < 0 || band >= NUM_GRAPHIC_BANDS) return;
    std::lock_guard<std::mutex> lock(m_stageMutex);
    m_staging.graphicGainsDb[band] = gainDb;
    recomputeGraphic(band, m_staging);
    commitSnapshot();
}

void HiResAudioEngine::setParametricBand(int band, bool enabled, int type, float freq, float gainDb, float q) {
    if (band < 0 || band >= NUM_PARAMETRIC_BANDS) return;
    std::lock_guard<std::mutex> lock(m_stageMutex);
    auto& cfg = m_staging.paramConfigs[band];
    cfg.enabled = enabled;
    cfg.type = static_cast<FilterType>(std::clamp(type, 0, 6));
    cfg.frequency = std::clamp(freq, 20.0f, m_sampleRate * 0.49f);
    cfg.gainDb = std::clamp(gainDb, -18.0f, 18.0f);
    cfg.q = std::clamp(q, 0.1f, 10.0f);
    recomputeParametric(band, m_staging);
    commitSnapshot();
}

void HiResAudioEngine::setPreampGain(float gainDb) {
    std::lock_guard<std::mutex> lock(m_stageMutex);
    m_staging.preampLinear = std::pow(10.0f, gainDb / 20.0f);
    commitSnapshot();
}

void HiResAudioEngine::setVocalFocus(int mode, float amount) {
    std::lock_guard<std::mutex> lock(m_stageMutex);
    m_staging.vocalMode = mode;
    m_staging.vocalAmount = std::clamp(amount, 0.0f, 1.0f);
    recomputeVocal(m_staging);
    commitSnapshot();
}

void HiResAudioEngine::setDspEnabled(bool enabled) {
    m_dspEnabled.store(enabled, std::memory_order_relaxed);
}

BiquadCoeffs HiResAudioEngine::computeCoeffs(FilterType type, float f0, float gainDb, float Q, float sr) {
    BiquadCoeffs c;
    if (f0 <= 0.0f || sr <= 0.0f) return c;

    float w0 = 2.0f * PI * f0 / sr;
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float alpha = sin_w0 / (2.0f * std::max(0.01f, Q));
    float A = std::pow(10.0f, gainDb / 40.0f);

    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a0 = 1.0f, a1 = 0.0f, a2 = 0.0f;

    switch (type) {
        case FilterType::Peaking: {
            if (std::abs(gainDb) < 0.01f) return c;
            b0 = 1.0f + alpha * A;
            b1 = -2.0f * cos_w0;
            b2 = 1.0f - alpha * A;
            a0 = 1.0f + alpha / A;
            a1 = -2.0f * cos_w0;
            a2 = 1.0f - alpha / A;
            break;
        }
        case FilterType::LowShelf: {
            if (std::abs(gainDb) < 0.01f) return c;
            float sqrtA2Alpha = 2.0f * std::sqrt(A) * alpha;
            b0 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 + sqrtA2Alpha);
            b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cos_w0);
            b2 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 - sqrtA2Alpha);
            a0 = (A + 1.0f) + (A - 1.0f) * cos_w0 + sqrtA2Alpha;
            a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cos_w0);
            a2 = (A + 1.0f) + (A - 1.0f) * cos_w0 - sqrtA2Alpha;
            break;
        }
        case FilterType::HighShelf: {
            if (std::abs(gainDb) < 0.01f) return c;
            float sqrtA2Alpha = 2.0f * std::sqrt(A) * alpha;
            b0 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 + sqrtA2Alpha);
            b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cos_w0);
            b2 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 - sqrtA2Alpha);
            a0 = (A + 1.0f) - (A - 1.0f) * cos_w0 + sqrtA2Alpha;
            a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cos_w0);
            a2 = (A + 1.0f) - (A - 1.0f) * cos_w0 - sqrtA2Alpha;
            break;
        }
        case FilterType::LowPass: {
            b0 = (1.0f - cos_w0) * 0.5f;
            b1 = 1.0f - cos_w0;
            b2 = (1.0f - cos_w0) * 0.5f;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cos_w0;
            a2 = 1.0f - alpha;
            break;
        }
        case FilterType::HighPass: {
            b0 = (1.0f + cos_w0) * 0.5f;
            b1 = -(1.0f + cos_w0);
            b2 = (1.0f + cos_w0) * 0.5f;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cos_w0;
            a2 = 1.0f - alpha;
            break;
        }
        case FilterType::Notch: {
            b0 = 1.0f;
            b1 = -2.0f * cos_w0;
            b2 = 1.0f;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cos_w0;
            a2 = 1.0f - alpha;
            break;
        }
        case FilterType::BandPass: {
            b0 = alpha;
            b1 = 0.0f;
            b2 = -alpha;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cos_w0;
            a2 = 1.0f - alpha;
            break;
        }
    }

    if (std::abs(a0) > 1e-7f) {
        c.b0 = b0 / a0;
        c.b1 = b1 / a0;
        c.b2 = b2 / a0;
        c.a1 = a1 / a0;
        c.a2 = a2 / a0;
    }
    return c;
}

void HiResAudioEngine::recomputeGraphic(int band, DspSnapshot& snap) {
    float f0 = snap.graphicFrequencies[band];
    float gainDb = snap.graphicGainsDb[band];
    FilterType type = (band == 0) ? FilterType::LowShelf :
                      (band == NUM_GRAPHIC_BANDS - 1) ? FilterType::HighShelf :
                      FilterType::Peaking;
    snap.graphicCoeffs[band] = computeCoeffs(type, f0, gainDb, 1.4142f, m_sampleRate);

    bool active = false;
    for (int i = 0; i < NUM_GRAPHIC_BANDS; ++i) {
        if (std::abs(snap.graphicGainsDb[i]) > 0.01f) {
            active = true;
            break;
        }
    }
    snap.graphicActive = active;
}

void HiResAudioEngine::recomputeParametric(int band, DspSnapshot& snap) {
    const auto& cfg = snap.paramConfigs[band];
    if (cfg.enabled) {
        snap.paramCoeffs[band] = computeCoeffs(cfg.type, cfg.frequency, cfg.gainDb, cfg.q, m_sampleRate);
    } else {
        snap.paramCoeffs[band] = BiquadCoeffs();
    }

    bool active = false;
    for (int i = 0; i < NUM_PARAMETRIC_BANDS; ++i) {
        if (snap.paramConfigs[i].enabled) {
            active = true;
            break;
        }
    }
    snap.paramActive = active;
}

void HiResAudioEngine::recomputeVocal(DspSnapshot& snap) {
    if (snap.vocalMode == 1) { // Vocal Focus
        snap.vocalBoostCoeffs = computeCoeffs(FilterType::Peaking, 2600.0f, snap.vocalAmount * 6.0f, 1.2f, m_sampleRate);
        snap.vocalShelfCoeffs = computeCoeffs(FilterType::HighShelf, 6000.0f, snap.vocalAmount * 3.0f, 0.707f, m_sampleRate);
    } else if (snap.vocalMode == 2) { // Instrumental Focus
        snap.lpBassCoeffs = computeCoeffs(FilterType::LowPass, 140.0f, 0.0f, 0.707f, m_sampleRate);
        snap.hpAirCoeffs = computeCoeffs(FilterType::HighPass, 6000.0f, 0.0f, 0.707f, m_sampleRate);
    }
}

void HiResAudioEngine::commitSnapshot() {
    int cur = m_activeSnapshotIndex.load(std::memory_order_relaxed);
    int next = 1 - cur;
    m_snapshots[next] = m_staging;
    m_activeSnapshotIndex.store(next, std::memory_order_release);
}

void HiResAudioEngine::processInterleavedStereo(float* buffer, int frameCount) {
    if (!buffer || frameCount <= 0) return;

    float maxL = 0.0f;
    float maxR = 0.0f;
    bool clipped = false;
    bool dsp = m_dspEnabled.load(std::memory_order_relaxed);

    // Lock-free atomic acquisition of active DSP parameter snapshot
    int snapIdx = m_activeSnapshotIndex.load(std::memory_order_acquire);
    const auto& snap = m_snapshots[snapIdx];

    int sampleCount = frameCount * 2;
    for (int i = 0; i < sampleCount; i += 2) {
        float inL = buffer[i];
        float inR = buffer[i + 1];

        if (dsp) {
            // 1. Preamp Stage
            inL *= snap.preampLinear;
            inR *= snap.preampLinear;

            // 2. Direct Form II Transposed 10-Band Graphic EQ
            if (snap.graphicActive) {
                for (int b = 0; b < NUM_GRAPHIC_BANDS; ++b) {
                    const auto& c = snap.graphicCoeffs[b];
                    auto& s = m_graphicStates[b];

                    float yL = c.b0 * inL + s.z1_l;
                    s.z1_l = c.b1 * inL - c.a1 * yL + s.z2_l;
                    s.z2_l = c.b2 * inL - c.a2 * yL;
                    inL = yL;

                    float yR = c.b0 * inR + s.z1_r;
                    s.z1_r = c.b1 * inR - c.a1 * yR + s.z2_r;
                    s.z2_r = c.b2 * inR - c.a2 * yR;
                    inR = yR;
                }
            }

            // 3. 12-Band Parametric EQ Cascade (Full Parity with Kotlin Pipeline)
            if (snap.paramActive) {
                for (int b = 0; b < NUM_PARAMETRIC_BANDS; ++b) {
                    if (!snap.paramConfigs[b].enabled) continue;
                    const auto& c = snap.paramCoeffs[b];
                    auto& s = m_paramStates[b];

                    float yL = c.b0 * inL + s.z1_l;
                    s.z1_l = c.b1 * inL - c.a1 * yL + s.z2_l;
                    s.z2_l = c.b2 * inL - c.a2 * yL;
                    inL = yL;

                    float yR = c.b0 * inR + s.z1_r;
                    s.z1_r = c.b1 * inR - c.a1 * yR + s.z2_r;
                    s.z2_r = c.b2 * inR - c.a2 * yR;
                    inR = yR;
                }
            }

            // 4. Mid/Side Center Channel Vocal & Instrumental Separation
            if (snap.vocalMode != 0 && snap.vocalAmount > 0.001f) {
                float mid = 0.5f * (inL + inR);
                float side = 0.5f * (inL - inR);

                if (snap.vocalMode == 1) { // Vocal Focus
                    const auto& cb = snap.vocalBoostCoeffs;
                    auto& sb = m_vocalBoostState;
                    float midBoost = cb.b0 * mid + sb.z1_l;
                    sb.z1_l = cb.b1 * mid - cb.a1 * midBoost + sb.z2_l;
                    sb.z2_l = cb.b2 * mid - cb.a2 * midBoost;

                    const auto& cs = snap.vocalShelfCoeffs;
                    auto& ss = m_vocalShelfState;
                    float midShelf = cs.b0 * midBoost + ss.z1_l;
                    ss.z1_l = cs.b1 * midBoost - cs.a1 * midShelf + ss.z2_l;
                    ss.z2_l = cs.b2 * midBoost - cs.a2 * midShelf;

                    float midOut = midShelf * (1.0f + 0.35f * snap.vocalAmount);
                    float sideAtten = std::max(0.20f, 1.0f - (snap.vocalAmount * 0.65f));
                    side *= sideAtten;

                    inL = midOut + side;
                    inR = midOut - side;
                } else if (snap.vocalMode == 2) { // Instrumental Focus
                    const auto& clp = snap.lpBassCoeffs;
                    auto& slp = m_lpBassState;
                    float subBass = clp.b0 * mid + slp.z1_l;
                    slp.z1_l = clp.b1 * mid - clp.a1 * subBass + slp.z2_l;
                    slp.z2_l = clp.b2 * mid - clp.a2 * subBass;

                    const auto& chp = snap.hpAirCoeffs;
                    auto& shp = m_hpAirState;
                    float air = chp.b0 * mid + shp.z1_l;
                    shp.z1_l = chp.b1 * mid - chp.a1 * air + shp.z2_l;
                    shp.z2_l = chp.b2 * mid - chp.a2 * air;

                    float vocalBand = mid - subBass - air;
                    float vocalAtten = std::max(0.063f, 1.0f - (snap.vocalAmount * 0.937f));
                    float midOut = subBass + air + (vocalBand * vocalAtten);

                    inL = midOut + side;
                    inR = midOut - side;
                }
            }

            // 5. Studio Soft Limiter (Smooth Cubic Saturation prevents hard digital clipping)
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
        auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
        m_lastClipTimeMs.store(now, std::memory_order_relaxed);
    }

    m_peakL.store(maxL, std::memory_order_relaxed);
    m_peakR.store(maxR, std::memory_order_relaxed);
}

void HiResAudioEngine::computeFftBands(float* outBands, int bandCount) {
    if (!outBands || bandCount <= 0 || bandCount > 64) return;

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
            float mag = std::sqrt(m_fftReal[bin] * m_fftReal[bin] + m_fftImag[bin] * m_fftImag[bin]) / static_cast<float>(FFT_SIZE / 2);
            maxMag = std::max(maxMag, mag);
        }

        // Apply logarithmic perceptual scaling (dB)
        float db = 20.0f * std::log10(std::max(1e-4f, maxMag));
        float norm = std::clamp((db + 60.0f) / 60.0f, 0.05f, 1.0f);

        // Exponential smoothing filter
        m_smoothedBands[b] = m_smoothedBands[b] * 0.70f + norm * 0.30f;
        outBands[b] = m_smoothedBands[b];
    }
}

void HiResAudioEngine::performFft(float* real, float* imag, int n) {
    int j = 0;
    for (int i = 0; i < n - 1; ++i) {
        if (i < j) {
            std::swap(real[i], real[j]);
            std::swap(imag[i], imag[j]);
        }
        int k = n >> 1;
        while (k <= j) {
            j -= k;
            k >>= 1;
        }
        j += k;
    }

    for (int len = 2; len <= n; len <<= 1) {
        float angle = -2.0f * PI / static_cast<float>(len);
        float wlen_real = std::cos(angle);
        float wlen_imag = std::sin(angle);
        int halfLen = len >> 1;

        for (int i = 0; i < n; i += len) {
            float w_real = 1.0f;
            float w_imag = 0.0f;

            for (int k = 0; k < halfLen; ++k) {
                float u_real = real[i + k];
                float u_imag = imag[i + k];
                float v_real = real[i + k + halfLen] * w_real - imag[i + k + halfLen] * w_imag;
                float v_imag = real[i + k + halfLen] * w_imag + imag[i + k + halfLen] * w_real;

                real[i + k] = u_real + v_real;
                imag[i + k] = u_imag + v_imag;
                real[i + k + halfLen] = u_real - v_real;
                imag[i + k + halfLen] = u_imag - v_imag;

                float next_w_real = w_real * wlen_real - w_imag * wlen_imag;
                w_imag = w_real * wlen_imag + w_imag * wlen_real;
                w_real = next_w_real;
            }
        }
    }
}

} // namespace hires
