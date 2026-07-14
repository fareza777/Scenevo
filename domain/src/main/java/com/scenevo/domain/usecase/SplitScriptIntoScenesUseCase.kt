package com.scenevo.domain.usecase

import com.scenevo.domain.model.Scene
import java.util.UUID
import javax.inject.Inject

/**
 * Local-first scene splitter. No network required.
 * Splits script by blank lines / sentence boundaries into montage scenes.
 */
class SplitScriptIntoScenesUseCase @Inject constructor() {

    operator fun invoke(script: String, wordsPerMinute: Int = 145): List<Scene> {
        val blocks = script
            .trim()
            .split(Regex("\\n\\s*\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty {
                script
                    .split(Regex("(?<=[.!?])\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }

        if (blocks.isEmpty()) return emptyList()

        var cursor = 0L
        return blocks.mapIndexed { index, text ->
            val words = text.split(Regex("\\s+")).size.coerceAtLeast(1)
            val durationMs = ((words / wordsPerMinute.toFloat()) * 60_000f)
                .toLong()
                .coerceIn(1_800L, 12_000L)
            val scene = Scene(
                id = UUID.randomUUID().toString(),
                index = index,
                text = text,
                durationMs = durationMs,
                startMs = cursor,
            )
            cursor += durationMs
            scene
        }
    }
}
