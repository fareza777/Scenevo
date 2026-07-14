package com.scenevo.engine.tts

import android.content.Context
import com.scenevo.core.common.MediaUris
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ElevenLabs TTS — optional BYOK only. Never bundles an app key.
 */
@Singleton
class ElevenLabsNarrationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun synthesize(text: String, locale: Locale = Locale.getDefault()): TtsResult =
        withContext(Dispatchers.IO) {
            val apiKey = settingsRepository.getApiKey(SettingsDataSource.ELEVENLABS_KEY)
                ?: error("Add your ElevenLabs API key in Settings (BYOK).")
            val prefs = settingsRepository.observeAppPreferences().first()
            val voiceId = prefs.elevenLabsVoiceId.ifBlank { DEFAULT_VOICE }
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
            // Multilingual model helps Indonesian scripts.
            val payload = """
                {
                  "text": ${text.toJsonString()},
                  "model_id": "eleven_multilingual_v2",
                  "voice_settings": { "stability": 0.4, "similarity_boost": 0.75 }
                }
            """.trimIndent()
            val request = Request.Builder()
                .url(url)
                .header("xi-api-key", apiKey)
                .header("Accept", "audio/mpeg")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val outDir = File(context.filesDir, "tts").apply { mkdirs() }
            val outFile = File(outDir, "eleven_${UUID.randomUUID()}.mp3")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty().take(200)
                    error("ElevenLabs error ${response.code}: $err")
                }
                response.body?.byteStream()?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Empty ElevenLabs audio body")
            }
            val words = text.split(Regex("\\s+")).size.coerceAtLeast(1)
            val estimateMs = ((words / 145f) * 60_000f).toLong().coerceAtLeast(1500L)
            Timber.i("ElevenLabs voice saved ${MediaUris.fileUri(outFile)} locale=$locale")
            TtsResult(outFile, estimateMs)
        }

    private fun String.toJsonString(): String =
        buildString {
            append('"')
            forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append('"')
        }

    companion object {
        // Public demo voice id; user can override in Settings.
        const val DEFAULT_VOICE = "JBFqnCBsd6RMkjVDRZzb"
    }
}
