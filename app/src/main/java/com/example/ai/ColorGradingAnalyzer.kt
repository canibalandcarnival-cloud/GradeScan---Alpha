package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ColorGradingProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "ColorGradingAnalyzer"

class ColorGradingAnalyzer {

    private val colorGradingSchema = Schema.obj(
        properties = mapOf(
            "presetName" to Schema.string(
                description = "Short, evocative 2 to 4 word Lightroom preset name matching the aesthetic, e.g. 'Warm Teal Cinema'"
            ),
            "moodSummary" to Schema.string(
                description = "Concise mood summary label, e.g. 'Warm Teal & Orange Cinematic' or 'Moody Matte Nordic Film'"
            ),
            "moodNotes" to Schema.string(
                description = "One-sentence colorist note explaining the contrast curve, white balance bias, and shadow/highlight split"
            ),
            "exposure" to Schema.double(
                description = "Estimated Lightroom Exposure2012 adjustment in EV from -3.0 to +3.0"
            ),
            "contrast" to Schema.integer(
                description = "Estimated Lightroom Contrast2012 from -100 to +100"
            ),
            "highlights" to Schema.integer(
                description = "Estimated Lightroom Highlights2012 from -100 to +100"
            ),
            "shadows" to Schema.integer(
                description = "Estimated Lightroom Shadows2012 from -100 to +100"
            ),
            "whites" to Schema.integer(
                description = "Estimated Lightroom Whites2012 from -100 to +100"
            ),
            "blacks" to Schema.integer(
                description = "Estimated Lightroom Blacks2012 from -100 to +100"
            ),
            "temperature" to Schema.integer(
                description = "Estimated color Temperature in Kelvin from 2500 (cool) to 10000 (warm), neutral daylight is 5500"
            ),
            "tint" to Schema.integer(
                description = "Estimated green-magenta Tint from -150 (green) to +150 (magenta)"
            ),
            "vibrance" to Schema.integer(
                description = "Estimated Lightroom Vibrance from -100 to +100"
            ),
            "saturation" to Schema.integer(
                description = "Estimated Lightroom Saturation from -100 to +100"
            ),
            "highlightsHue" to Schema.integer(
                description = "3-Way Color Grading Highlights Hue angle in degrees from 0 to 360"
            ),
            "highlightsSaturation" to Schema.integer(
                description = "3-Way Color Grading Highlights Saturation from 0 to 100"
            ),
            "midtonesHue" to Schema.integer(
                description = "3-Way Color Grading Midtones Hue angle in degrees from 0 to 360"
            ),
            "midtonesSaturation" to Schema.integer(
                description = "3-Way Color Grading Midtones Saturation from 0 to 100"
            ),
            "shadowsHue" to Schema.integer(
                description = "3-Way Color Grading Shadows Hue angle in degrees from 0 to 360"
            ),
            "shadowsSaturation" to Schema.integer(
                description = "3-Way Color Grading Shadows Saturation from 0 to 100"
            ),
            "texture" to Schema.integer(
                description = "Estimated Lightroom Texture from -100 to +100"
            ),
            "clarity" to Schema.integer(
                description = "Estimated Lightroom Clarity2012 from -100 to +100"
            ),
            "dehaze" to Schema.integer(
                description = "Estimated Lightroom Dehaze from -100 to +100"
            ),
            "vignette" to Schema.integer(
                description = "Estimated Post-Crop Vignette amount from -100 (dark edges) to +100 (bright edges)"
            ),
            "grain" to Schema.integer(
                description = "Estimated film Grain amount from 0 to 100"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = colorGradingSchema
            }
        )
    }

    suspend fun analyzeImage(bitmap: Bitmap): ColorGradingProfile {
        val prompt = """
            You are a master digital colorist and Adobe Lightroom preset engineer.
            Carefully inspect this reference photograph's luminance distribution, black/white points,
            color temperature, skin/midtone warmth, highlight/shadow split-toning, micro-contrast,
            vignetting, and film grain.

            Output an accurate set of Adobe Lightroom Classic / Mobile develop settings that replicate
            this exact color grading look when applied as a preset to a neutral reference photo.
            Keep values realistic and well-calibrated (e.g., avoid extreme clipping unless clearly stylized,
            use lifted blacks / negative contrast for faded matte film looks, and precise 3-way hue angles
            in degrees where 0=red, 35=amber/orange, 180=cyan, 210=teal/blue).
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")
            parseProfile(jsonText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Color grading analysis failed", e)
            throw e
        }
    }

    private fun parseProfile(jsonText: String): ColorGradingProfile {
        val json = JSONObject(jsonText)
        return ColorGradingProfile(
            presetName = json.optString("presetName", "GradeScan Look").ifBlank { "GradeScan Look" },
            moodSummary = json.optString("moodSummary", "Balanced Studio Grade")
                .ifBlank { "Balanced Studio Grade" },
            moodNotes = json.optString(
                "moodNotes",
                "Calibrated tonal balance with subtle split-tone separation."
            ),
            exposure = ((json.optDouble("exposure", 0.0) * 100.0).roundToInt() / 100f)
                .coerceIn(-5.0f, 5.0f),
            contrast = json.optInt("contrast", 0).coerceIn(-100, 100),
            highlights = json.optInt("highlights", 0).coerceIn(-100, 100),
            shadows = json.optInt("shadows", 0).coerceIn(-100, 100),
            whites = json.optInt("whites", 0).coerceIn(-100, 100),
            blacks = json.optInt("blacks", 0).coerceIn(-100, 100),
            temperature = json.optInt("temperature", 5500).coerceIn(2000, 12000),
            tint = json.optInt("tint", 0).coerceIn(-150, 150),
            vibrance = json.optInt("vibrance", 0).coerceIn(-100, 100),
            saturation = json.optInt("saturation", 0).coerceIn(-100, 100),
            highlightsHue = json.optInt("highlightsHue", 0).mod(360),
            highlightsSaturation = json.optInt("highlightsSaturation", 0).coerceIn(0, 100),
            midtonesHue = json.optInt("midtonesHue", 0).mod(360),
            midtonesSaturation = json.optInt("midtonesSaturation", 0).coerceIn(0, 100),
            shadowsHue = json.optInt("shadowsHue", 0).mod(360),
            shadowsSaturation = json.optInt("shadowsSaturation", 0).coerceIn(0, 100),
            texture = json.optInt("texture", 0).coerceIn(-100, 100),
            clarity = json.optInt("clarity", 0).coerceIn(-100, 100),
            dehaze = json.optInt("dehaze", 0).coerceIn(-100, 100),
            vignette = json.optInt("vignette", 0).coerceIn(-100, 100),
            grain = json.optInt("grain", 0).coerceIn(0, 100)
        )
    }
}
