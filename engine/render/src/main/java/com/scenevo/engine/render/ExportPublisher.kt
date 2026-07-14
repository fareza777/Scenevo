package com.scenevo.engine.render

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PublishedExport(
    val galleryUri: Uri?,
    val shareUri: Uri,
    val displayName: String,
)

@Singleton
class ExportPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun publish(outputPath: String): PublishedExport = withContext(Dispatchers.IO) {
        val file = File(outputPath)
        require(file.exists()) { "Export file missing: $outputPath" }

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val galleryUri = runCatching { copyToMovies(file) }
            .onFailure { Timber.e(it, "Gallery publish failed") }
            .getOrNull()

        PublishedExport(
            galleryUri = galleryUri,
            shareUri = shareUri,
            displayName = file.name,
        )
    }

    fun shareIntent(shareUri: Uri, displayName: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun copyToMovies(file: File): Uri {
        val resolver = context.contentResolver
        val name = file.name
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Scenevo")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values)
            ?: error("MediaStore insert returned null")

        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Cannot open MediaStore output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
