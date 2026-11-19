package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ColorGradingProfile
import com.google.genai.Client
import com.google.genai.gaos.models.interactions.CreateModelInteraction
import com.google.genai.gaos.models.interactions.CreateModelInteractionResponseFormat
import com.google.genai.gaos.models.interactions.ImageContent
import com.google.genai.gaos.models.interactions.ImageContentMimeType
import com.google.genai.gaos.models.interactions.Interaction
import com.google.genai.gaos.models.interactions.InteractionStatus
import com.google.genai.gaos.models.interactions.InteractionsInput
import com.google.genai.gaos.models.interactions.Model
import com.google.genai.gaos.models.interactions.ResponseFormat
import com.google.genai.gaos.models.interactions.TextContent
import com.google.genai.gaos.models.interactions.TextResponseFormat
import com.google.genai.gaos.models.interactions.TextResponseFormatMimeType
import com.google.genai.gaos.models.operations.CreateInteractionRequestBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private const val TAG = "ColorGradingAnalyzer"

class ColorGradingAnalyzer(
    private val clientFactory: (String) -> Client = { apiKey ->
        Client.builder()
            .apiKey(apiKey)
            .vertexAI(false)
            .build()
    }
) {

    private val client: Client by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "GEMINI_API_KEY is missing or unconfigured.")
            throw IllegalStateException(
                "Gemini API key is missing. Add GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }
        clientFactory(apiKey)
    }

    suspend fun analyzeImage(bitmap: Bitmap): ColorGradingProfile {
        return try {
            withTimeout(60_000L) {
                val base64Jpeg = withContext(Dispatchers.Default) {
                    encodeBitmapToBase64Jpeg(bitmap)
                }
                val requestModel = buildModelInteraction(base64Jpeg)
                val response = withContext(Dispatchers.IO) {
                    client.interactions.create(
                        CreateInteractionRequestBody.of(requestModel)
                    )
                }
                val interaction = response.interaction().orElseThrow {
                    IllegalStateException(
                        "Gemini Interactions API returned no interaction (HTTP ${response.statusCode()})."
                    )
                }
                val generatedJsonText = extractInteractionOutputText(interaction)
                parseProfile(generatedJsonText)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Color grading analysis timed out after 60s", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Color grading analysis failed", e)
            throw e
        }
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    internal fun buildModelInteraction(base64Jpeg: String): CreateModelInteraction {
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

        val imageContent = ImageContent.builder()
            .data(base64Jpeg)
            .mimeType(ImageContentMimeType.IMAGE_JPEG)
            .build()
        val textContent = TextContent.builder()
            .text(prompt)
            .build()

        val textResponseFormat = TextResponseFormat.builder()
            .mimeType(TextResponseFormatMimeType.APPLICATION_JSON)
            .schema(buildResponseSchemaMap())
            .build()

        return CreateModelInteraction.builder()
            .model(Model.GEMINI38_FLASH)
            .input(InteractionsInput.ofContent(listOf(imageContent, textContent)))
            .responseFormat(
                CreateModelInteractionResponseFormat.of(
                    ResponseFormat.of(textResponseFormat)
                )
            )
            .store(false)
            .build()
    }

    internal fun buildResponseSchemaMap(): Map<String, Any> {
        val properties = mapOf(
            "presetName" to schemaProp("string", "Short 2 to 4 word Lightroom preset name"),
            "moodSummary" to schemaProp("string", "Concise mood summary, e.g. 'Warm Teal & Orange Cinematic'"),
            "moodNotes" to schemaProp("string", "One-sentence colorist note explaining the look"),
            "exposure" to schemaProp("number", "Lightroom Exposure2012 in EV from -3.0 to +3.0"),
            "contrast" to schemaProp("integer", "Lightroom Contrast2012 from -100 to +100"),
            "highlights" to schemaProp("integer", "Lightroom Highlights2012 from -100 to +100"),
            "shadows" to schemaProp("integer", "Lightroom Shadows2012 from -100 to +100"),
            "whites" to schemaProp("integer", "Lightroom Whites2012 from -100 to +100"),
            "blacks" to schemaProp("integer", "Lightroom Blacks2012 from -100 to +100"),
            "temperature" to schemaProp("integer", "Color Temperature in Kelvin from 2500 to 10000"),
            "tint" to schemaProp("integer", "Green-magenta Tint from -150 to +150"),
            "vibrance" to schemaProp("integer", "Lightroom Vibrance from -100 to +100"),
            "saturation" to schemaProp("integer", "Lightroom Saturation from -100 to +100"),
            "highlightsHue" to schemaProp("integer", "3-Way Highlights Hue angle 0 to 360"),
            "highlightsSaturation" to schemaProp("integer", "3-Way Highlights Saturation 0 to 100"),
            "midtonesHue" to schemaProp("integer", "3-Way Midtones Hue angle 0 to 360"),
            "midtonesSaturation" to schemaProp("integer", "3-Way Midtones Saturation 0 to 100"),
            "shadowsHue" to schemaProp("integer", "3-Way Shadows Hue angle 0 to 360"),
            "shadowsSaturation" to schemaProp("integer", "3-Way Shadows Saturation 0 to 100"),
            "texture" to schemaProp("integer", "Lightroom Texture from -100 to +100"),
            "clarity" to schemaProp("integer", "Lightroom Clarity2012 from -100 to +100"),
            "dehaze" to schemaProp("integer", "Lightroom Dehaze from -100 to +100"),
            "vignette" to schemaProp("integer", "Post-Crop Vignette amount from -100 to +100"),
            "grain" to schemaProp("integer", "Film Grain amount from 0 to 100")
        )

        return mapOf(
            "type" to "object",
            "properties" to properties,
            "required" to properties.keys.toList()
        )
    }

    private fun schemaProp(type: String, description: String): Map<String, Any> = mapOf(
        "type" to type,
        "description" to description
    )

    internal fun extractInteractionOutputText(interaction: Interaction): String {
        val status = interaction.status().orElse(null)
        if (status == InteractionStatus.FAILED || status == InteractionStatus.CANCELLED) {
            val errorMsg = interaction.errors()
                .orElse(emptyList())
                .mapNotNull { it.message().orElse(null) }
                .firstOrNull { it.isNotBlank() }
            throw IllegalStateException(
                errorMsg ?: "Gemini interaction ended with status: ${status.value()}"
            )
        }

        val text = interaction.outputText().orElse("")
        if (text.isBlank()) {
            throw IllegalStateException("Gemini Interactions API returned empty output text.")
        }
        return text
    }

    internal fun parseProfile(jsonText: String): ColorGradingProfile {
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
