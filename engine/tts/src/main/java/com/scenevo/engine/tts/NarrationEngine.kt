package com.scenevo.engine.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class TtsResult(
    val audioFile: File,
    val durationMsEstimate: Long,
)

interface NarrationEngine {
    suspend fun synthesize(text: String, locale: Locale = Locale.getDefault()): TtsResult
    fun shutdown()
}

/**
 * Offline Android TTS — default voice path. No API key required.
 * Writes to persistent filesDir (not cache) so exports survive across days.
 */
@Singleton
class AndroidTtsNarrationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : NarrationEngine {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private val mutex = Mutex()

    private suspend fun ensureReady(): TextToSpeech = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            val existing = tts
            if (existing != null && ready) {
                cont.resume(existing)
                return@suspendCancellableCoroutine
            }
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine.language = Locale.getDefault()
                    ready = true
                    tts = engine
                    if (cont.isActive) cont.resume(engine)
                } else {
                    if (cont.isActive) {
                        cont.resumeWith(Result.failure(IllegalStateException("TTS init failed")))
                    }
                }
            }
        }
    }

    override suspend fun synthesize(text: String, locale: Locale): TtsResult = mutex.withLock {
        withContext(Dispatchers.Main.immediate) {
            val engine = ensureReady()
            engine.language = locale
            val outDir = File(context.filesDir, "tts").apply { mkdirs() }
            val outFile = File(outDir, "voice_${UUID.randomUUID()}.wav")
            val words = text.split(Regex("\\s+")).size.coerceAtLeast(1)
            val estimateMs = ((words / 145f) * 60_000f).toLong().coerceAtLeast(1500L)

            suspendCancellableCoroutine { cont ->
                val utteranceId = UUID.randomUUID().toString()
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        if (cont.isActive) cont.resume(TtsResult(outFile, estimateMs))
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (cont.isActive) {
                            cont.resumeWith(Result.failure(IllegalStateException("TTS synthesis failed")))
                        }
                    }
                })
                val code = engine.synthesizeToFile(text, null, outFile, utteranceId)
                if (code != TextToSpeech.SUCCESS) {
                    Timber.e("synthesizeToFile returned $code")
                    if (cont.isActive) {
                        cont.resumeWith(Result.failure(IllegalStateException("TTS enqueue failed")))
                    }
                }
            }
        }
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
