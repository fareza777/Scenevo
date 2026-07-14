package com.scenevo.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import timber.log.Timber
import java.io.File
import java.util.UUID

/**
 * Copies gallery/SAF content into app-private storage so export & preview
 * keep working after process death (no stale content:// permission).
 */
object LocalMediaCache {

    data class CachedMedia(
        val fileUri: String,
        val mimeType: String,
        val isVideo: Boolean,
    )

    fun import(context: Context, uriString: String): CachedMedia? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        val isVideo = mime.startsWith("video/") ||
            (!mime.startsWith("audio/") && uriString.contains("video", ignoreCase = true))
        val isAudio = mime.startsWith("audio/")

        val dir = File(context.filesDir, "media_cache").apply { mkdirs() }

        if (!isVideo && !isAudio && (mime.contains("heic", true) || mime.contains("heif", true))) {
            return decodeToJpeg(context, uri, dir)
        }

        val ext = extensionFor(mime, isVideo, isAudio)
        val out = File(dir, "local_${UUID.randomUUID()}.$ext")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (!out.exists() || out.length() == 0L) {
                out.delete()
                return null
            }
            CachedMedia(
                fileUri = MediaUris.fileUri(out),
                mimeType = mime.ifBlank {
                    when {
                        isVideo -> "video/mp4"
                        isAudio -> "audio/mpeg"
                        else -> "image/jpeg"
                    }
                },
                isVideo = isVideo,
            )
        }.onFailure {
            Timber.e(it, "LocalMediaCache import failed for $uriString")
            out.delete()
        }.getOrNull()
    }

    private fun decodeToJpeg(context: Context, uri: Uri, dir: File): CachedMedia? {
        val out = File(dir, "local_${UUID.randomUUID()}.jpg")
        return runCatching {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null
            out.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
            bitmap.recycle()
            CachedMedia(
                fileUri = MediaUris.fileUri(out),
                mimeType = "image/jpeg",
                isVideo = false,
            )
        }.onFailure {
            Timber.e(it, "HEIC→JPEG failed")
            out.delete()
        }.getOrNull()
    }

    private fun extensionFor(mime: String, isVideo: Boolean, isAudio: Boolean): String = when {
        isAudio && (mime.contains("mpeg", true) || mime.contains("mp3", true)) -> "mp3"
        isAudio && mime.contains("wav", true) -> "wav"
        isAudio && (mime.contains("aac", true) || mime.contains("mp4", true) || mime.contains("m4a", true)) -> "m4a"
        isAudio -> "mp3"
        mime.contains("png", true) -> "png"
        mime.contains("webp", true) -> "webp"
        mime.contains("jpeg", true) || mime.contains("jpg", true) -> "jpg"
        mime.contains("mp4", true) || mime.contains("m4v", true) -> "mp4"
        mime.contains("webm", true) -> "webm"
        mime.contains("3gpp", true) || mime.contains("3gp", true) -> "3gp"
        isVideo -> "mp4"
        else -> "jpg"
    }
}
