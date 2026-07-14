package com.scenevo.engine.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.scenevo.domain.model.VoiceProvider
import com.scenevo.domain.repository.SettingsRepository
import com.scenevo.domain.repository.VoicePackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class NarrationOutcome(
    val result: TtsResult,
    val provider: VoiceProvider,
)

interface SmartNarrationEngine : NarrationEngine {
    suspend fun synthesizeSmart(text: String, locale: Locale = Locale.getDefault()): NarrationOutcome
}

@Singleton
class PreferLocalNarrationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val androidTts: AndroidTtsNarrationEngine,
    private val voicePackRepository: VoicePackRepository,
    private val settingsRepository: SettingsRepository,
) : SmartNarrationEngine {

    override suspend fun synthesize(text: String, locale: Locale): TtsResult =
        synthesizeSmart(text, locale).result

    override suspend fun synthesizeSmart(text: String, locale: Locale): NarrationOutcome {
        val prefs = settingsRepository.observeAppPreferences().first()
        val packReady = voicePackRepository.isPackReady()
        val preferPiper = prefs.preferPiper || packReady

        if (preferPiper) {
            val enginePkg = findInstalledNeuralEngine()
            if (enginePkg != null) {
                val result = synthesizeWithEnginePackage(text, locale, enginePkg)
                return NarrationOutcome(result, VoiceProvider.PIPER_LOCAL)
            }
        }

        val fallback = androidTts.synthesize(text, locale)
        val provider = if (preferPiper && packReady) VoiceProvider.PIPER_LOCAL else VoiceProvider.ANDROID_TTS
        return NarrationOutcome(fallback, provider)
    }

    private fun findInstalledNeuralEngine(): String? {
        val known = listOf(
            "com.k2fsa.sherpa.onnx.tts.engine",
            "com.rhasspy.piper",
            "com.piper.tts",
        )
        return known.firstOrNull { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }
    }

    private suspend fun synthesizeWithEnginePackage(
        text: String,
        locale: Locale,
        enginePackage: String,
    ): TtsResult = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context, { status ->
                if (status != TextToSpeech.SUCCESS) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("Neural TTS init failed"))
                    return@TextToSpeech
                }
                engine.language = locale
                val outDir = File(context.filesDir, "tts").apply { mkdirs() }
                val outFile = File(outDir, "piper_${UUID.randomUUID()}.wav")
                val words = text.split(Regex("\\s+")).size.coerceAtLeast(1)
                val estimateMs = ((words / 145f) * 60_000f).toLong().coerceAtLeast(1500L)
                val utteranceId = UUID.randomUUID().toString()
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        engine.shutdown()
                        if (cont.isActive) cont.resume(TtsResult(outFile, estimateMs))
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        engine.shutdown()
                        if (cont.isActive) {
                            cont.resumeWithException(IllegalStateException("Piper synthesis failed"))
                        }
                    }
                })
                val code = engine.synthesizeToFile(text, null, outFile, utteranceId)
                if (code != TextToSpeech.SUCCESS && cont.isActive) {
                    engine.shutdown()
                    cont.resumeWithException(IllegalStateException("Piper enqueue failed"))
                }
            }, enginePackage)
            cont.invokeOnCancellation { runCatching { engine.shutdown() } }
        }
    }

    override fun shutdown() = androidTts.shutdown()
}
