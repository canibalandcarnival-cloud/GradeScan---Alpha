package com.example

import com.example.ai.ColorGradingAnalyzer
import com.google.genai.Client
import com.google.genai.gaos.models.interactions.Error
import com.google.genai.gaos.models.interactions.ImageContent
import com.google.genai.gaos.models.interactions.Interaction
import com.google.genai.gaos.models.interactions.InteractionStatus
import com.google.genai.gaos.models.interactions.Model
import com.google.genai.gaos.models.interactions.TextResponseFormatMimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `google-genai Client builds cleanly for Gemini Developer API`() {
        val client = Client.builder()
            .apiKey("test-api-key")
            .vertexAI(false)
            .build()
        assertNotNull(client.interactions)
        client.close()
    }

    @Test
    fun `buildModelInteraction targets gemini-3_8-flash with multimodal image and JSON schema`() {
        val request = analyzer.buildModelInteraction("ZmFrZUJhc2U2NA==")

        assertEquals(Model.GEMINI38_FLASH, request.model().orElse(null))

        val contents = request.input().orElseThrow().arrayOfContent().orElseThrow()
        assertEquals(2, contents.size)
        val imageContent = contents[0] as ImageContent
        assertEquals("ZmFrZUJhc2U2NA==", imageContent.data().orElse(null))

        val textResponseFormat = request.responseFormat()
            .orElseThrow()
            .responseFormat()
            .orElseThrow()
            .textResponseFormat()
            .orElseThrow()
        assertEquals(TextResponseFormatMimeType.APPLICATION_JSON, textResponseFormat.mimeType().orElse(null))
        val schema = textResponseFormat.schema().orElseThrow()
        assertEquals("object", schema["type"])
        @Suppress("UNCHECKED_CAST")
        val props = schema["properties"] as Map<String, Any>
        assertTrue(props.containsKey("exposure"))
        assertTrue(props.containsKey("highlightsHue"))
        assertTrue(props.containsKey("grain"))
    }

    @Test
    fun `extractInteractionOutputText and parseProfile decode Interactions response`() {
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

        val interaction = Interaction.builder()
            .status(InteractionStatus.COMPLETED)
            .outputText(innerJson)
            .build()

        val extracted = analyzer.extractInteractionOutputText(interaction)
        val profile = analyzer.parseProfile(extracted)

        assertEquals("Amber Noir", profile.presetName)
        assertEquals("Warm Teal & Orange Cinematic", profile.moodSummary)
        assertEquals(0.45f, profile.exposure, 0.001f)
        assertEquals(6200, profile.temperature)
        assertEquals(202, profile.shadowsHue)
        assertEquals(18, profile.grain)
    }

    @Test
    fun `extractInteractionOutputText surfaces error message when interaction fails`() {
        val failedInteraction = Interaction.builder()
            .status(InteractionStatus.FAILED)
            .errors(listOf(Error.builder().message("API key not valid.").build()))
            .build()

        var thrown: IllegalStateException? = null
        try {
            analyzer.extractInteractionOutputText(failedInteraction)
        } catch (e: IllegalStateException) {
            thrown = e
        }
        assertEquals("API key not valid.", thrown?.message)
    }
}
