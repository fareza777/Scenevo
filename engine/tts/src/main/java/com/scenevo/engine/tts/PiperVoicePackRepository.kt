package com.scenevo.engine.tts

import android.content.Context
import com.scenevo.domain.model.VoicePackInfo
import com.scenevo.domain.model.VoicePackSource
import com.scenevo.domain.repository.SettingsRepository
import com.scenevo.domain.repository.VoicePackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiperVoicePackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val padDelivery: PlayAssetVoicePackDelivery,
) : VoicePackRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val packsFlow = MutableStateFlow(listOf(defaultPackInfo()))

    private fun packDir(): File = File(context.filesDir, "piper/$DEFAULT_PACK_ID").apply { mkdirs() }

    private fun defaultPackInfo(installed: Boolean = isPackReady()): VoicePackInfo =
        VoicePackInfo(
            id = DEFAULT_PACK_ID,
            displayName = "Piper en_US Amy (low)",
            localeTag = "en-US",
            installed = installed,
            sizeLabel = "~15 MB",
            source = VoicePackSource.DOWNLOAD,
        )

    override fun observePacks(): Flow<List<VoicePackInfo>> = packsFlow.map { list ->
        list.map { it.copy(installed = isPackReady()) }
    }

    override fun isPackReady(): Boolean {
        val dir = packDir()
        val onnx = File(dir, "$DEFAULT_PACK_ID.onnx")
        val json = File(dir, "$DEFAULT_PACK_ID.onnx.json")
        return (onnx.exists() && json.exists()) || padDelivery.isPackAvailable()
    }

    override suspend fun installDefaultPack(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val dir = packDir()
        onProgress(0.05f)
        download(ONNX_URL, File(dir, "$DEFAULT_PACK_ID.onnx")) { onProgress(0.05f + it * 0.7f) }
        download(JSON_URL, File(dir, "$DEFAULT_PACK_ID.onnx.json")) { onProgress(0.75f + it * 0.2f) }
        val prefs = settingsRepository.observeAppPreferences().first()
        settingsRepository.updateAppPreferences(
            prefs.copy(preferPiper = true, piperPackId = DEFAULT_PACK_ID),
        )
        packsFlow.value = listOf(defaultPackInfo(installed = true))
        onProgress(1f)
        Timber.i("Piper pack installed at ${dir.absolutePath}")
    }

    override suspend fun requestPlayAssetPack(onProgress: (Float) -> Unit) {
        padDelivery.requestPack(onProgress)
        packsFlow.value = listOf(defaultPackInfo(installed = isPackReady()))
    }

    override suspend fun uninstall() = withContext(Dispatchers.IO) {
        packDir().deleteRecursively()
        val prefs = settingsRepository.observeAppPreferences().first()
        settingsRepository.updateAppPreferences(
            prefs.copy(preferPiper = false, piperPackId = null),
        )
        packsFlow.value = listOf(defaultPackInfo(installed = false))
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        if (dest.exists() && dest.length() > 1_000) {
            onProgress(1f)
            return
        }
        val tmp = File(dest.absolutePath + ".part")
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed ${response.code} for $url")
            val body = response.body ?: error("Empty body")
            val total = body.contentLength().coerceAtLeast(1L)
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var written = 0L
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        written += read
                        onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    companion object {
        const val DEFAULT_PACK_ID = "en_US-amy-low"
        private const val ONNX_URL =
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/low/en_US-amy-low.onnx"
        private const val JSON_URL =
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/low/en_US-amy-low.onnx.json"
    }
}
