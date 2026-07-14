package com.scenevo.engine.tts

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Play Asset Delivery hook for optional Piper voice packs.
 * Pack name must match `assetpacks/piper_voices` (`piper_voices`).
 * Outside Play (sideload/debug) this no-ops gracefully.
 */
@Singleton
class PlayAssetVoicePackDelivery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packName = "piper_voices"

    fun isPackAvailable(): Boolean {
        val assetPath = runCatching {
            val manager = AssetPackManagerFactory.getInstance(context)
            manager.getPackLocation(packName)?.assetsPath()
        }.getOrNull()
        if (assetPath != null) {
            val dir = File(assetPath)
            return dir.exists() && dir.list()?.isNotEmpty() == true
        }
        // Debug / install-time stub assets under filesDir
        val stub = File(context.filesDir, "pad_stub/$packName")
        return stub.exists() && stub.list()?.isNotEmpty() == true
    }

    suspend fun requestPack(onProgress: (Float) -> Unit) = suspendCancellableCoroutine { cont ->
        val manager = runCatching { AssetPackManagerFactory.getInstance(context) }.getOrElse {
            // Sideload / missing Play Store: create stub marker so UI can proceed
            File(context.filesDir, "pad_stub/$packName").apply { mkdirs() }
                .resolve("README.txt")
                .writeText("Place Piper ONNX voice files here for PAD simulation.")
            onProgress(1f)
            if (cont.isActive) cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val listener = AssetPackStateUpdateListener { state: AssetPackState ->
            if (state.name() != packName) return@AssetPackStateUpdateListener
            when (state.status()) {
                AssetPackStatus.DOWNLOADING -> {
                    val total = state.totalBytesToDownload().coerceAtLeast(1L)
                    onProgress(state.bytesDownloaded().toFloat() / total)
                }
                AssetPackStatus.COMPLETED -> {
                    onProgress(1f)
                    if (cont.isActive) cont.resume(Unit)
                }
                AssetPackStatus.FAILED, AssetPackStatus.CANCELED -> {
                    Timber.w("PAD pack failed status=${state.status()} error=${state.errorCode()}")
                    if (cont.isActive) {
                        cont.resumeWithException(
                            IllegalStateException("Play Asset Delivery failed (${state.errorCode()})"),
                        )
                    }
                }
                else -> Unit
            }
        }
        manager.registerListener(listener)
        manager.fetch(listOf(packName))
        cont.invokeOnCancellation {
            runCatching { manager.unregisterListener(listener) }
            runCatching { manager.cancel(listOf(packName)) }
        }
    }
}
