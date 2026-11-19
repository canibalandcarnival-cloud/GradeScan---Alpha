package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HistogramData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.HistogramBlue
import com.example.ui.theme.HistogramGreen
import com.example.ui.theme.HistogramLuma
import com.example.ui.theme.HistogramRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HistogramChannel

@Composable
fun HistogramChartCard(
    histogram: HistogramData,
    selectedChannel: HistogramChannel,
    onChannelSelected: (HistogramChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.histogram_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistogramChannel.entries.forEach { channel ->
                        val selected = channel == selectedChannel
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    role = Role.Tab,
                                    onClickLabel = "Show ${channel.label} histogram",
                                    onClick = { onChannelSelected(channel) }
                                )
                                .testTag("histogram_channel_${channel.name.lowercase()}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) AmberGold.copy(alpha = 0.18f) else StudioSurfaceHighest,
                            border = BorderStroke(
                                1.dp,
                                if (selected) AmberGold.copy(alpha = 0.7f) else Color.Transparent
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channel.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) AmberGold else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioBackground)
                    .padding(top = 8.dp, bottom = 2.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Subtle tonal quarter-grid lines
                    val gridSteps = listOf(0.25f, 0.5f, 0.75f)
                    gridSteps.forEach { fraction ->
                        val x = size.width * fraction
                        drawLine(
                            color = StudioBorderSubtle,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    when (selectedChannel) {
                        HistogramChannel.RGB -> {
                            drawHistogramChannel(histogram.redBins, HistogramRed, fillAlpha = 0.20f)
                            drawHistogramChannel(histogram.greenBins, HistogramGreen, fillAlpha = 0.18f)
                            drawHistogramChannel(histogram.blueBins, HistogramBlue, fillAlpha = 0.22f)
                        }
                        HistogramChannel.LUMA -> {
                            drawHistogramChannel(histogram.lumaBins, HistogramLuma, fillAlpha = 0.34f)
                        }
                        HistogramChannel.RED -> {
                            drawHistogramChannel(histogram.redBins, HistogramRed, fillAlpha = 0.34f)
                        }
                        HistogramChannel.GREEN -> {
                            drawHistogramChannel(histogram.greenBins, HistogramGreen, fillAlpha = 0.34f)
                        }
                        HistogramChannel.BLUE -> {
                            drawHistogramChannel(histogram.blueBins, HistogramBlue, fillAlpha = 0.34f)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryChip(label = "SHDW", value = "${histogram.shadowSharePercent}%")
                TelemetryChip(label = "MID", value = "${histogram.midtoneSharePercent}%")
                TelemetryChip(label = "HIGH", value = "${histogram.highlightSharePercent}%")
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}

private fun DrawScope.drawHistogramChannel(
    bins: List<Float>,
    color: Color,
    fillAlpha: Float
) {
    if (bins.size < 2) return

    val width = size.width
    val height = size.height
    val stepX = width / (bins.size - 1).toFloat()

    val strokePath = Path()
    val fillPath = Path()

    bins.forEachIndexed { index, normalized ->
        val x = index * stepX
        val y = height - (normalized.coerceIn(0f, 1f) * (height - 4f))
        if (index == 0) {
            strokePath.moveTo(x, y)
            fillPath.moveTo(x, height)
            fillPath.lineTo(x, y)
        } else {
            val prevX = (index - 1) * stepX
            val prevY = height - (bins[index - 1].coerceIn(0f, 1f) * (height - 4f))
            val midX = (prevX + x) / 2f
            strokePath.cubicTo(midX, prevY, midX, y, x, y)
            fillPath.cubicTo(midX, prevY, midX, y, x, y)
        }
    }

    fillPath.lineTo(width, height)
    fillPath.close()

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = fillAlpha),
                color.copy(alpha = 0.02f)
            )
        )
    )
    drawPath(
        path = strokePath,
        color = color.copy(alpha = 0.9f),
        style = Stroke(
            width = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
