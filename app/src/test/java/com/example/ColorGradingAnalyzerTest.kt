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

    @Test
    fun `interactions create serializes exact expected v1beta URL headers and JSON body`() {
        val server = java.net.ServerSocket(0)
        val port = server.localPort
        var capturedRequest = ""
        val serverThread = Thread {
            server.use { srv ->
                srv.accept().use { socket ->
                    val input = socket.getInputStream()
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
                    val sb = StringBuilder()
                    var contentLength = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        sb.append(line).append("\n")
                        if (line.lowercase().startsWith("content-length:")) {
                            contentLength = line.substringAfter(":").trim().toInt()
                        }
                        if (line.isEmpty()) break
                    }
                    val bodyChars = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(bodyChars, read, contentLength - read)
                        if (r <= 0) break
                        read += r
                    }
                    sb.append(String(bodyChars, 0, read))
                    capturedRequest = sb.toString()

                    val responseJson = """{"id":"int_1","status":"completed","outputs":[{"type":"text","text":"{}"}]}"""
                    val httpResp = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseJson.toByteArray().size}\r\nConnection: close\r\n\r\n$responseJson"
                    socket.getOutputStream().write(httpResp.toByteArray())
                    socket.getOutputStream().flush()
                }
            }
        }
        serverThread.start()

        val client = Client.builder()
            .apiKey("test-api-key")
            .vertexAI(false)
            .httpOptions(
                com.google.genai.types.HttpOptions.builder()
                    .baseUrl("http://127.0.0.1:$port")
                    .apiVersion("v1beta")
                    .build()
            )
            .build()
        val requestModel = analyzer.buildModelInteraction("ZmFrZUJhc2U2NA==")
        client.interactions.create(
            com.google.genai.gaos.models.operations.CreateInteractionRequestBody.of(requestModel)
        )
        serverThread.join(5000)
        client.close()

        assertTrue(capturedRequest.startsWith("POST /v1beta/interactions HTTP/1.1"))
        assertTrue(capturedRequest.contains("\"model\":\"gemini-3.8-flash\""))
        assertTrue(!capturedRequest.contains("models/"))
        assertTrue(!capturedRequest.contains("\"response_mime_type\""))
        assertTrue(!capturedRequest.contains("\"generation_config\""))
        assertTrue(!capturedRequest.contains("\"store\""))
        assertTrue(capturedRequest.contains("\"response_format\":{"))
    }

    @Test
    fun `formatApiExceptionDetails and buildUserFacingErrorMessage expose full 400 error body`() {
        val errorBody = """{"error":{"code":"invalid_request","message":"Invalid schema field"}}"""
        val httpRequest = com.google.genai.gaos.utils.transport.HttpRequest.builder()
            .method("POST")
            .uri(java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
            .build()
        val bodyStream: java.io.InputStream = java.io.ByteArrayInputStream(errorBody.toByteArray())
        val httpResponse = com.google.genai.gaos.utils.transport.HttpResponse<java.io.InputStream>(
            httpRequest,
            400,
            com.google.genai.gaos.utils.Headers.EMPTY,
            bodyStream
        )
        val clientError = com.google.genai.gaos.models.errors.CreateInteractionClientError.from(httpResponse)

        val formatted = analyzer.formatApiExceptionDetails(clientError)
        val userMessage = analyzer.buildUserFacingErrorMessage(clientError)

        assertTrue(formatted.contains("HTTP 400"))
        assertTrue(formatted.contains("Invalid schema field"))
        assertTrue(formatted.contains(errorBody))
        assertEquals("Gemini API error (HTTP 400): Invalid schema field", userMessage)
    }
}
