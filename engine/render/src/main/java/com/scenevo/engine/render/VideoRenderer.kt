package com.scenevo.engine.render

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.AudioKind
import com.scenevo.domain.model.ExportResolution
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.MotionEffect
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.RenderJob
import com.scenevo.domain.model.RenderStatus
import com.scenevo.domain.model.TransitionType
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

/**
 * On-device renderer using Media3 Transformer.
 * Burns subtitles via TextOverlay, writes sidecar SRT, mixes voice + ducked music.
 */
interface VideoRenderer {
    suspend fun render(
        project: Project,
        onProgress: (RenderProgress) -> Unit,
    ): RenderJob
}

@Singleton
class Media3VideoRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoRenderer {

    override suspend fun render(
        project: Project,
        onProgress: (RenderProgress) -> Unit,
    ): RenderJob = withContext(Dispatchers.Main.immediate) {
        val jobId = UUID.randomUUID().toString()
        var job = RenderJob(
            id = jobId,
            projectId = project.id,
            status = RenderStatus.PREPARING,
            progress = 0.05f,
            startedAt = System.currentTimeMillis(),
        )
        onProgress(RenderProgress(job, "Preparing timeline…"))

        val timeline = TimelineComposer.compose(project)
        if (timeline.clips.isEmpty()) {
            return@withContext job.copy(
                status = RenderStatus.FAILED,
                errorMessage = "No visuals assigned to scenes. Add images or video clips first.",
                finishedAt = System.currentTimeMillis(),
            )
        }

        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val outFile = File(outDir, "scenevo_${project.id.take(8)}_$stamp.mp4")
        val srtFile = File(outDir, "scenevo_${project.id.take(8)}_$stamp.srt")

        if (project.subtitleStyle.enabled && timeline.subtitleCues.isNotEmpty()) {
            SrtWriter.write(timeline.subtitleCues, srtFile)
        }

        job = job.copy(status = RenderStatus.RENDERING, progress = 0.15f)
        onProgress(RenderProgress(job, "Rendering on device…"))

        val outputSize = resolveOutputSize(project.aspectRatio, project.exportSettings.resolution)
        val editedItems = buildList {
            timeline.clips.forEachIndexed { index, clip ->
                if (index > 0) {
                    bumperDurationUs(clip.transition)?.let { us ->
                        add(blackBumper(context, durationUs = us))
                    }
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(clip.mediaUri))
                    .setMimeType(
                        when (clip.mediaType) {
                            MediaType.VIDEO -> MimeTypes.VIDEO_MP4
                            MediaType.IMAGE -> MimeTypes.IMAGE_JPEG
                            MediaType.AUDIO -> MimeTypes.AUDIO_AAC
                        },
                    )
                    .build()

                val videoEffects = buildList {
                    add(
                        Presentation.createForWidthAndHeight(
                            outputSize.first,
                            outputSize.second,
                            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
                        ),
                    )
                    add(motionEffect(clip.motion))
                    val cueText = timeline.subtitleCues
                        .firstOrNull { it.startMs == clip.startMs && it.endMs == clip.endMs }
                        ?.text
                        ?: project.scenes.firstOrNull { it.id == clip.sceneId }?.text
                    SubtitleOverlayFactory.create(cueText.orEmpty(), project.subtitleStyle)?.let { add(it) }
                }

                add(
                    EditedMediaItem.Builder(mediaItem)
                        .setDurationUs((clip.endMs - clip.startMs) * 1000)
                        .setRemoveAudio(true)
                        .setEffects(Effects(/* audioProcessors = */ emptyList(), videoEffects))
                        .build(),
                )
            }
        }

        val sequences = mutableListOf(
            EditedMediaItemSequence.Builder(editedItems).build(),
        )

        timeline.audioLayers.forEach { layer ->
            if (layer.kind == AudioKind.MUSIC && !project.exportSettings.includeMusic) return@forEach
            val processors = mutableListOf<AudioProcessor>()
            if (layer.kind == AudioKind.MUSIC) {
                processors += GainAudioProcessor(layer.volume.coerceIn(0.05f, 1f))
            }
            val audioItem = EditedMediaItem.Builder(
                MediaItem.fromUri(Uri.parse(layer.uri)),
            )
                .setDurationUs((layer.endMs - layer.startMs).coerceAtLeast(500L) * 1000)
                .setEffects(Effects(processors, emptyList()))
                .build()
            sequences += EditedMediaItemSequence.Builder(audioItem).build()
        }

        val composition = Composition.Builder(sequences).build()

        job = job.copy(status = RenderStatus.MUXING, progress = 0.55f)
        onProgress(RenderProgress(job, "Muxing audio & subtitles…"))

        val result = runTransformer(composition, outFile.absolutePath)
        result.fold(
            onSuccess = {
                job.copy(
                    status = RenderStatus.COMPLETED,
                    progress = 1f,
                    outputUri = outFile.absolutePath,
                    finishedAt = System.currentTimeMillis(),
                ).also {
                    onProgress(
                        RenderProgress(
                            it,
                            if (srtFile.exists()) {
                                "Export complete (+ SRT)"
                            } else {
                                "Export complete"
                            },
                        ),
                    )
                }
            },
            onFailure = { err ->
                Timber.e(err, "Render failed")
                job.copy(
                    status = RenderStatus.FAILED,
                    errorMessage = err.message ?: "Render failed",
                    finishedAt = System.currentTimeMillis(),
                ).also {
                    onProgress(RenderProgress(it, it.errorMessage ?: "Failed"))
                }
            },
        )
    }

    private fun bumperDurationUs(transition: TransitionType): Long? = when (transition) {
        TransitionType.CUT -> null
        TransitionType.CROSSFADE -> 200_000L
        TransitionType.FADE_TO_BLACK -> 420_000L
        TransitionType.SLIDE_LEFT, TransitionType.ZOOM -> 280_000L
    }

    private fun resolveOutputSize(aspect: AspectRatio, resolution: ExportResolution): Pair<Int, Int> {
        val shortSide = when (resolution) {
            ExportResolution.SD_720 -> 720
            ExportResolution.HD_1080 -> 1080
            ExportResolution.UHD_4K -> 2160
        }
        return when (aspect) {
            AspectRatio.VERTICAL_9_16 -> shortSide to (shortSide * 16 / 9)
            AspectRatio.SQUARE_1_1 -> shortSide to shortSide
            AspectRatio.HORIZONTAL_16_9 -> (shortSide * 16 / 9) to shortSide
        }
    }

    private fun motionEffect(motion: MotionEffect) = when (motion) {
        MotionEffect.NONE -> ScaleAndRotateTransformation.Builder().setScale(1f, 1f).build()
        MotionEffect.KEN_BURNS_ZOOM_IN,
        MotionEffect.KEN_BURNS_ZOOM_OUT,
        -> ScaleAndRotateTransformation.Builder().setScale(1.08f, 1.08f).build()
        MotionEffect.PAN_LEFT,
        MotionEffect.PAN_RIGHT,
        -> ScaleAndRotateTransformation.Builder().setScale(1.12f, 1.12f).build()
    }

    private fun blackBumper(context: Context, durationUs: Long): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(BlackFrameProvider.getUri(context))))
            .setMimeType(MimeTypes.IMAGE_PNG)
            .build()
        return EditedMediaItem.Builder(mediaItem)
            .setDurationUs(durationUs)
            .setRemoveAudio(true)
            .build()
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
