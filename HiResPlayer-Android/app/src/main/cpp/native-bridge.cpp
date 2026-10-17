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
    if (!engine || !outBands || bandCount <= 0) return;

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
    std::string ver = "HiRes-Native-C++20-v2.6-BitPerfect-DSP (libhires_audio.so)";
    return env->NewStringUTF(ver.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_audioplayer_hires_audio_NativeAudioEngine_isNativeAvailable(JNIEnv* /*env*/, jobject /*thiz*/) {
    return JNI_TRUE;
}

} // extern "C"
