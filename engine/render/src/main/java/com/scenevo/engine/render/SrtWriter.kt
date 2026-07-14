package com.scenevo.engine.render

import com.scenevo.domain.model.SubtitleCue
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

object SrtWriter {

    fun write(cues: List<SubtitleCue>, outFile: File): File {
        outFile.parentFile?.mkdirs()
        val body = buildString {
            cues.forEachIndexed { index, cue ->
                append(index + 1).append('\n')
                append(formatTs(cue.startMs))
                append(" --> ")
                append(formatTs(cue.endMs))
                append('\n')
                append(cue.text.trim().replace("\n", " "))
                append("\n\n")
            }
        }
        outFile.writeText(body, Charsets.UTF_8)
        return outFile
    }

    private fun formatTs(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
