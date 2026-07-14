package com.scenevo.engine.timeline

import com.scenevo.domain.model.AudioKind
import com.scenevo.domain.model.AudioLayer
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.SubtitleCue
import com.scenevo.domain.model.Timeline
import com.scenevo.domain.model.TimelineClip
import java.util.UUID

/**
 * Builds a deterministic timeline from a project — the mobile equivalent of
 * OpenMontage's edit/compose directors, without requiring a network.
 */
object TimelineComposer {

    fun compose(project: Project): Timeline {
        val clips = mutableListOf<TimelineClip>()
        val cues = mutableListOf<SubtitleCue>()
        var cursor = 0L

        project.scenes.sortedBy { it.index }.forEach { scene ->
            val visual = scene.visual ?: return@forEach
            val end = cursor + scene.durationMs
            clips += TimelineClip(
                id = UUID.randomUUID().toString(),
                sceneId = scene.id,
                mediaUri = visual.uri,
                mediaType = visual.type,
                startMs = cursor,
                endMs = end,
                transition = scene.transition,
                motion = scene.motion,
            )
            if (project.subtitleStyle.enabled && scene.text.isNotBlank()) {
                cues += SubtitleCue(
                    id = UUID.randomUUID().toString(),
                    text = scene.text,
                    startMs = cursor,
                    endMs = end,
                )
            }
            cursor = end
        }

        val audio = mutableListOf<AudioLayer>()
        val voice = project.voiceTrack
        val voiceUri = voice?.uri
        if (voice != null && voiceUri != null) {
            audio += AudioLayer(
                id = voice.id,
                uri = voiceUri,
                startMs = 0L,
                endMs = voice.durationMs.coerceAtLeast(cursor),
                volume = 1f,
                kind = AudioKind.VOICE,
            )
        }
        project.musicTrack?.let { music ->
            audio += AudioLayer(
                id = music.id,
                uri = music.uri,
                startMs = 0L,
                endMs = cursor,
                volume = music.volume,
                kind = AudioKind.MUSIC,
            )
        }

        return Timeline(
            projectId = project.id,
            totalDurationMs = cursor,
            clips = clips,
            audioLayers = audio,
            subtitleCues = cues,
        )
    }
}
