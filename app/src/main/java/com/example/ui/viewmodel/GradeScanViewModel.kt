package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ColorGradingAnalyzer
import com.example.model.ColorGradingProfile
import com.example.model.HistogramData
import com.example.util.ImageProcessor
import com.example.util.XmpExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GradeScanViewModel"

enum class HistogramChannel(val label: String) {
    RGB("RGB"),
    LUMA("Luma"),
    RED("R"),
    GREEN("G"),
    BLUE("B")
}

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Analyzing(
        val bitmap: Bitmap?,
        val histogram: HistogramData?
    ) : AnalysisUiState

    data class Success(
        val bitmap: Bitmap,
        val histogram: HistogramData,
        val aiProfile: ColorGradingProfile,
        val activeProfile: ColorGradingProfile
    ) : AnalysisUiState

    data class Error(
        val message: String,
        val bitmap: Bitmap?,
        val histogram: HistogramData?
    ) : AnalysisUiState
}

data class GradeScanScreenState(
    val selectedImageUri: Uri? = null,
    val analysisState: AnalysisUiState = AnalysisUiState.Idle,
    val selectedHistogramChannel: HistogramChannel = HistogramChannel.RGB,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val pendingShareIntent: Intent? = null
)

class GradeScanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val analyzer = ColorGradingAnalyzer()

    private val _uiState = MutableStateFlow(GradeScanScreenState())
    val uiState: StateFlow<GradeScanScreenState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                analysisState = AnalysisUiState.Analyzing(bitmap = null, histogram = null),
                exportMessage = null,
                pendingShareIntent = null
            )
        }
        runAnalysis(uri)
    }

    fun retryAnalysis() {
        val uri = _uiState.value.selectedImageUri ?: return
        runAnalysis(uri)
    }

    private fun runAnalysis(uri: Uri) {
        viewModelScope.launch {
            var decodedBitmap: Bitmap? = null
            var computedHistogram: HistogramData? = null
            try {
                val context = getApplication<Application>()
                decodedBitmap = withContext(Dispatchers.IO) {
                    ImageProcessor.decodeScaledBitmap(context, uri, maxDimension = 1024)
                }

                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Analyzing(
                            bitmap = decodedBitmap,
                            histogram = null
                        )
                    )
                }

                val profile = coroutineScope {
                    val histogramDeferred = async(Dispatchers.Default) {
                        ImageProcessor.computeHistogram(decodedBitmap).also { hist ->
                            computedHistogram = hist
                            _uiState.update { current ->
                                val analyzing = current.analysisState as? AnalysisUiState.Analyzing
                                if (analyzing != null) {
                                    current.copy(analysisState = analyzing.copy(histogram = hist))
                                } else {
                                    current
                                }
                            }
                        }
                    }
                    val profileDeferred = async {
                        analyzer.analyzeImage(decodedBitmap)
                    }
                    histogramDeferred.await()
                    profileDeferred.await()
                }

                val finalHistogram = computedHistogram
                    ?: ImageProcessor.computeHistogram(decodedBitmap)

                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Success(
                            bitmap = decodedBitmap,
                            histogram = finalHistogram,
                            aiProfile = profile,
                            activeProfile = profile
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Color analysis timed out", e)
                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Error(
                            message = "Analysis timed out after 60 seconds. Check your connection and retry.",
                            bitmap = decodedBitmap,
                            histogram = computedHistogram
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Color analysis failed", e)
                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Error(
                            message = e.localizedMessage ?: "Failed to analyze color grading.",
                            bitmap = decodedBitmap,
                            histogram = computedHistogram
                        )
                    )
                }
            }
        }
    }

    fun selectHistogramChannel(channel: HistogramChannel) {
        _uiState.update { it.copy(selectedHistogramChannel = channel) }
    }

    fun updateActiveProfile(transform: (ColorGradingProfile) -> ColorGradingProfile) {
        _uiState.update { current ->
            val success = current.analysisState as? AnalysisUiState.Success ?: return@update current
            current.copy(
                analysisState = success.copy(
                    activeProfile = transform(success.activeProfile)
                )
            )
        }
    }

    fun resetToAiValues() {
        _uiState.update { current ->
            val success = current.analysisState as? AnalysisUiState.Success ?: return@update current
            current.copy(
                analysisState = success.copy(activeProfile = success.aiProfile)
            )
        }
    }

    fun exportXmpPreset() {
        val success = _uiState.value.analysisState as? AnalysisUiState.Success ?: return
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            try {
                val context = getApplication<Application>()
                val exportResult = withContext(Dispatchers.IO) {
                    XmpExporter.exportPreset(context, success.activeProfile)
                }
                val shareIntent = XmpExporter.buildShareIntent(
                    context = context,
                    exportResult = exportResult,
                    presetName = success.activeProfile.presetName
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "Saved to ${exportResult.downloadsDisplayLocation}",
                        pendingShareIntent = shareIntent
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export XMP preset", e)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = e.localizedMessage ?: "Failed to save .xmp preset."
                    )
                }
            }
        }
    }

    fun consumeShareIntent() {
        _uiState.update { it.copy(pendingShareIntent = null) }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    fun resetSession() {
        _uiState.value = GradeScanScreenState()
    }
}
