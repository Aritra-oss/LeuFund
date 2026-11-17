package com.example.domain

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

data class PdfSaveOutcome(
  val displayUri: Uri?,
  val shareUri: Uri,
  val savedToDownloads: Boolean,
)

object PdfExportHelper {

  private const val CACHE_DIR_NAME = "exports"
  private const val DOWNLOADS_SUBDIR = "LeuFunds"
  private const val PDF_MIME_TYPE = "application/pdf"

  fun savePdfToCacheAndDownloads(
    context: Context,
    fileName: String,
    pdfBytes: ByteArray,
  ): PdfSaveOutcome {
    require(pdfBytes.isNotEmpty()) { "PDF payload must not be empty" }

    val exportDir = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    val cacheFile = File(exportDir, fileName)
    cacheFile.outputStream().use { stream ->
      stream.write(pdfBytes)
      stream.flush()
    }

    val shareUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

    val downloadsUri =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToMediaStoreDownloads(context = context, fileName = fileName, pdfBytes = pdfBytes)
      } else {
        null
      }

    return PdfSaveOutcome(
      displayUri = downloadsUri,
      shareUri = shareUri,
      savedToDownloads = downloadsUri != null,
    )
  }

  private fun saveToMediaStoreDownloads(
    context: Context,
    fileName: String,
    pdfBytes: ByteArray,
  ): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val resolver = context.contentResolver
    val values =
      ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, PDF_MIME_TYPE)
        put(
          MediaStore.Downloads.RELATIVE_PATH,
          "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBDIR",
        )
        put(MediaStore.Downloads.IS_PENDING, 1)
      }

    val itemUri =
      runCatching { resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) }
        .getOrNull() ?: return null

    return try {
      val outStream =
        resolver.openOutputStream(itemUri) ?: error("Could not open Downloads output stream")
      outStream.use { stream ->
        stream.write(pdfBytes)
        stream.flush()
      }
      val doneValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
      resolver.update(itemUri, doneValues, null, null)
      itemUri
    } catch (_: Throwable) {
      runCatching { resolver.delete(itemUri, null, null) }
      null
    }
  }

  fun openOrSharePdf(context: Context, uri: Uri, title: String): Boolean {
    val viewIntent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, PDF_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val openedViewer =
      try {
        context.startActivity(viewIntent)
        true
      } catch (_: ActivityNotFoundException) {
        false
      } catch (_: Throwable) {
        false
      }
    if (openedViewer) return true

    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = PDF_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(shareIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return runCatching { context.startActivity(chooser) }.isSuccess
  }
}
