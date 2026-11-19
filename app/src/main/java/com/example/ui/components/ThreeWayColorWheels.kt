package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThreeWayColorWheels(
    shadowsHue: Int,
    shadowsSat: Int,
    midtonesHue: Int,
    midtonesSat: Int,
    highlightsHue: Int,
    highlightsSat: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleColorWheel(label = "Shadows", hue = shadowsHue, saturation = shadowsSat)
        SingleColorWheel(label = "Midtones", hue = midtonesHue, saturation = midtonesSat)
        SingleColorWheel(label = "Highlights", hue = highlightsHue, saturation = highlightsSat)
    }
}

@Composable
private fun SingleColorWheel(
    label: String,
    hue: Int,
    saturation: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(76.dp)) {
            val radius = size.minDimension / 2f - 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer hue ring
            val hueSweep = listOf(
                Color.hsv(0f, 0.85f, 0.95f),
                Color.hsv(60f, 0.85f, 0.95f),
                Color.hsv(120f, 0.85f, 0.95f),
                Color.hsv(180f, 0.85f, 0.95f),
                Color.hsv(240f, 0.85f, 0.95f),
                Color.hsv(300f, 0.85f, 0.95f),
                Color.hsv(360f, 0.85f, 0.95f)
            )
            drawCircle(
                color = StudioBackground,
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.sweepGradient(hueSweep, center = center),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = StudioBorder,
                radius = radius - 3.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crosshair
            drawLine(
                color = StudioBorder.copy(alpha = 0.7f),
                start = Offset(center.x - radius + 5.dp.toPx(), center.y),
                end = Offset(center.x + radius - 5.dp.toPx(), center.y),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = StudioBorder.copy(alpha = 0.7f),
                start = Offset(center.x, center.y - radius + 5.dp.toPx()),
                end = Offset(center.x, center.y + radius - 5.dp.toPx()),
                strokeWidth = 0.8.dp.toPx()
            )

            // Target dot positioned by hue angle & saturation magnitude
            val radians = (hue.toFloat() * PI / 180.0)
            val usableRadius = radius - 6.dp.toPx()
            val normSat = (saturation.coerceIn(0, 100) / 100f)
            val dotX = center.x + (cos(radians) * usableRadius * normSat).toFloat()
            val dotY = center.y + (sin(radians) * usableRadius * normSat).toFloat()
            val dotColor = Color.hsv(
                hue = hue.coerceIn(0, 360).toFloat(),
                saturation = normSat.coerceAtLeast(0.15f),
                value = 0.98f
            )

            drawLine(
                color = dotColor.copy(alpha = 0.6f),
                start = center,
                end = Offset(dotX, dotY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = dotColor,
                radius = 5.dp.toPx(),
                center = Offset(dotX, dotY)
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(dotX, dotY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = "${hue}° · ${saturation}%",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}
