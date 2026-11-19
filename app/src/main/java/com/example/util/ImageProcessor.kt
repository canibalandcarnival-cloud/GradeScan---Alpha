package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.model.HistogramData
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageProcessor {

    fun createCameraCaptureUri(context: Context): Uri {
        val capturesDir = File(context.cacheDir, "captures").apply { mkdirs() }
        val photoFile = File(capturesDir, "gradescan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1280): Bitmap {
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val largest = max(width, height)
                if (largest > maxDimension) {
                    val scale = maxDimension.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            decodeLegacyScaledBitmap(context, uri, maxDimension)
        }

        // Guarantee ARGB_8888 software bitmap for pixel histogram sampling and Gemini inline transfer
        return if (rawBitmap.config != Bitmap.Config.ARGB_8888) {
            rawBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            rawBitmap
        }
    }

    private fun decodeLegacyScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDimension * 2 ||
            boundsOptions.outHeight / sampleSize > maxDimension * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Unable to decode selected image.")

        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val rotated = applyExifOrientation(decoded, orientation)
        val largest = max(rotated.width, rotated.height)
        if (largest <= maxDimension) return rotated

        val scale = maxDimension.toFloat() / largest.toFloat()
        val targetW = (rotated.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (rotated.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun computeHistogram(bitmap: Bitmap, binCount: Int = 64): HistogramData {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Stride sampling keeps computation under ~10ms while remaining statistically accurate
        val stride = max(1, totalPixels / 80_000)
        val redRaw = FloatArray(binCount)
        val greenRaw = FloatArray(binCount)
        val blueRaw = FloatArray(binCount)
        val lumaRaw = FloatArray(binCount)

        var shadowCount = 0
        var midtoneCount = 0
        var highlightCount = 0
        var sampledCount = 0

        var idx = 0
        while (idx < totalPixels) {
            val argb = pixels[idx]
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).roundToInt().coerceIn(0, 255)

            redRaw[(r * binCount / 256).coerceIn(0, binCount - 1)] += 1f
            greenRaw[(g * binCount / 256).coerceIn(0, binCount - 1)] += 1f
            blueRaw[(b * binCount / 256).coerceIn(0, binCount - 1)] += 1f
            lumaRaw[(luma * binCount / 256).coerceIn(0, binCount - 1)] += 1f

            when {
                luma < 72 -> shadowCount++
                luma <= 185 -> midtoneCount++
                else -> highlightCount++
            }
            sampledCount++
            idx += stride
        }

        val redSmoothed = smoothBins(redRaw)
        val greenSmoothed = smoothBins(greenRaw)
        val blueSmoothed = smoothBins(blueRaw)
        val lumaSmoothed = smoothBins(lumaRaw)

        // Normalize by 98th percentile across channels so pure black/white borders don't crush curves
        val allValues = (redSmoothed + greenSmoothed + blueSmoothed + lumaSmoothed).sorted()
        val refMax = allValues.getOrElse((allValues.size * 0.985f).toInt().coerceAtMost(allValues.lastIndex)) { 1f }
            .coerceAtLeast(1f)

        val safeTotal = max(1, sampledCount)
        val shadowPct = (shadowCount * 100f / safeTotal).roundToInt()
        val highlightPct = (highlightCount * 100f / safeTotal).roundToInt()
        val midtonePct = (100 - shadowPct - highlightPct).coerceAtLeast(0)

        return HistogramData(
            redBins = redSmoothed.map { min(1f, it / refMax) },
            greenBins = greenSmoothed.map { min(1f, it / refMax) },
            blueBins = blueSmoothed.map { min(1f, it / refMax) },
            lumaBins = lumaSmoothed.map { min(1f, it / refMax) },
            shadowSharePercent = shadowPct,
            midtoneSharePercent = midtonePct,
            highlightSharePercent = highlightPct
        )
    }

    private fun smoothBins(bins: FloatArray): List<Float> {
        val result = MutableList(bins.size) { 0f }
        for (i in bins.indices) {
            val prev = bins[(i - 1).coerceAtLeast(0)]
            val curr = bins[i]
            val next = bins[(i + 1).coerceAtMost(bins.lastIndex)]
            result[i] = (prev * 0.2f + curr * 0.6f + next * 0.2f)
        }
        return result
    }
}
