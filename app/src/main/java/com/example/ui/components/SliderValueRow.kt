package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun IntSliderRow(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    unitSuffix: String = "",
    showSigned: Boolean = valueRange.first < 0,
    accentColor: Color = AmberGold,
    swatchColor: Color? = null
) {
    val formattedValue = when {
        showSigned && value > 0 -> "+$value$unitSuffix"
        else -> "$value$unitSuffix"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (swatchColor != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(1.dp, StudioBorder, CircleShape)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            ValueBadge(text = formattedValue, accentColor = accentColor)
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor.copy(alpha = 0.85f),
                inactiveTrackColor = StudioSurfaceHighest
            )
        )
    }
}

@Composable
fun FloatExposureRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val formatted = String.format(Locale.US, "%+.2f EV", value)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            ValueBadge(text = formatted, accentColor = AmberGold)
        }

        Slider(
            value = value,
            onValueChange = { raw ->
                val snapped = (raw * 20f).roundToInt() / 20f
                onValueChange(snapped.coerceIn(-5.0f, 5.0f))
            },
            valueRange = -3.0f..3.0f,
            colors = SliderDefaults.colors(
                thumbColor = AmberGold,
                activeTrackColor = AmberGold.copy(alpha = 0.85f),
                inactiveTrackColor = StudioSurfaceHighest
            )
        )
    }
}

@Composable
fun KelvinSliderRow(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            ValueBadge(
                text = "${value}K",
                accentColor = if (value < 5200) CyanTeal else AmberGold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CyanTeal,
                            Color(0xFFE5E7EB),
                            AmberGold
                        )
                    )
                )
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val rounded = ((raw / 50f).roundToInt() * 50).coerceIn(2000, 12000)
                onValueChange(rounded)
            },
            valueRange = 2000f..12000f,
            colors = SliderDefaults.colors(
                thumbColor = if (value < 5200) CyanTeal else AmberGold,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = StudioSurfaceHighest.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun HueSpectrumRow(
    label: String,
    hue: Int,
    saturation: Int,
    onHueChange: (Int) -> Unit
) {
    val previewColor = Color.hsv(
        hue = hue.coerceIn(0, 360).toFloat(),
        saturation = (saturation / 100f).coerceIn(0.25f, 1f),
        value = 0.92f
    )
    val rainbow = listOf(
        Color.hsv(0f, 0.85f, 0.95f),
        Color.hsv(60f, 0.85f, 0.95f),
        Color.hsv(120f, 0.85f, 0.95f),
        Color.hsv(180f, 0.85f, 0.95f),
        Color.hsv(240f, 0.85f, 0.95f),
        Color.hsv(300f, 0.85f, 0.95f),
        Color.hsv(360f, 0.85f, 0.95f)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(1.dp, StudioBorder, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            ValueBadge(text = "${hue}°", accentColor = previewColor)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(rainbow))
        )

        Slider(
            value = hue.toFloat(),
            onValueChange = { onHueChange(it.roundToInt().coerceIn(0, 360)) },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = previewColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = StudioSurfaceHighest.copy(alpha = 0.45f)
            )
        )
    }
}

@Composable
private fun ValueBadge(
    text: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = StudioSurfaceHighest
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (accentColor == TextSecondary) TextSecondary else accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
