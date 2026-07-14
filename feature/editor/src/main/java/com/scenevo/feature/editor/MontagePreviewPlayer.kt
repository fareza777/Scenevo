package com.scenevo.feature.editor

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.scenevo.core.common.MediaUris
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.SubtitleCue
import com.scenevo.domain.model.Timeline
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun MontagePreviewPlayer(
    project: Project,
    timeline: Timeline?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clips = timeline?.clips.orEmpty()
    val cues = timeline?.subtitleCues.orEmpty()
    val aspect = when (project.aspectRatio) {
        AspectRatio.VERTICAL_9_16 -> 9f / 16f
        AspectRatio.SQUARE_1_1 -> 1f
        AspectRatio.HORIZONTAL_16_9 -> 16f / 9f
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(timeline?.totalDurationMs?.toFloat() ?: 1f) }
    var currentSubtitle by remember { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = error.message ?: "Preview gagal memutar media"
                playing = false
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(clips) {
        playbackError = null
        player.clearMediaItems()
        clips.forEach { clip ->
            val clipDurationMs = (clip.endMs - clip.startMs).coerceAtLeast(500L)
            val uri = MediaUris.parse(clip.mediaUri)
            val builder = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(clip.id)
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                ?: when {
                    clip.mediaUri.contains(".png", true) -> "image/png"
                    clip.mediaUri.contains(".webp", true) -> "image/webp"
                    clip.mediaUri.contains(".mp4", true) -> "video/mp4"
                    clip.mediaType == MediaType.VIDEO -> "video/mp4"
                    else -> "image/jpeg"
                }
            builder.setMimeType(mime)
            if (clip.mediaType == MediaType.IMAGE) {
                builder.setImageDurationMs(clipDurationMs)
            } else if (clip.mediaType == MediaType.VIDEO) {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(clipDurationMs)
                        .build(),
                )
            }
            player.addMediaItem(builder.build())
        }
        if (clips.isNotEmpty()) {
            player.prepare()
            player.seekTo(0, 0)
        }
        durationMs = (timeline?.totalDurationMs ?: 1L).toFloat().coerceAtLeast(1f)
    }

    LaunchedEffect(player, playing) {
        while (true) {
            val pos = player.currentPosition.coerceAtLeast(0L)
            // Approximate global position across playlist by summing prior clip durations
            val window = player.currentMediaItemIndex.coerceAtLeast(0)
            val prior = clips.take(window).sumOf { it.endMs - it.startMs }
            val global = (prior + pos).toFloat()
            positionMs = global
            durationMs = (timeline?.totalDurationMs ?: 1L).toFloat().coerceAtLeast(1f)
            currentSubtitle = resolveSubtitle(cues, global.toLong())
            delay(200)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(16.dp))
                .background(ScenevoColors.FilmGate),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        this.player = player
                        setShutterBackgroundColor(android.graphics.Color.parseColor("#0F1217"))
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )

            if (project.subtitleStyle.enabled && !currentSubtitle.isNullOrBlank()) {
                Text(
                    text = currentSubtitle.orEmpty(),
                    color = ScenevoColors.Mist,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScenevoColors.Ink.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            if (clips.isEmpty()) {
                Text(
                    "Add visuals to preview",
                    color = ScenevoColors.MistDim,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            if (playbackError != null) {
                Text(
                    playbackError.orEmpty(),
                    color = ScenevoColors.Danger,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = {
                    if (player.isPlaying) {
                        player.pause()
                        playing = false
                    } else {
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekTo(0, 0)
                        }
                        player.play()
                        playing = true
                    }
                },
            ) {
                Icon(
                    imageVector = if (playing && player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = ScenevoColors.Cue,
                )
            }
            Text(
                formatMs(positionMs.toLong()),
                style = MaterialTheme.typography.labelLarge,
                color = ScenevoColors.MistDim,
                modifier = Modifier.width(48.dp),
            )
            Slider(
                value = positionMs.coerceIn(0f, durationMs),
                onValueChange = { target ->
                    positionMs = target
                    seekTimeline(player, clips, target.toLong())
                },
                valueRange = 0f..durationMs.coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = ScenevoColors.Cue,
                    activeTrackColor = ScenevoColors.Cue,
                    inactiveTrackColor = ScenevoColors.Line,
                ),
            )
            Text(
                formatMs(durationMs.toLong()),
                style = MaterialTheme.typography.labelLarge,
                color = ScenevoColors.MistDim,
                modifier = Modifier.width(48.dp),
            )
        }

        val imageHint = clips.any { it.mediaType == MediaType.IMAGE }
        if (imageHint) {
            Text(
                "Preview: foto tampil sebagai still. Tekan ▶ untuk play. Suara baru di export.",
                style = MaterialTheme.typography.bodyMedium,
                color = ScenevoColors.MistDim,
            )
        }
    }
}

private fun resolveSubtitle(cues: List<SubtitleCue>, positionMs: Long): String? =
    cues.firstOrNull { positionMs in it.startMs until it.endMs }?.text

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun seekTimeline(
    player: ExoPlayer,
    clips: List<com.scenevo.domain.model.TimelineClip>,
    globalMs: Long,
) {
    if (clips.isEmpty()) return
    var cursor = 0L
    clips.forEachIndexed { index, clip ->
        val dur = clip.endMs - clip.startMs
        if (globalMs < cursor + dur || index == clips.lastIndex) {
            val offset = (globalMs - cursor).coerceAtLeast(0L)
            player.seekTo(index, offset)
            return
        }
        cursor += dur
    }
}
