#include <jni.h>
#include <string>
#include <android/log.h>
#include "HiResAudioEngine.h"

#define TAG "HiResAudioEngineNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static inline hires::HiResAudioEngine* getEngine(jlong handle) {
    return reinterpret_cast<hires::HiResAudioEngine*>(handle);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_createEngine(JNIEnv* /*env*/, jobject /*thiz*/) {
    auto* engine = new hires::HiResAudioEngine();
    LOGI("Native HiResAudioEngine instance allocated: %p", engine);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_destroyEngine(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    auto* engine = getEngine(handle);
    if (engine) {
        LOGI("Destroying Native HiResAudioEngine instance: %p", engine);
        delete engine;
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setSampleRate(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jfloat sampleRate) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setSampleRate(sampleRate);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setGraphicBandGain(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint band, jfloat gainDb) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setGraphicBandGain(band, gainDb);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setParametricBand(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint band, jboolean enabled, jint type, jfloat freq, jfloat gainDb, jfloat q) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setParametricBand(band, enabled, type, freq, gainDb, q);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setPreampGain(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jfloat gainDb) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setPreampGain(gainDb);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setVocalFocus(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint mode, jfloat amount) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setVocalFocus(mode, amount);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setStemBalance(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jfloat vocalGain, jfloat instrumentGain) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setStemBalance(vocalGain, instrumentGain);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_setDspEnabled(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jboolean enabled) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->setDspEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_processInterleavedStereo(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jfloatArray buffer, jint frameCount) {
    auto* engine = getEngine(handle);
    if (!engine || !buffer || frameCount <= 0) return;

    // Strict overflow-safe bounds check: array must contain at least frameCount * 2 samples
    jsize arrayLen = env->GetArrayLength(buffer);
    if (frameCount > arrayLen / 2) {
        LOGE("processInterleavedStereo buffer underflow: len=%d, required=%d frames", arrayLen, frameCount);
        return;
    }

    jfloat* rawBuf = env->GetFloatArrayElements(buffer, nullptr);
    if (rawBuf) {
        engine->processInterleavedStereo(rawBuf, frameCount);
        env->ReleaseFloatArrayElements(buffer, rawBuf, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_computeFftBands(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jfloatArray outBands, jint bandCount) {
    auto* engine = getEngine(handle);
    if (!engine || !outBands || bandCount <= 0 || bandCount > 64) return;

    // Strict bounds check: destination array must have at least bandCount elements
    jsize arrayLen = env->GetArrayLength(outBands);
    if (arrayLen < bandCount) {
        LOGE("computeFftBands array underflow: len=%d, required=%d", arrayLen, bandCount);
        return;
    }

    jfloat* rawBands = env->GetFloatArrayElements(outBands, nullptr);
    if (rawBands) {
        engine->computeFftBands(rawBands, bandCount);
        env->ReleaseFloatArrayElements(outBands, rawBands, 0);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_hasClipped(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    auto* engine = getEngine(handle);
    return engine ? engine->hasClipped() : false;
}

JNIEXPORT void JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_resetClipping(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    auto* engine = getEngine(handle);
    if (engine) {
        engine->resetClipping();
    }
}

JNIEXPORT jstring JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_getEngineVersion(JNIEnv* env, jobject /*thiz*/) {
    std::string v = "LuxNote Native C++20 SIMD Studio Engine v3.0 (Thread-Safe Snapshot, 10-Graphic + 12-Parametric Biquad Cascade, M/S Vocal Studio, 1024-FFT)";
    return env->NewStringUTF(v.c_str());
}

} // extern "C"
