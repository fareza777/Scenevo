package com.scenevo.engine.render

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/** Simple PCM gain for music ducking under narration. */
class GainAudioProcessor(
    private val gain: Float,
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val scaled = (sample * gain)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(scaled.toShort())
        }
        output.flip()
    }
}
