package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ColorGradingProfile
import com.example.model.HistogramData
import com.example.ui.components.CollapsibleSectionCard
import com.example.ui.components.FloatExposureRow
import com.example.ui.components.HistogramChartCard
import com.example.ui.components.HueSpectrumRow
import com.example.ui.components.IntSliderRow
import com.example.ui.components.KelvinSliderRow
import com.example.ui.components.ThreeWayColorWheels
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldOnDark
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.GradeScanScreenState
import com.example.ui.viewmodel.HistogramChannel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: GradeScanScreenState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onNewPhotoClick: () -> Unit,
    onRetryClick: () -> Unit,
    onResetAiClick: () -> Unit,
    onChannelSelected: (HistogramChannel) -> Unit,
    onUpdateProfile: ((ColorGradingProfile) -> ColorGradingProfile) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successState = state.analysisState as? AnalysisUiState.Success
    val hasManualEdits = successState != null && successState.activeProfile != successState.aiProfile

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.results_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to home",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (hasManualEdits) {
                        IconButton(
                            onClick = onResetAiClick,
                            modifier = Modifier.testTag("reset_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset sliders to AI values",
                                tint = AmberGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onNewPhotoClick,
                        modifier = Modifier.testTag("new_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Pick another photo",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = StudioBackground
                )
            )
        },
        bottomBar = {
            if (successState != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = StudioSurface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = StudioBorder, thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onExportClick,
                                enabled = !state.isExporting,
                                modifier = Modifier
                                    .widthIn(max = 580.dp)
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .testTag("export_xmp_button"),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = AmberGoldOnDark,
                                    disabledContainerColor = AmberGold.copy(alpha = 0.45f)
                                )
                            ) {
                                if (state.isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = AmberGoldOnDark,
                                        strokeWidth = 2.4.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Generating .XMP…",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.IosShare,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.export_xmp_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val analysis = state.analysisState) {
                is AnalysisUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AmberGold)
                    }
                }

                is AnalysisUiState.Analyzing -> {
                    AnalyzingContent(
                        bitmap = analysis.bitmap,
                        histogram = analysis.histogram,
                        selectedChannel = state.selectedHistogramChannel,
                        onChannelSelected = onChannelSelected
                    )
                }

                is AnalysisUiState.Error -> {
                    ErrorContent(
                        message = analysis.message,
                        bitmap = analysis.bitmap,
                        onRetryClick = onRetryClick,
                        onNewPhotoClick = onNewPhotoClick
                    )
                }

                is AnalysisUiState.Success -> {
                    SuccessContent(
                        bitmap = analysis.bitmap,
                        histogram = analysis.histogram,
                        profile = analysis.activeProfile,
                        selectedChannel = state.selectedHistogramChannel,
                        onChannelSelected = onChannelSelected,
                        onUpdateProfile = onUpdateProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzingContent(
    bitmap: Bitmap?,
    histogram: HistogramData?,
    selectedChannel: HistogramChannel,
    onChannelSelected: (HistogramChannel) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanBeam")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamProgress"
    )
    var previewHeightPx by rememberSaveable { mutableStateOf(0) }
    val density = LocalDensity.current

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (bitmap != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = Color(0xFF0A0A0D),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .onSizeChanged { previewHeightPx = it.height },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Reference photo being scanned",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (previewHeightPx > 0) {
                            val beamHeightPx = with(density) { 4.dp.toPx() }
                            val yOffset = ((previewHeightPx - beamHeightPx) * beamProgress).roundToInt()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset { IntOffset(0, yOffset) }
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                AmberGold,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = StudioSurface,
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.analyzing_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = stringResource(R.string.analyzing_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AmberGold,
                        trackColor = StudioSurfaceElevated
                    )
                }
            }
        }

        if (histogram != null) {
            item {
                HistogramChartCard(
                    histogram = histogram,
                    selectedChannel = selectedChannel,
                    onChannelSelected = onChannelSelected
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    bitmap: Bitmap?,
    onRetryClick: () -> Unit,
    onNewPhotoClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (bitmap != null) {
            item {
                PhotoPreviewCard(
                    bitmap = bitmap,
                    presetName = "Reference Frame"
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ErrorContainerDark,
                border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorCoral
                        )
                        Text(
                            text = "Analysis Interrupted",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRetryClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("retry_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = AmberGoldOnDark
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.retry_action))
                        }
                        OutlinedButton(
                            onClick = onNewPhotoClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Text(
                                text = stringResource(R.string.new_photo_action),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    bitmap: Bitmap,
    histogram: HistogramData,
    profile: ColorGradingProfile,
    selectedChannel: HistogramChannel,
    onChannelSelected: (HistogramChannel) -> Unit,
    onUpdateProfile: ((ColorGradingProfile) -> ColorGradingProfile) -> Unit
) {
    var basicToneExpanded by rememberSaveable { mutableStateOf(true) }
    var colorExpanded by rememberSaveable { mutableStateOf(false) }
    var colorGradingExpanded by rememberSaveable { mutableStateOf(false) }
    var effectsExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Uploaded Image Preview
        item {
            PhotoPreviewCard(
                bitmap = bitmap,
                presetName = profile.presetName
            )
        }

        // 2. Live Color Histogram
        item {
            HistogramChartCard(
                histogram = histogram,
                selectedChannel = selectedChannel,
                onChannelSelected = onChannelSelected
            )
        }

        // 3. Mood Summary Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = AmberGold.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.38f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.mood_summary_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold
                        )
                    }
                    Text(
                        text = profile.moodSummary,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    if (profile.moodNotes.isNotBlank()) {
                        Text(
                            text = profile.moodNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 4. Collapsible Card: Basic Tone
        item {
            val expSummary = String.format(Locale.US, "%+.2f EV", profile.exposure)
            CollapsibleSectionCard(
                title = "Basic Tone",
                summaryBadge = "$expSummary · Contrast ${formatSigned(profile.contrast)} · Shadows ${formatSigned(profile.shadows)}",
                icon = Icons.Rounded.Exposure,
                expanded = basicToneExpanded,
                onToggle = { basicToneExpanded = !basicToneExpanded },
                testTag = "section_basic_tone"
            ) {
                FloatExposureRow(
                    label = "Exposure",
                    value = profile.exposure,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(exposure = newVal) } }
                )
                IntSliderRow(
                    label = "Contrast",
                    value = profile.contrast,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(contrast = newVal) } }
                )
                IntSliderRow(
                    label = "Highlights",
                    value = profile.highlights,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(highlights = newVal) } }
                )
                IntSliderRow(
                    label = "Shadows",
                    value = profile.shadows,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(shadows = newVal) } }
                )
                IntSliderRow(
                    label = "Whites",
                    value = profile.whites,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(whites = newVal) } }
                )
                IntSliderRow(
                    label = "Blacks",
                    value = profile.blacks,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(blacks = newVal) } }
                )
            }
        }

        // 5. Collapsible Card: Color
        item {
            CollapsibleSectionCard(
                title = "Color",
                summaryBadge = "${profile.temperature}K · Tint ${formatSigned(profile.tint)} · Vib ${formatSigned(profile.vibrance)}",
                icon = Icons.Rounded.ColorLens,
                expanded = colorExpanded,
                onToggle = { colorExpanded = !colorExpanded },
                testTag = "section_color"
            ) {
                KelvinSliderRow(
                    value = profile.temperature,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(temperature = newVal) } }
                )
                IntSliderRow(
                    label = "Tint",
                    value = profile.tint,
                    valueRange = -150..150,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(tint = newVal) } }
                )
                IntSliderRow(
                    label = "Vibrance",
                    value = profile.vibrance,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(vibrance = newVal) } }
                )
                IntSliderRow(
                    label = "Saturation",
                    value = profile.saturation,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(saturation = newVal) } }
                )
            }
        }

        // 6. Collapsible Card: Color Grading (3-Way)
        item {
            CollapsibleSectionCard(
                title = "Color Grading (3-Way)",
                summaryBadge = "S ${profile.shadowsHue}°/${profile.shadowsSaturation}% · M ${profile.midtonesHue}°/${profile.midtonesSaturation}% · H ${profile.highlightsHue}°/${profile.highlightsSaturation}%",
                icon = Icons.Rounded.BlurOn,
                expanded = colorGradingExpanded,
                onToggle = { colorGradingExpanded = !colorGradingExpanded },
                testTag = "section_color_grading"
            ) {
                ThreeWayColorWheels(
                    shadowsHue = profile.shadowsHue,
                    shadowsSat = profile.shadowsSaturation,
                    midtonesHue = profile.midtonesHue,
                    midtonesSat = profile.midtonesSaturation,
                    highlightsHue = profile.highlightsHue,
                    highlightsSat = profile.highlightsSaturation,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(color = StudioBorderSubtle, thickness = 1.dp)

                HueSpectrumRow(
                    label = "Highlights Hue",
                    hue = profile.highlightsHue,
                    saturation = profile.highlightsSaturation,
                    onHueChange = { newVal -> onUpdateProfile { it.copy(highlightsHue = newVal) } }
                )
                IntSliderRow(
                    label = "Highlights Saturation",
                    value = profile.highlightsSaturation,
                    valueRange = 0..100,
                    unitSuffix = "%",
                    showSigned = false,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(highlightsSaturation = newVal) } }
                )

                HorizontalDivider(color = StudioBorderSubtle, thickness = 1.dp)

                HueSpectrumRow(
                    label = "Midtones Hue",
                    hue = profile.midtonesHue,
                    saturation = profile.midtonesSaturation,
                    onHueChange = { newVal -> onUpdateProfile { it.copy(midtonesHue = newVal) } }
                )
                IntSliderRow(
                    label = "Midtones Saturation",
                    value = profile.midtonesSaturation,
                    valueRange = 0..100,
                    unitSuffix = "%",
                    showSigned = false,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(midtonesSaturation = newVal) } }
                )

                HorizontalDivider(color = StudioBorderSubtle, thickness = 1.dp)

                HueSpectrumRow(
                    label = "Shadows Hue",
                    hue = profile.shadowsHue,
                    saturation = profile.shadowsSaturation,
                    onHueChange = { newVal -> onUpdateProfile { it.copy(shadowsHue = newVal) } }
                )
                IntSliderRow(
                    label = "Shadows Saturation",
                    value = profile.shadowsSaturation,
                    valueRange = 0..100,
                    unitSuffix = "%",
                    showSigned = false,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(shadowsSaturation = newVal) } }
                )
            }
        }

        // 7. Collapsible Card: Effects
        item {
            CollapsibleSectionCard(
                title = "Effects",
                summaryBadge = "Tex ${formatSigned(profile.texture)} · Clar ${formatSigned(profile.clarity)} · Grain ${profile.grain}",
                icon = Icons.Rounded.Grain,
                expanded = effectsExpanded,
                onToggle = { effectsExpanded = !effectsExpanded },
                testTag = "section_effects"
            ) {
                IntSliderRow(
                    label = "Texture",
                    value = profile.texture,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(texture = newVal) } }
                )
                IntSliderRow(
                    label = "Clarity",
                    value = profile.clarity,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(clarity = newVal) } }
                )
                IntSliderRow(
                    label = "Dehaze",
                    value = profile.dehaze,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(dehaze = newVal) } }
                )
                IntSliderRow(
                    label = "Vignette",
                    value = profile.vignette,
                    valueRange = -100..100,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(vignette = newVal) } }
                )
                IntSliderRow(
                    label = "Grain",
                    value = profile.grain,
                    valueRange = 0..100,
                    showSigned = false,
                    onValueChange = { newVal -> onUpdateProfile { it.copy(grain = newVal) } }
                )
            }
        }
    }
}

@Composable
private fun PhotoPreviewCard(
    bitmap: Bitmap,
    presetName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF0A0A0D),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Uploaded reference photo preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = presetName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioBackground.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "${bitmap.width}×${bitmap.height}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSigned(value: Int): String = if (value > 0) "+$value" else value.toString()
