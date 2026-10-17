package com.audioplayer.hires.audio

/** Conservative results for a USB Audio Class inspection. */
enum class UsbAudioClassVersion { UAC1, UAC2, UNKNOWN }
enum class UsbClockingMode { SYNCHRONOUS, ADAPTIVE, ASYNCHRONOUS, UNKNOWN }

/**
 * Explicit evidence parsed from descriptors / active Android APIs.
 * Do not construct `UAC2` or a clocking mode merely because an AudioStreaming interface exists.
 */
data class UsbAudioEvidence(
    val classVersion: UsbAudioClassVersion = UsbAudioClassVersion.UNKNOWN,
    val clockingMode: UsbClockingMode = UsbClockingMode.UNKNOWN,
    val hasPlaybackOutEndpoint: Boolean = false,
    val supportedSampleRates: List<Int> = emptyList(),
    val activeAndroidRouteName: String? = null,
    val androidBitPerfectApiAvailable: Boolean = false,
    val bitPerfectModeConfirmedByFramework: Boolean = false
)

data class UsbCapabilityReport(
    val classVersion: UsbAudioClassVersion,
    val clockingMode: UsbClockingMode,
    val hasPlaybackOutEndpoint: Boolean,
    val sampleRatesHz: List<Int>,
    val activeRouteName: String?,
    val bitPerfectStatus: CapabilityStatus,
    val explanation: String
)

/** Pure logic only; hardware descriptor parsing and API queries belong in the Android adapter. */
object UsbCapabilityClassifier {
    fun classify(evidence: UsbAudioEvidence): UsbCapabilityReport {
        val bitPerfectStatus = when {
            evidence.bitPerfectModeConfirmedByFramework -> CapabilityStatus.SUPPORTED
            evidence.androidBitPerfectApiAvailable -> CapabilityStatus.UNKNOWN
            else -> CapabilityStatus.UNKNOWN
        }
        val explanation = when (bitPerfectStatus) {
            CapabilityStatus.SUPPORTED ->
                "The framework reports bit-perfect mixer mode for this route; source format and DSP state must still be checked."
            CapabilityStatus.UNSUPPORTED ->
                "The active route does not support the required output mode."
            CapabilityStatus.UNKNOWN ->
                "Bit-perfect playback has not been confirmed by the active Android framework route."
        }
        return UsbCapabilityReport(
            classVersion = evidence.classVersion,
            clockingMode = evidence.clockingMode,
            hasPlaybackOutEndpoint = evidence.hasPlaybackOutEndpoint,
            sampleRatesHz = evidence.supportedSampleRates.distinct().filter { it > 0 }.sorted(),
            activeRouteName = evidence.activeAndroidRouteName,
            bitPerfectStatus = bitPerfectStatus,
            explanation = explanation
        )
    }
}
