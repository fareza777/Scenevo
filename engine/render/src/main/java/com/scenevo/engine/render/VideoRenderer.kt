package com.scenevo.engine.render

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.scenevo.core.common.MediaUris
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.AudioKind
import com.scenevo.domain.model.ExportResolution
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.RenderJob
import com.scenevo.domain.model.RenderStatus
import com.scenevo.domain.model.SubtitleCue
import com.scenevo.domain.model.TimelineClip
import com.scenevo.engine.timeline.TimelineComposer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class RenderProgress(
    val job: RenderJob,
    val message: String,
)

interface VideoRenderer {
    suspend fun render(
        project: Project,
        onProgress: (RenderProgress) -> Unit,
    ): RenderJob
}

@Singleton
class Media3VideoRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegBridge: FfmpegTransitionBridge,
) : VideoRenderer {

    @OptIn(UnstableApi::class)
    override suspend fun render(
        project: Project,
        onProgress: (RenderProgress) -> Unit,
    ): RenderJob {
        val jobId = UUID.randomUUID().toString()
        var job = RenderJob(
            id = jobId,
            projectId = project.id,
            status = RenderStatus.PREPARING,
            progress = 0.05f,
            startedAt = System.currentTimeMillis(),
        )
        onProgress(RenderProgress(job, "Menyiapkan timeline…"))

        val timeline = withContext(Dispatchers.IO) { TimelineComposer.compose(project) }
        if (timeline.clips.isEmpty()) {
            return fail(job, "Belum ada visual. Pasang foto/video di tiap scene dulu.", onProgress)
        }

        val unreadable = withContext(Dispatchers.IO) {
            timeline.clips.mapNotNull { clip ->
                if (canRead(clip.mediaUri)) null else clip.mediaUri
            }
        }
        if (unreadable.isNotEmpty()) {
            return fail(
                job,
                "File visual tidak bisa dibaca. Buat montage baru & pilih ulang dari galeri.",
                onProgress,
            )
        }

        val outDir = withContext(Dispatchers.IO) {
            File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        }
        val stamp = System.currentTimeMillis()
        val outFile = File(outDir, "scenevo_${project.id.take(8)}_$stamp.mp4")
        val srtFile = File(outDir, "scenevo_${project.id.take(8)}_$stamp.srt")

        if (project.subtitleStyle.enabled && timeline.subtitleCues.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                SrtWriter.write(timeline.subtitleCues, srtFile)
            }
        }

        job = job.copy(status = RenderStatus.RENDERING, progress = 0.15f)
        onProgress(RenderProgress(job, "Render di perangkat…"))

        val safeResolution = when (project.exportSettings.resolution) {
            ExportResolution.UHD_4K -> ExportResolution.HD_1080
            else -> project.exportSettings.resolution
        }
        val outputSize = resolveOutputSize(project.aspectRatio, safeResolution)
        val fps = project.exportSettings.fps.coerceIn(15, 60)

        val editedItems = withContext(Dispatchers.IO) {
            timeline.clips.map { clip ->
                buildVisualItem(clip, project, timeline.subtitleCues, outputSize, fps)
            }
        }

        val sequences = mutableListOf(
            EditedMediaItemSequence.Builder(editedItems).build(),
        )

        var hasAudio = false
        timeline.audioLayers.forEach { layer ->
            if (layer.kind == AudioKind.MUSIC && !project.exportSettings.includeMusic) return@forEach
            if (!canRead(layer.uri)) {
                Timber.w("Skipping unreadable audio layer ${layer.kind}: ${layer.uri}")
                return@forEach
            }
            val processors = mutableListOf<AudioProcessor>()
            if (layer.kind == AudioKind.MUSIC) {
                processors += GainAudioProcessor(layer.volume.coerceIn(0.05f, 1f))
            }
            val audioItem = EditedMediaItem.Builder(
                MediaItem.fromUri(MediaUris.parse(layer.uri)),
            )
                .setDurationUs((layer.endMs - layer.startMs).coerceAtLeast(500L) * 1000)
                .setEffects(Effects(processors, emptyList()))
                .build()
            sequences += EditedMediaItemSequence.Builder(listOf(audioItem)).build()
            hasAudio = true
        }

        // Required when composition starts with images / has no audio input.
        val compositionBuilder = Composition.Builder(sequences)
        if (!hasAudio || timeline.clips.any { it.mediaType == MediaType.IMAGE }) {
            compositionBuilder.experimentalSetForceAudioTrack(true)
        }
        val composition = compositionBuilder.build()

        job = job.copy(status = RenderStatus.MUXING, progress = 0.55f)
        onProgress(RenderProgress(job, "Menggabungkan audio & subtitle…"))

        val result = withContext(Dispatchers.Main.immediate) {
            runTransformer(composition, outFile.absolutePath)
        }
        return result.fold(
            onSuccess = {
                job = job.copy(progress = 0.9f)
                onProgress(RenderProgress(job, "Menyelesaikan export…"))
                val polished = withContext(Dispatchers.IO) {
                    runCatching {
                        ffmpegBridge.polish(outFile.absolutePath, project.id)
                    }.getOrDefault(outFile.absolutePath)
                }

                job.copy(
                    status = RenderStatus.COMPLETED,
                    progress = 1f,
                    outputUri = polished,
                    finishedAt = System.currentTimeMillis(),
                ).also {
                    onProgress(
                        RenderProgress(
                            it,
                            if (srtFile.exists()) "Export selesai (+ SRT)" else "Export selesai",
                        ),
                    )
                }
            },
            onFailure = { err ->
                Timber.e(err, "Render failed")
                fail(job, humanizeExportError(err), onProgress)
            },
        )
    }

    private fun buildVisualItem(
        clip: TimelineClip,
        project: Project,
        cues: List<SubtitleCue>,
        outputSize: Pair<Int, Int>,
        fps: Int,
    ): EditedMediaItem {
        val sceneDurationMs = (clip.endMs - clip.startMs).coerceAtLeast(500L)
        val uri = MediaUris.parse(clip.mediaUri)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // Transformer detects images via MIME or file extension — both must be correct.
        val mime = resolveMime(uri, clip.mediaType)
        mediaItemBuilder.setMimeType(mime)

        val durationMs = when (clip.mediaType) {
            MediaType.IMAGE -> {
                mediaItemBuilder.setImageDurationMs(sceneDurationMs)
                sceneDurationMs
            }
            MediaType.VIDEO -> {
                val sourceMs = probeDurationMs(uri) ?: sceneDurationMs
                val clipped = minOf(sceneDurationMs, sourceMs).coerceAtLeast(500L)
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(clipped)
                        .build(),
                )
                clipped
            }
            MediaType.AUDIO -> sceneDurationMs
        }

        val videoEffects = buildList {
            add(
                Presentation.createForWidthAndHeight(
                    outputSize.first,
                    outputSize.second,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
                ),
            )
            addAll(TransitionEffectFactory.clipEffects(clip.motion, clip.transition))
            val cueText = cues
                .firstOrNull { it.startMs == clip.startMs && it.endMs == clip.endMs }
                ?.text
                ?: project.scenes.firstOrNull { it.id == clip.sceneId }?.text
            SubtitleOverlayFactory.create(cueText.orEmpty(), project.subtitleStyle)?.let { add(it) }
        }

        val editedBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setDurationUs(durationMs * 1000)
            .setRemoveAudio(true)
            .setEffects(Effects(emptyList(), videoEffects))

        // CRITICAL for image→video: without frameRate, Transformer fails.
        if (clip.mediaType == MediaType.IMAGE) {
            editedBuilder.setFrameRate(fps)
        }

        return editedBuilder.build()
    }

    private fun resolveMime(uri: Uri, mediaType: MediaType): String {
        val fromResolver = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        if (!fromResolver.isNullOrBlank()) return fromResolver
        val path = (uri.path ?: uri.toString()).lowercase()
        return when {
            path.endsWith(".png") -> MimeTypes.IMAGE_PNG
            path.endsWith(".webp") -> MimeTypes.IMAGE_WEBP
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> MimeTypes.IMAGE_JPEG
            path.endsWith(".mp4") || path.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            path.endsWith(".3gp") -> MimeTypes.VIDEO_MP4
            mediaType == MediaType.VIDEO -> MimeTypes.VIDEO_MP4
            mediaType == MediaType.AUDIO -> MimeTypes.AUDIO_AAC
            else -> MimeTypes.IMAGE_JPEG
        }
    }

    private fun probeDurationMs(uri: Uri): Long? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            when (uri.scheme?.lowercase()) {
                "content" -> retriever.setDataSource(context, uri)
                "file" -> retriever.setDataSource(uri.path)
                else -> {
                    val path = uri.path
                    if (path != null && File(path).exists()) {
                        retriever.setDataSource(path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                }
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        }
    }.getOrNull()

    private fun canRead(uriOrPath: String): Boolean {
        val uri = MediaUris.parse(uriOrPath)
        return when (uri.scheme?.lowercase()) {
            "content" -> runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } == true
            }.getOrDefault(false)
            "file" -> {
                val path = uri.path ?: return false
                val file = File(path)
                file.exists() && file.canRead() && file.length() > 0L
            }
            null -> {
                val file = File(uriOrPath)
                file.exists() && file.canRead() && file.length() > 0L
            }
            else -> runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } == true
            }.getOrDefault(false)
        }
    }

    private fun fail(
        job: RenderJob,
        message: String,
        onProgress: (RenderProgress) -> Unit,
    ): RenderJob = job.copy(
        status = RenderStatus.FAILED,
        errorMessage = message,
        finishedAt = System.currentTimeMillis(),
    ).also {
        onProgress(RenderProgress(it, message))
    }

    private fun humanizeExportError(err: Throwable): String {
        val raw = err.message.orEmpty()
        return when {
            err is SecurityException || raw.contains("Permission", ignoreCase = true) ->
                "Izin file hilang. Buat montage baru & pilih ulang visual."
            raw.contains("ImageAssetLoader", ignoreCase = true) ||
                raw.contains("frame rate", ignoreCase = true) ->
                "Gagal render foto. Update app / coba JPG."
            raw.contains("No decoder", ignoreCase = true) ||
                raw.contains("codec", ignoreCase = true) ->
                "Format media tidak didukung. Coba JPG/MP4, resolusi 720p."
            raw.contains("ENOENT", ignoreCase = true) ||
                raw.contains("FileNotFound", ignoreCase = true) ||
                raw.contains("No such file", ignoreCase = true) ->
                "File media tidak ditemukan. Pasang ulang visual."
            raw.isBlank() -> "Render gagal. Coba 720p + foto JPG."
            else -> "Render gagal: ${raw.take(180)}"
        }
    }

    private fun resolveOutputSize(aspect: AspectRatio, resolution: ExportResolution): Pair<Int, Int> {
        val shortSide = when (resolution) {
            ExportResolution.SD_720 -> 720
            ExportResolution.HD_1080 -> 1080
            ExportResolution.UHD_4K -> 1080
        }
        return when (aspect) {
            AspectRatio.VERTICAL_9_16 -> shortSide to (shortSide * 16 / 9)
            AspectRatio.SQUARE_1_1 -> shortSide to shortSide
            AspectRatio.HORIZONTAL_16_9 -> (shortSide * 16 / 9) to shortSide
        }
    }

    private suspend fun runTransformer(
        composition: Composition,
        outputPath: String,
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(Result.success(Unit))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    if (cont.isActive) cont.resume(Result.failure(exportException))
                }
            })
            .build()

        cont.invokeOnCancellation {
            runCatching { transformer.cancel() }
        }

        try {
            transformer.start(composition, outputPath)
        } catch (t: Throwable) {
            if (cont.isActive) cont.resume(Result.failure(t))
        }
    }
}
