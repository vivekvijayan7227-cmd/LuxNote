package com.audioplayer.hires.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity and contract tests for CenterChannelProcessor and UsbCapabilityClassifier.
 * Enforces consistency between Kotlin fallback and native C++ DSP specifications.
 */
class CenterChannelParityContractTest {
    @Test
    fun vocalFocusUsesNativeIntendedAirShelfContract() {
        assertEquals(2600.0f, CenterChannelProcessor.VOCAL_PRESENCE_HZ, 0.001f)
        assertEquals(6.0f, CenterChannelProcessor.VOCAL_PRESENCE_MAX_DB, 0.001f)
        assertEquals(6000.0f, CenterChannelProcessor.VOCAL_AIR_HZ, 0.001f)
        assertEquals(3.0f, CenterChannelProcessor.VOCAL_AIR_MAX_DB, 0.001f)
    }

    @Test
    fun offModeWithoutStemBalanceLeavesSamplesUnchanged() {
        val p = CenterChannelProcessor()
        p.configure(CenterChannelProcessor.MODE_OFF, 0f)
        val frame = floatArrayOf(0.123f, -0.456f)
        val expected = frame.copyOf()
        p.processFrame(frame, 0)
        assertEquals(expected[0], frame[0], 0f)
        assertEquals(expected[1], frame[1], 0f)
    }

    @Test
    fun neutralMidSideReconstructionPreservesStereoSignal() {
        val p = CenterChannelProcessor()
        p.configure(CenterChannelProcessor.MODE_OFF, 0f)
        val inputs = arrayOf(
            floatArrayOf(0.2f, 0.4f),
            floatArrayOf(-0.7f, 0.1f),
            floatArrayOf(0f, 0.9f)
        )
        for (input in inputs) {
            val expected = input.copyOf()
            p.processFrame(input, 0)
            assertEquals(expected[0], input[0], 0f)
            assertEquals(expected[1], input[1], 0f)
        }
    }

    @Test
    fun usbInspectorDoesNotInferClassVersionOrClockingFromMissingEvidence() {
        val report = UsbCapabilityClassifier.classify(UsbAudioEvidence())
        assertEquals(UsbAudioClassVersion.UNKNOWN, report.classVersion)
        assertEquals(UsbClockingMode.UNKNOWN, report.clockingMode)
        assertEquals(CapabilityStatus.UNKNOWN, report.bitPerfectStatus)
        assertFalse(report.hasPlaybackOutEndpoint)
    }

    @Test
    fun usbInspectorReportsOnlyExplicitClassAndClockEvidence() {
        val report = UsbCapabilityClassifier.classify(
            UsbAudioEvidence(
                classVersion = UsbAudioClassVersion.UAC2,
                clockingMode = UsbClockingMode.ASYNCHRONOUS,
                hasPlaybackOutEndpoint = true,
                supportedSampleRates = listOf(96000, 44100, 96000, 0),
                activeAndroidRouteName = "USB DAC",
                androidBitPerfectApiAvailable = true,
                bitPerfectModeConfirmedByFramework = false
            )
        )
        assertEquals(UsbAudioClassVersion.UAC2, report.classVersion)
        assertEquals(UsbClockingMode.ASYNCHRONOUS, report.clockingMode)
        assertEquals(listOf(44100, 96000), report.sampleRatesHz)
        assertEquals(CapabilityStatus.UNKNOWN, report.bitPerfectStatus)
        assertTrue(report.hasPlaybackOutEndpoint)
    }
}
