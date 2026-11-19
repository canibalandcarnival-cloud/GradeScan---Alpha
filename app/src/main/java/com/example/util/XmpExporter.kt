package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ColorGradingProfile
import java.io.File

data class XmpExportResult(
    val fileName: String,
    val downloadsDisplayLocation: String,
    val shareUri: Uri
)

object XmpExporter {

    fun exportPreset(context: Context, profile: ColorGradingProfile): XmpExportResult {
        val fileName = XmpGenerator.buildPresetFileName(profile)
        val xmpContent = XmpGenerator.buildXmpContent(profile)
        val bytes = xmpContent.toByteArray(Charsets.UTF_8)

        val displayLocation = saveToDownloads(context, fileName, bytes)
        val shareUri = saveShareableCopy(context, fileName, bytes)

        return XmpExportResult(
            fileName = fileName,
            downloadsDisplayLocation = displayLocation,
            shareUri = shareUri
        )
    }

    fun buildShareIntent(context: Context, exportResult: XmpExportResult, presetName: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, exportResult.shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "$presetName (.xmp Lightroom Preset)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Lightroom preset '$presetName' extracted with GradeScan. Open in Adobe Lightroom Mobile to apply."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Send $presetName to Lightroom").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun saveToDownloads(context: Context, fileName: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/rdf+xml")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/GradeScan"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Could not create preset file in Downloads.")
            resolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: throw IllegalStateException("Could not write preset file to Downloads.")

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            return "Downloads/GradeScan/$fileName"
        } else {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads").apply { mkdirs() }
            val outFile = File(downloadsDir, fileName)
            outFile.writeBytes(bytes)
            return outFile.name
        }
    }

    private fun saveShareableCopy(context: Context, fileName: String, bytes: ByteArray): Uri {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareFile = File(exportsDir, fileName)
        shareFile.writeBytes(bytes)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
    }
}
