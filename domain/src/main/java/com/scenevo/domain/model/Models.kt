package com.scenevo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val title: String,
    val script: String = "",
    val aspectRatio: AspectRatio = AspectRatio.VERTICAL_9_16,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val createdAt: Long,
    val updatedAt: Long,
    val scenes: List<Scene> = emptyList(),
    val voiceTrack: VoiceTrack? = null,
    val musicTrack: MusicTrack? = null,
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val exportSettings: ExportSettings = ExportSettings(),
)

@Serializable
enum class AspectRatio(val label: String, val width: Int, val height: Int) {
    VERTICAL_9_16("9:16", 1080, 1920),
    SQUARE_1_1("1:1", 1080, 1080),
    HORIZONTAL_16_9("16:9", 1920, 1080),
}

@Serializable
enum class ProjectStatus {
    DRAFT,
    READY,
    RENDERING,
    EXPORTED,
    FAILED,
}

@Serializable
data class Scene(
    val id: String,
    val index: Int,
    val text: String,
    val durationMs: Long,
    val visual: VisualAsset? = null,
    val transition: TransitionType = TransitionType.CROSSFADE,
    val motion: MotionEffect = MotionEffect.KEN_BURNS_ZOOM_IN,
    val startMs: Long = 0L,
)

@Serializable
data class VisualAsset(
    val id: String,
    val uri: String,
    val type: MediaType,
    val displayName: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val source: AssetSource = AssetSource.LOCAL,
)

@Serializable
enum class MediaType { IMAGE, VIDEO, AUDIO }

@Serializable
enum class AssetSource { LOCAL, IMPORTED, STOCK_CACHE }

@Serializable
enum class TransitionType {
    CUT,
    CROSSFADE,
    FADE_TO_BLACK,
    SLIDE_LEFT,
    ZOOM,
}

@Serializable
enum class MotionEffect {
    NONE,
    KEN_BURNS_ZOOM_IN,
    KEN_BURNS_ZOOM_OUT,
    PAN_LEFT,
    PAN_RIGHT,
}

@Serializable
data class VoiceTrack(
    val id: String,
    val uri: String?,
    val provider: VoiceProvider,
    val voiceId: String? = null,
    val speed: Float = 1.0f,
    val durationMs: Long = 0L,
)

@Serializable
enum class VoiceProvider {
    ANDROID_TTS,
    PIPER_LOCAL,
    IMPORTED_AUDIO,
    OPENAI_USER_KEY,
    ELEVENLABS_USER_KEY,
}

@Serializable
data class MusicTrack(
    val id: String,
    val uri: String,
    val displayName: String,
    val volume: Float = 0.25f,
    val fadeInMs: Long = 500L,
    val fadeOutMs: Long = 1000L,
)

@Serializable
data class SubtitleStyle(
    val enabled: Boolean = true,
    val fontSizeSp: Int = 22,
    val textColorArgb: Long = 0xFFFFFFFF,
    val backgroundArgb: Long = 0x99000000,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val burnIn: Boolean = true,
)

@Serializable
enum class SubtitlePosition { TOP, CENTER, BOTTOM }

@Serializable
data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.HD_1080,
    val fps: Int = 30,
    val videoBitrateMbps: Float = 8f,
    val includeSubtitles: Boolean = true,
    val includeMusic: Boolean = true,
)

@Serializable
enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    SD_720(720, 1280, "720p"),
    HD_1080(1080, 1920, "1080p"),
    UHD_4K(2160, 3840, "4K"),
}

@Serializable
data class Timeline(
    val projectId: String,
    val totalDurationMs: Long,
    val clips: List<TimelineClip>,
    val audioLayers: List<AudioLayer>,
    val subtitleCues: List<SubtitleCue>,
)

@Serializable
data class TimelineClip(
    val id: String,
    val sceneId: String,
    val mediaUri: String,
    val mediaType: MediaType,
    val startMs: Long,
    val endMs: Long,
    val transition: TransitionType,
    val motion: MotionEffect,
)

@Serializable
data class AudioLayer(
    val id: String,
    val uri: String,
    val startMs: Long,
    val endMs: Long,
    val volume: Float,
    val kind: AudioKind,
)

@Serializable
enum class AudioKind { VOICE, MUSIC, SFX }

@Serializable
data class SubtitleCue(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@Serializable
data class RenderJob(
    val id: String,
    val projectId: String,
    val status: RenderStatus,
    val progress: Float = 0f,
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
)

@Serializable
enum class RenderStatus {
    QUEUED,
    PREPARING,
    RENDERING,
    MUXING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@Serializable
data class AiProviderConfig(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.NONE,
    val encryptedApiKeyRef: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
)

@Serializable
enum class AiProvider {
    NONE,
    OPENAI_USER_KEY,
    ANTHROPIC_USER_KEY,
    GEMINI_USER_KEY,
    OLLAMA_LOCAL,
}

@Serializable
data class AppPreferences(
    val stockConsent: Boolean = false,
    val stockWifiOnly: Boolean = true,
    val preferPiper: Boolean = false,
    val piperPackId: String? = null,
)

@Serializable
data class StockPhoto(
    val id: String,
    val previewUrl: String,
    val downloadUrl: String,
    val photographer: String,
    val width: Int,
    val height: Int,
    val alt: String = "",
)

@Serializable
data class VoicePackInfo(
    val id: String,
    val displayName: String,
    val localeTag: String,
    val installed: Boolean,
    val sizeLabel: String,
    val source: VoicePackSource,
)

@Serializable
enum class VoicePackSource {
    DOWNLOAD,
    PLAY_ASSET_DELIVERY,
}
