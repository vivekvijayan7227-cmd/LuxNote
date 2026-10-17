#pragma once

#include <vector>
#include <cmath>
#include <cstdint>
#include <atomic>
#include <algorithm>

namespace hires {

constexpr int FFT_SIZE = 1024;
constexpr int NUM_EQ_BANDS = 10;
constexpr float PI = 3.14159265358979323846f;

struct BiquadCoeffs {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a1 = 0.0f, a2 = 0.0f;
};

struct BiquadState {
    float z1_l = 0.0f, z2_l = 0.0f;
    float z1_r = 0.0f, z2_r = 0.0f;
};

class HiResAudioEngine {
public:
    HiResAudioEngine();
    ~HiResAudioEngine() = default;

    void setSampleRate(float sampleRate);
    void setGraphicBandGain(int band, float gainDb);
    void setPreampGain(float gainDb);
    void setVocalFocus(int mode, float amount);
    void setDspEnabled(bool enabled);

    void processInterleavedStereo(float* buffer, int frameCount);
    void computeFftBands(float* outBands, int bandCount);

    bool hasClipped() const { return m_hasClipped.load(std::memory_order_relaxed); }
    void resetClipping() { m_hasClipped.store(false, std::memory_order_relaxed); }

    float getPeakL() const { return m_peakL.load(std::memory_order_relaxed); }
    float getPeakR() const { return m_peakR.load(std::memory_order_relaxed); }

private:
    void recomputeBiquad(int band);
    void performFft(float* real, float* imag, int n);

    float m_sampleRate = 44100.0f;
    std::atomic<bool> m_dspEnabled{false};
    std::atomic<bool> m_hasClipped{false};

    // Preamp
    float m_preampGainLinear = 1.0f;

    // 10-Band Graphic EQ
    float m_bandFrequencies[NUM_EQ_BANDS] = {
        31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
        1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
    };
    float m_bandGainsDb[NUM_EQ_BANDS] = {0.0f};
    BiquadCoeffs m_eqCoeffs[NUM_EQ_BANDS];
    BiquadState m_eqStates[NUM_EQ_BANDS];

    // Vocal Studio Mid/Side DSP
    int m_vocalMode = 0; // 0=off, 1=vocal focus, 2=instrumental focus
    float m_vocalAmount = 0.0f;
    BiquadCoeffs m_vocalShelfCoeffs;
    BiquadState m_vocalShelfState;

    // Spectrum Analyzer
    float m_fftBuffer[FFT_SIZE] = {0.0f};
    int m_fftBufferIndex = 0;
    float m_hannWindow[FFT_SIZE] = {0.0f};
    float m_fftReal[FFT_SIZE] = {0.0f};
    float m_fftImag[FFT_SIZE] = {0.0f};
    float m_smoothedBands[64] = {0.0f};

    std::atomic<float> m_peakL{0.0f};
    std::atomic<float> m_peakR{0.0f};
};

} // namespace hires
