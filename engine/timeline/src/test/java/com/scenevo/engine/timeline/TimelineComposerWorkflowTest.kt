package com.scenevo.engine.timeline

import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.Scene
import com.scenevo.domain.model.VisualAsset
import com.scenevo.domain.model.VoiceTrack
import com.scenevo.domain.model.VoiceProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineComposerWorkflowTest {

    @Test
    fun happyPathProducesClipsAndCues() {
        val visual = VisualAsset(
            id = "v1",
            uri = "file:///tmp/a.jpg",
            type = MediaType.IMAGE,
            displayName = "a",
        )
        val project = Project(
            id = "p1",
            title = "Test",
            script = "Hello world. Second beat.",
            createdAt = 1L,
            updatedAt = 1L,
            scenes = listOf(
                Scene(id = "s1", index = 0, text = "Hello world", durationMs = 2000, visual = visual),
                Scene(id = "s2", index = 1, text = "Second beat", durationMs = 3000, visual = visual),
            ),
            voiceTrack = VoiceTrack(
                id = "voice",
                uri = "file:///tmp/voice.wav",
                provider = VoiceProvider.ANDROID_TTS,
                durationMs = 5000,
            ),
        )
        val timeline = TimelineComposer.compose(project)
        assertEquals(2, timeline.clips.size)
        assertEquals(5000L, timeline.totalDurationMs)
        assertEquals(2, timeline.subtitleCues.size)
        assertTrue(timeline.audioLayers.any { it.uri.endsWith("voice.wav") })
    }

    @Test
    fun scenesWithoutVisualsProduceEmptyClips() {
        val project = Project(
            id = "p2",
            title = "Empty",
            createdAt = 1L,
            updatedAt = 1L,
            scenes = listOf(
                Scene(id = "s1", index = 0, text = "No visual", durationMs = 2000),
            ),
        )
        val timeline = TimelineComposer.compose(project)
        assertTrue(timeline.clips.isEmpty())
        assertEquals(0L, timeline.totalDurationMs)
    }
}
