package com.scenevo.engine.stock

import android.content.Context
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.domain.model.AssetSource
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.StockPhoto
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

@Singleton
class PexelsStockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val networkPolicy: NetworkPolicy,
    private val settingsDataSource: SettingsDataSource,
) : StockRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String, page: Int): List<StockPhoto> = withContext(Dispatchers.IO) {
        val prefs = settingsRepository.observeAppPreferences().first()
        if (!prefs.stockConsent) {
            error("Stock search requires explicit consent in Settings.")
        }
        if (!networkPolicy.allowStockFetch(prefs.stockWifiOnly)) {
            error(
                if (prefs.stockWifiOnly) {
                    "Stock search is Wi‑Fi only. Connect to Wi‑Fi or disable the limit in Settings."
                } else {
                    "No network connection."
                },
            )
        }
        val apiKey = settingsRepository.getApiKey(SettingsDataSource.PEXELS_KEY)
            ?: error("Add your Pexels API key in Settings (BYO, free at pexels.com/api).")
        val q = query.trim().ifBlank { "cinematic" }
        val url = "https://api.pexels.com/v1/search?query=${java.net.URLEncoder.encode(q, "UTF-8")}&per_page=15&page=$page"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Pexels error ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<PexelsSearchResponse>(body)
            parsed.photos.map { photo ->
                StockPhoto(
                    id = photo.id.toString(),
                    previewUrl = photo.src.medium,
                    downloadUrl = photo.src.large2x ?: photo.src.large,
                    photographer = photo.photographer,
                    width = photo.width,
                    height = photo.height,
                    alt = photo.alt.orEmpty(),
                )
            }
        }
    }

    override suspend fun cachePhoto(photo: StockPhoto): VisualAsset = withContext(Dispatchers.IO) {
        val prefs = settingsRepository.observeAppPreferences().first()
        if (!prefs.stockConsent) error("Stock cache requires consent.")
        if (!networkPolicy.allowStockFetch(prefs.stockWifiOnly)) {
            error("Cannot download stock off Wi‑Fi (policy).")
        }
        val apiKey = settingsRepository.getApiKey(SettingsDataSource.PEXELS_KEY)
            ?: error("Missing Pexels API key.")

        val cacheDir = File(context.filesDir, "stock_cache").apply { mkdirs() }
        val outFile = File(cacheDir, "pexels_${photo.id}.jpg")
        if (!outFile.exists()) {
            val request = Request.Builder()
                .url(photo.downloadUrl)
                .header("Authorization", apiKey)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed ${response.code}")
                response.body?.byteStream()?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Empty download body")
            }
            Timber.i("Cached stock photo ${photo.id} → ${outFile.absolutePath}")
        }
        VisualAsset(
            id = UUID.randomUUID().toString(),
            uri = outFile.toURI().toString(),
            type = MediaType.IMAGE,
            displayName = "Pexels · ${photo.photographer}",
            width = photo.width,
            height = photo.height,
            source = AssetSource.STOCK_CACHE,
        )
    }
}

@Serializable
private data class PexelsSearchResponse(
    val photos: List<PexelsPhoto> = emptyList(),
)

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
