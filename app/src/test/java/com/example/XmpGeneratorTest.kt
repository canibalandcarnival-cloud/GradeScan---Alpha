package com.example

import com.example.model.ColorGradingProfile
import com.example.util.XmpGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XmpGeneratorTest {

    @Test
    fun `buildXmpContent produces valid parseable XML with all Lightroom crs settings`() {
        val profile = ColorGradingProfile(
            presetName = "Warm & Moody <Film>",
            moodSummary = "Warm Teal & Orange Cinematic",
            moodNotes = "Rich amber highlights with cool teal shadows.",
            exposure = 0.35f,
            contrast = 18,
            highlights = -28,
            shadows = 24,
            whites = 12,
            blacks = -14,
            temperature = 6100,
            tint = 8,
            vibrance = 22,
            saturation = -6,
            highlightsHue = 42,
            highlightsSaturation = 24,
            midtonesHue = 35,
            midtonesSaturation = 10,
            shadowsHue = 205,
            shadowsSaturation = 28,
            texture = 12,
            clarity = 16,
            dehaze = 8,
            vignette = -18,
            grain = 20
        )

        val xmp = XmpGenerator.buildXmpContent(profile, uuid = "TESTUUID1234")

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val doc = factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(xmp.toByteArray(Charsets.UTF_8)))

        assertEquals("xmpmeta", doc.documentElement.localName)
        assertTrue(xmp.contains("""crs:Exposure2012="+0.35""""))
        assertTrue(xmp.contains("""crs:Contrast2012="+18""""))
        assertTrue(xmp.contains("""crs:Highlights2012="-28""""))
        assertTrue(xmp.contains("""crs:Temperature="6100""""))
        assertTrue(xmp.contains("""crs:ColorGradeHighlightHue="42""""))
        assertTrue(xmp.contains("""crs:ColorGradeShadowHue="205""""))
        assertTrue(xmp.contains("""crs:GrainAmount="20""""))
        assertTrue(xmp.contains("Warm &amp; Moody &lt;Film&gt;"))
    }

    @Test
    fun `buildPresetFileName sanitizes special characters`() {
        val profile = ColorGradingProfile(
            presetName = "Warm Teal & Orange!",
            moodSummary = "Warm Teal & Orange",
            moodNotes = "",
            exposure = 0f,
            contrast = 0,
            highlights = 0,
            shadows = 0,
            whites = 0,
            blacks = 0,
            temperature = 5500,
            tint = 0,
            vibrance = 0,
            saturation = 0,
            highlightsHue = 0,
            highlightsSaturation = 0,
            midtonesHue = 0,
            midtonesSaturation = 0,
            shadowsHue = 0,
            shadowsSaturation = 0,
            texture = 0,
            clarity = 0,
            dehaze = 0,
            vignette = 0,
            grain = 0
        )

        assertEquals("GradeScan_Warm_Teal_Orange.xmp", XmpGenerator.buildPresetFileName(profile))
    }
}
