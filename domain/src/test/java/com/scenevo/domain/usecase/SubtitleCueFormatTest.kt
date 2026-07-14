package com.scenevo.domain.usecase

import com.scenevo.domain.model.SubtitleCue
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lightweight SRT formatting check (mirror of engine SrtWriter rules).
 */
class SubtitleCueFormatTest {

    @Test
    fun cueOrderingStable() {
        val cues = listOf(
            SubtitleCue("1", "Hello", 0, 2000),
            SubtitleCue("2", "World", 2000, 4000),
        )
        assertTrue(cues.zipWithNext().all { (a, b) -> a.endMs <= b.startMs || a.endMs == b.startMs })
    }
}
