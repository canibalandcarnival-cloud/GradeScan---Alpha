package com.example.model

data class ColorGradingProfile(
    val presetName: String,
    val moodSummary: String,
    val moodNotes: String,
    // Basic Tone
    val exposure: Float,       // -5.00 to +5.00 EV
    val contrast: Int,         // -100 to +100
    val highlights: Int,       // -100 to +100
    val shadows: Int,          // -100 to +100
    val whites: Int,           // -100 to +100
    val blacks: Int,           // -100 to +100
    // Color
    val temperature: Int,      // 2000 to 12000 K
    val tint: Int,             // -150 to +150
    val vibrance: Int,         // -100 to +100
    val saturation: Int,       // -100 to +100
    // Color Grading (3-Way)
    val highlightsHue: Int,        // 0 to 360
    val highlightsSaturation: Int, // 0 to 100
    val midtonesHue: Int,          // 0 to 360
    val midtonesSaturation: Int,   // 0 to 100
    val shadowsHue: Int,           // 0 to 360
    val shadowsSaturation: Int,    // 0 to 100
    // Effects
    val texture: Int,          // -100 to +100
    val clarity: Int,          // -100 to +100
    val dehaze: Int,           // -100 to +100
    val vignette: Int,         // -100 to +100
    val grain: Int             // 0 to 100
)

data class HistogramData(
    val redBins: List<Float>,
    val greenBins: List<Float>,
    val blueBins: List<Float>,
    val lumaBins: List<Float>,
    val shadowSharePercent: Int,
    val midtoneSharePercent: Int,
    val highlightSharePercent: Int
)
