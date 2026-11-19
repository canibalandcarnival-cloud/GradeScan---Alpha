package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ColorGradingProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "ColorGradingAnalyzer"
private const val GEMINI_MODEL_ID = "gemini-2.5-flash"
private const val GEMINI_ENDPOINT =
    "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL_ID:generateContent"

class ColorGradingAnalyzer(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    suspend fun analyzeImage(bitmap: Bitmap): ColorGradingProfile {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "GEMINI_API_KEY is missing or unconfigured.")
            throw IllegalStateException(
                "Gemini API key is missing. Add GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        return try {
            withTimeout(60_000L) {
                val base64Jpeg = withContext(Dispatchers.Default) {
                    encodeBitmapToBase64Jpeg(bitmap)
                }
                val requestJson = buildRequestPayload(base64Jpeg)
                val responseBody = withContext(Dispatchers.IO) {
                    executeGenerateContentRequest(apiKey, requestJson)
                }
                val generatedJsonText = extractGeneratedText(responseBody)
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

    private fun executeGenerateContentRequest(apiKey: String, requestJson: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(GEMINI_ENDPOINT)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(requestJson.toRequestBody(mediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiError = extractApiErrorMessage(bodyString)
                throw IllegalStateException(
                    apiError ?: "Gemini API request failed (HTTP ${response.code})."
                )
            }
            if (bodyString.isBlank()) {
                throw IllegalStateException("Gemini API returned an empty response body.")
            }
            return bodyString
        }
    }

    internal fun buildRequestPayload(base64Jpeg: String): String {
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

        val inlineData = JSONObject().apply {
            put("mime_type", "image/jpeg")
            put("data", base64Jpeg)
        }
        val imagePart = JSONObject().put("inline_data", inlineData)
        val textPart = JSONObject().put("text", prompt)
        val parts = JSONArray().apply {
            put(imagePart)
            put(textPart)
        }
        val content = JSONObject().apply {
            put("role", "user")
            put("parts", parts)
        }

        val generationConfig = JSONObject().apply {
            put("temperature", 0.2)
            put("responseMimeType", "application/json")
            put("responseSchema", buildResponseSchema())
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(content))
            put("generationConfig", generationConfig)
        }.toString()
    }

    private fun buildResponseSchema(): JSONObject {
        val properties = JSONObject().apply {
            put("presetName", schemaProp("STRING", "Short 2 to 4 word Lightroom preset name"))
            put("moodSummary", schemaProp("STRING", "Concise mood summary, e.g. 'Warm Teal & Orange Cinematic'"))
            put("moodNotes", schemaProp("STRING", "One-sentence colorist note explaining the look"))
            put("exposure", schemaProp("NUMBER", "Lightroom Exposure2012 in EV from -3.0 to +3.0"))
            put("contrast", schemaProp("INTEGER", "Lightroom Contrast2012 from -100 to +100"))
            put("highlights", schemaProp("INTEGER", "Lightroom Highlights2012 from -100 to +100"))
            put("shadows", schemaProp("INTEGER", "Lightroom Shadows2012 from -100 to +100"))
            put("whites", schemaProp("INTEGER", "Lightroom Whites2012 from -100 to +100"))
            put("blacks", schemaProp("INTEGER", "Lightroom Blacks2012 from -100 to +100"))
            put("temperature", schemaProp("INTEGER", "Color Temperature in Kelvin from 2500 to 10000"))
            put("tint", schemaProp("INTEGER", "Green-magenta Tint from -150 to +150"))
            put("vibrance", schemaProp("INTEGER", "Lightroom Vibrance from -100 to +100"))
            put("saturation", schemaProp("INTEGER", "Lightroom Saturation from -100 to +100"))
            put("highlightsHue", schemaProp("INTEGER", "3-Way Highlights Hue angle 0 to 360"))
            put("highlightsSaturation", schemaProp("INTEGER", "3-Way Highlights Saturation 0 to 100"))
            put("midtonesHue", schemaProp("INTEGER", "3-Way Midtones Hue angle 0 to 360"))
            put("midtonesSaturation", schemaProp("INTEGER", "3-Way Midtones Saturation 0 to 100"))
            put("shadowsHue", schemaProp("INTEGER", "3-Way Shadows Hue angle 0 to 360"))
            put("shadowsSaturation", schemaProp("INTEGER", "3-Way Shadows Saturation 0 to 100"))
            put("texture", schemaProp("INTEGER", "Lightroom Texture from -100 to +100"))
            put("clarity", schemaProp("INTEGER", "Lightroom Clarity2012 from -100 to +100"))
            put("dehaze", schemaProp("INTEGER", "Lightroom Dehaze from -100 to +100"))
            put("vignette", schemaProp("INTEGER", "Post-Crop Vignette amount from -100 to +100"))
            put("grain", schemaProp("INTEGER", "Film Grain amount from 0 to 100"))
        }

        val required = JSONArray(
            listOf(
                "presetName", "moodSummary", "moodNotes",
                "exposure", "contrast", "highlights", "shadows", "whites", "blacks",
                "temperature", "tint", "vibrance", "saturation",
                "highlightsHue", "highlightsSaturation",
                "midtonesHue", "midtonesSaturation",
                "shadowsHue", "shadowsSaturation",
                "texture", "clarity", "dehaze", "vignette", "grain"
            )
        )

        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", properties)
            put("required", required)
        }
    }

    private fun schemaProp(type: String, description: String): JSONObject =
        JSONObject().apply {
            put("type", type)
            put("description", description)
        }

    internal fun extractGeneratedText(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini returned no candidates.")
        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini returned an empty candidates list.")
        }
        val content = candidates.getJSONObject(0).optJSONObject("content")
            ?: throw IllegalStateException("Gemini candidate missing content.")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini candidate missing parts.")
        val text = (0 until parts.length())
            .mapNotNull { idx -> parts.getJSONObject(idx).optString("text", "") }
            .firstOrNull { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned no text output.")
        return text
    }

    internal fun extractApiErrorMessage(errorBody: String): String? {
        if (errorBody.isBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
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
