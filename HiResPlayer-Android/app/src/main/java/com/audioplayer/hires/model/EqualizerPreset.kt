package com.audioplayer.hires.model

data class GraphicEqPreset(
    val name: String,
    val gainsDb: FloatArray
)

object FactoryEqPresets {
    val GRAPHIC_PRESETS = listOf(
        GraphicEqPreset("Flat (Bypass)", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        GraphicEqPreset("Studio Reference", floatArrayOf(0.5f, 0.5f, 0f, 0f, 0f, 0.5f, 1.0f, 0.5f, 1.0f, 1.5f)),
        GraphicEqPreset("Vocal Clarity", floatArrayOf(-2.0f, -1.0f, 0f, 1.0f, 2.5f, 3.5f, 3.0f, 1.5f, 0.5f, 1.0f)),
        GraphicEqPreset("Warm Acoustic", floatArrayOf(2.5f, 2.0f, 1.5f, 1.0f, 0.5f, 0f, -0.5f, -1.0f, -1.5f, -2.0f)),
        GraphicEqPreset("Bright & Airy", floatArrayOf(-1.5f, -1.0f, -0.5f, 0f, 0.5f, 1.0f, 2.0f, 3.0f, 4.0f, 4.5f)),
        GraphicEqPreset("Bass Boost", floatArrayOf(5.0f, 4.0f, 3.0f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        GraphicEqPreset("Bass Reduction", floatArrayOf(-5.0f, -4.0f, -3.0f, -1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        GraphicEqPreset("Treble Boost", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0.5f, 1.5f, 3.0f, 4.5f, 5.0f)),
        GraphicEqPreset("Treble Reduction", floatArrayOf(0f, 0f, 0f, 0f, 0f, -0.5f, -1.5f, -3.0f, -4.5f, -5.0f)),
        GraphicEqPreset("Rock & Metal", floatArrayOf(4.0f, 3.0f, 1.5f, 0f, -1.0f, -0.5f, 1.5f, 2.5f, 3.5f, 4.0f)),
        GraphicEqPreset("Electronic / EDM", floatArrayOf(5.5f, 4.5f, 2.0f, 0f, -1.0f, 0.5f, 1.0f, 2.5f, 4.0f, 4.5f)),
        GraphicEqPreset("Hip-Hop / Urban", floatArrayOf(6.0f, 5.0f, 2.5f, 1.0f, 0f, -0.5f, 0.5f, 1.5f, 3.0f, 3.5f)),
        GraphicEqPreset("Jazz Club", floatArrayOf(2.5f, 2.0f, 0.5f, 1.5f, -0.5f, -0.5f, 0.5f, 1.0f, 2.0f, 2.5f)),
        GraphicEqPreset("Classical / Orchestra", floatArrayOf(3.0f, 2.5f, 1.5f, 0.5f, -0.5f, -0.5f, 0f, 1.5f, 2.5f, 3.0f)),
        GraphicEqPreset("Night Listening", floatArrayOf(-3.0f, -2.0f, 0f, 1.0f, 1.5f, 1.0f, 0f, -1.0f, -2.5f, -4.0f))
    )
}
