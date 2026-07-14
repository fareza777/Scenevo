package com.scenevo.engine.stock

import android.content.Context
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.domain.model.AssetSource
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.StockKind
import com.scenevo.domain.model.StockMedia
import com.scenevo.domain.model.VisualAsset
import com.scenevo.domain.repository.SettingsRepository
import com.scenevo.domain.repository.StockRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pexels stock — images by default, videos optional.
 * Always BYOK (user Pexels key). Never ship a shared app key.
 */
@Singleton
class PexelsStockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val networkPolicy: NetworkPolicy,
) : StockRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun search(
        query: String,
        kind: StockKind,
        page: Int,
    ): List<StockMedia> = withContext(Dispatchers.IO) {
        val apiKey = requireReadyKey()
        val q = query.trim().ifBlank { "cinematic" }
        when (kind) {
            StockKind.IMAGE -> searchPhotos(apiKey, q, page)
            StockKind.VIDEO -> searchVideos(apiKey, q, page)
        }
    }

    override suspend fun cache(media: StockMedia): VisualAsset = withContext(Dispatchers.IO) {
        val apiKey = requireReadyKey()
        val cacheDir = File(context.filesDir, "stock_cache").apply { mkdirs() }
        when (media.kind) {
            StockKind.IMAGE -> {
                val outFile = File(cacheDir, "pexels_${media.id}.jpg")
                download(apiKey, media.downloadUrl, outFile)
                VisualAsset(
                    id = UUID.randomUUID().toString(),
                    uri = outFile.toURI().toString(),
                    type = MediaType.IMAGE,
                    displayName = "Pexels · ${media.photographer}",
                    width = media.width,
                    height = media.height,
                    source = AssetSource.STOCK_CACHE,
                )
            }
            StockKind.VIDEO -> {
                val outFile = File(cacheDir, "pexels_${media.id}.mp4")
                download(apiKey, media.downloadUrl, outFile)
                VisualAsset(
                    id = UUID.randomUUID().toString(),
                    uri = outFile.toURI().toString(),
                    type = MediaType.VIDEO,
                    displayName = "Pexels video · ${media.photographer}",
                    width = media.width,
                    height = media.height,
                    durationMs = media.durationMs,
                    source = AssetSource.STOCK_CACHE,
                )
            }
        }
    }

    private suspend fun requireReadyKey(): String {
        val prefs = settingsRepository.observeAppPreferences().first()
        if (!prefs.stockConsent) error("Stock search requires explicit consent in Settings.")
        if (!networkPolicy.allowStockFetch(prefs.stockWifiOnly)) {
            error(
                if (prefs.stockWifiOnly) {
                    "Stock is Wi‑Fi only. Connect to Wi‑Fi or disable the limit in Settings."
                } else {
                    "No network connection."
                },
            )
        }
        return settingsRepository.getApiKey(SettingsDataSource.PEXELS_KEY)
            ?: error("Add your Pexels API key in Settings (BYO, free at pexels.com/api).")
    }

    private fun searchPhotos(apiKey: String, q: String, page: Int): List<StockMedia> {
        val url =
            "https://api.pexels.com/v1/search?query=${enc(q)}&per_page=15&page=$page"
        val body = get(apiKey, url)
        val parsed = json.decodeFromString<PexelsPhotoSearchResponse>(body)
        return parsed.photos.map { photo ->
            StockMedia(
                id = photo.id.toString(),
                previewUrl = photo.src.medium,
                downloadUrl = photo.src.large2x ?: photo.src.large,
                photographer = photo.photographer,
                width = photo.width,
                height = photo.height,
                alt = photo.alt.orEmpty(),
                kind = StockKind.IMAGE,
            )
        }
    }

    private fun searchVideos(apiKey: String, q: String, page: Int): List<StockMedia> {
        val url =
            "https://api.pexels.com/videos/search?query=${enc(q)}&per_page=12&page=$page"
        val body = get(apiKey, url)
        val parsed = json.decodeFromString<PexelsVideoSearchResponse>(body)
        return parsed.videos.mapNotNull { video ->
            val file = pickVideoFile(video.videoFiles) ?: return@mapNotNull null
            StockMedia(
                id = "v_${video.id}",
                previewUrl = video.image,
                downloadUrl = file.link,
                photographer = video.user.name,
                width = file.width ?: video.width,
                height = file.height ?: video.height,
                alt = "Pexels video",
                kind = StockKind.VIDEO,
                durationMs = video.duration * 1000L,
            )
        }
    }

    private fun pickVideoFile(files: List<PexelsVideoFile>): PexelsVideoFile? {
        val mp4 = files.filter {
            it.fileType.contains("mp4", ignoreCase = true) &&
                !it.quality.equals("hls", ignoreCase = true)
        }
        return mp4.firstOrNull { it.quality.equals("hd", ignoreCase = true) && (it.width ?: 0) in 720..1280 }
            ?: mp4.firstOrNull { it.quality.equals("hd", ignoreCase = true) }
            ?: mp4.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
            ?: mp4.firstOrNull()
    }

    private fun get(apiKey: String, url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Pexels error ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun download(apiKey: String, url: String, dest: File) {
        if (dest.exists() && dest.length() > 1_000) return
        val tmp = File(dest.absolutePath + ".part")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed ${response.code}")
            response.body?.byteStream()?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Empty download body")
        }
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        Timber.i("Cached stock → ${dest.absolutePath}")
    }

    private fun enc(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}

@Serializable
private data class PexelsPhotoSearchResponse(val photos: List<PexelsPhoto> = emptyList())

@Serializable
private data class PexelsPhoto(
    val id: Long,
    val width: Int,
    val height: Int,
    val photographer: String,
    val alt: String? = null,
    val src: PexelsSrc,
)

@Serializable
private data class PexelsSrc(
    val medium: String,
    val large: String,
    @SerialName("large2x") val large2x: String? = null,
)

@Serializable
private data class PexelsVideoSearchResponse(val videos: List<PexelsVideo> = emptyList())

@Serializable
private data class PexelsVideo(
    val id: Long,
    val width: Int,
    val height: Int,
    val image: String,
    val duration: Int,
    val user: PexelsUser,
    @SerialName("video_files") val videoFiles: List<PexelsVideoFile> = emptyList(),
)

@Serializable
private data class PexelsUser(val name: String)

@Serializable
private data class PexelsVideoFile(
    val id: Long,
    val quality: String,
    @SerialName("file_type") val fileType: String = "video/mp4",
    val width: Int? = null,
    val height: Int? = null,
    val link: String,
)
