package com.example

import com.example.ai.ColorGradingAnalyzer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ColorGradingAnalyzerTest {

    private val analyzer = ColorGradingAnalyzer()

    @Test
    fun `buildRequestPayload includes image inline_data and structured JSON schema`() {
        val payload = analyzer.buildRequestPayload("ZmFrZUJhc2U2NA==")
        val root = JSONObject(payload)

        val parts = root.getJSONArray("contents")
            .getJSONObject(0)
            .getJSONArray("parts")
        assertEquals(2, parts.length())
        assertEquals("image/jpeg", parts.getJSONObject(0).getJSONObject("inline_data").getString("mime_type"))
        assertEquals("ZmFrZUJhc2U2NA==", parts.getJSONObject(0).getJSONObject("inline_data").getString("data"))

        val genConfig = root.getJSONObject("generationConfig")
        assertEquals("application/json", genConfig.getString("responseMimeType"))
        val schema = genConfig.getJSONObject("responseSchema")
        assertEquals("OBJECT", schema.getString("type"))
        assertTrue(schema.getJSONObject("properties").has("exposure"))
        assertTrue(schema.getJSONObject("properties").has("highlightsHue"))
        assertTrue(schema.getJSONObject("properties").has("grain"))
    }

    @Test
    fun `extractGeneratedText and parseProfile decode standard Gemini API response`() {
        val innerJson = """
            {
              "presetName": "Amber Noir",
              "moodSummary": "Warm Teal & Orange Cinematic",
              "moodNotes": "Lifted shadows with crisp amber highlights.",
              "exposure": 0.45,
              "contrast": 20,
              "highlights": -25,
              "shadows": 18,
              "whites": 10,
              "blacks": -12,
              "temperature": 6200,
              "tint": 6,
              "vibrance": 24,
              "saturation": -4,
              "highlightsHue": 38,
              "highlightsSaturation": 26,
              "midtonesHue": 30,
              "midtonesSaturation": 8,
              "shadowsHue": 202,
              "shadowsSaturation": 30,
              "texture": 10,
              "clarity": 15,
              "dehaze": 6,
              "vignette": -20,
              "grain": 18
            }
        """.trimIndent()

        val apiResponse = JSONObject().put(
            "candidates",
            org.json.JSONArray().put(
                JSONObject().put(
                    "content",
                    JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(JSONObject().put("text", innerJson))
                    )
                )
            )
        ).toString()

        val extracted = analyzer.extractGeneratedText(apiResponse)
        val profile = analyzer.parseProfile(extracted)

        assertEquals("Amber Noir", profile.presetName)
        assertEquals("Warm Teal & Orange Cinematic", profile.moodSummary)
        assertEquals(0.45f, profile.exposure, 0.001f)
        assertEquals(6200, profile.temperature)
        assertEquals(202, profile.shadowsHue)
        assertEquals(18, profile.grain)
    }

    @Test
    fun `extractApiErrorMessage parses standard Gemini error envelope`() {
        val errorJson = """{"error":{"code":400,"message":"API key not valid."}}"""
        assertEquals("API key not valid.", analyzer.extractApiErrorMessage(errorJson))
    }
}
