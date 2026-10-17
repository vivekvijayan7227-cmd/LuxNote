#pragma once

#include <vector>
#include <cmath>
#include <cstdint>
#include <atomic>
#include <algorithm>
#include <mutex>
#include <chrono>

namespace hires {

constexpr int FFT_SIZE = 1024;
constexpr int NUM_GRAPHIC_BANDS = 10;
constexpr int NUM_PARAMETRIC_BANDS = 12;
constexpr float PI = 3.14159265358979323846f;

enum class FilterType : int {
    Peaking = 0,
    LowShelf = 1,
    HighShelf = 2,
    LowPass = 3,
    HighPass = 4,
    Notch = 5,
    BandPass = 6
};

struct BiquadCoeffs {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a1 = 0.0f, a2 = 0.0f;
};

struct BiquadState {
    float z1_l = 0.0f, z2_l = 0.0f;
    float z1_r = 0.0f, z2_r = 0.0f;
    void reset() {
        z1_l = z2_l = z1_r = z2_r = 0.0f;
    }
};

struct ParametricBandConfig {
    bool enabled = false;
    FilterType type = FilterType::Peaking;
    float frequency = 1000.0f;
    float gainDb = 0.0f;
    float q = 0.707f;
};

// Snapshot of all DSP coefficients and configuration (for thread-safe lock-free double buffering)
struct DspSnapshot {
    float preampLinear = 1.0f;
    float graphicFrequencies[NUM_GRAPHIC_BANDS] = {
        31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
        1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
    };
    float graphicGainsDb[NUM_GRAPHIC_BANDS] = {0.0f};
    BiquadCoeffs graphicCoeffs[NUM_GRAPHIC_BANDS];
    bool graphicActive = false;
    
    ParametricBandConfig paramConfigs[NUM_PARAMETRIC_BANDS];
    BiquadCoeffs paramCoeffs[NUM_PARAMETRIC_BANDS];
    bool paramActive = false;
    
    int vocalMode = 0; // 0=off, 1=vocal focus, 2=instrumental focus
    float vocalAmount = 0.0f;
    float vocalGain = 1.0f;
    float instrumentGain = 1.0f;
    BiquadCoeffs vocalBoostCoeffs;
    BiquadCoeffs vocalShelfCoeffs;
    BiquadCoeffs lpBassCoeffs;
    BiquadCoeffs hpAirCoeffs;
};

class HiResAudioEngine {
public:
    HiResAudioEngine();
    ~HiResAudioEngine() = default;

    void setSampleRate(float sampleRate);
    void setGraphicBandGain(int band, float gainDb);
    void setParametricBand(int band, bool enabled, int type, float freq, float gainDb, float q);
    void setPreampGain(float gainDb);
    void setVocalFocus(int mode, float amount);
    void setStemBalance(float vocalGain, float instrumentGain);
    void setDspEnabled(bool enabled);

    void processInterleavedStereo(float* buffer, int frameCount);
    void computeFftBands(float* outBands, int bandCount);

    bool hasClipped() const {
        auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
        int64_t last = m_lastClipTimeMs.load(std::memory_order_relaxed);
        return (now - last) < 1500;
    }
    void resetClipping() { m_lastClipTimeMs.store(0, std::memory_order_relaxed); }

    float getPeakL() const { return m_peakL.load(std::memory_order_relaxed); }
    float getPeakR() const { return m_peakR.load(std::memory_order_relaxed); }

private:
    void recomputeGraphic(int band, DspSnapshot& snap);
    void recomputeParametric(int band, DspSnapshot& snap);
    void recomputeVocal(DspSnapshot& snap);
    void commitSnapshot();
    static BiquadCoeffs computeCoeffs(FilterType type, float f0, float gainDb, float Q, float sr);
    void performFft(float* real, float* imag, int n);

    float m_sampleRate = 44100.0f;
    std::atomic<bool> m_dspEnabled{false};
    std::atomic<int64_t> m_lastClipTimeMs{0};

    // Staging parameter state (modified by UI thread under mutex)
    std::mutex m_stageMutex;
    DspSnapshot m_staging;
    
    // Formally verified single-producer single-consumer lock-free triple buffer
    // Slot ownership:
    // - m_writeIdx: exclusively owned by producer (UI thread under m_stageMutex)
    // - m_readIdx: exclusively owned by consumer (audio processing thread)
    // - m_sharedState: bits 0-1 = shared buffer index, bit 2 (0x04) = dirty flag
    DspSnapshot m_snapshots[3];
    int m_writeIdx = 0;
    int m_readIdx = 1;
    std::atomic<uint32_t> m_sharedState{2}; // Initial shared slot = 2, dirty = 0

    // Audio thread filter states (only accessed on audio thread)
    BiquadState m_graphicStates[NUM_GRAPHIC_BANDS];
    BiquadState m_paramStates[NUM_PARAMETRIC_BANDS];
    BiquadState m_vocalBoostState;
    BiquadState m_vocalShelfState;
    BiquadState m_lpBassState;
    BiquadState m_hpAirState;

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
