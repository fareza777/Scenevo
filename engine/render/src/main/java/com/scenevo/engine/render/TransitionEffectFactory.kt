package com.scenevo.engine.render

import androidx.media3.common.Effect
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import com.scenevo.domain.model.MotionEffect
import com.scenevo.domain.model.TransitionType

/**
 * Media3-side transition / motion effects.
 * True FFmpeg xfade can plug in via [FfmpegTransitionBridge] after primary export.
 */
object TransitionEffectFactory {

    fun bumperDurationUs(transition: TransitionType): Long? = when (transition) {
        TransitionType.CUT -> null
        TransitionType.CROSSFADE -> 220_000L
        TransitionType.FADE_TO_BLACK -> 480_000L
        TransitionType.SLIDE_LEFT -> 300_000L
        TransitionType.ZOOM -> 320_000L
    }

    fun clipEffects(motion: MotionEffect, transition: TransitionType): List<Effect> = buildList {
        add(motionEffect(motion))
        when (transition) {
            TransitionType.FADE_TO_BLACK -> {
                add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.82f)
                        .setGreenScale(0.82f)
                        .setBlueScale(0.82f)
                        .build(),
                )
            }
            TransitionType.ZOOM -> {
                add(ScaleAndRotateTransformation.Builder().setScale(1.15f, 1.15f).build())
            }
            TransitionType.SLIDE_LEFT -> {
                add(ScaleAndRotateTransformation.Builder().setScale(1.1f, 1.1f).build())
            }
            TransitionType.CROSSFADE, TransitionType.CUT -> Unit
        }
    }

    private fun motionEffect(motion: MotionEffect): Effect = when (motion) {
        MotionEffect.NONE -> ScaleAndRotateTransformation.Builder().setScale(1f, 1f).build()
        MotionEffect.KEN_BURNS_ZOOM_IN,
        MotionEffect.KEN_BURNS_ZOOM_OUT,
        -> ScaleAndRotateTransformation.Builder().setScale(1.08f, 1.08f).build()
        MotionEffect.PAN_LEFT,
        MotionEffect.PAN_RIGHT,
        -> ScaleAndRotateTransformation.Builder().setScale(1.12f, 1.12f).build()
    }
}

/**
 * Optional post-pass for FFmpeg xfade / fancy transitions.
 * Default no-op keeps the APK free of EOL FFmpeg-Kit binaries; swap in a real
 * implementation when you ship a maintained FFmpeg AAR.
 */
interface FfmpegTransitionBridge {
    /** @return path to post-processed file, or [inputPath] if unchanged */
    suspend fun polish(inputPath: String, projectId: String): String
}

class NoOpFfmpegTransitionBridge @javax.inject.Inject constructor() : FfmpegTransitionBridge {
    override suspend fun polish(inputPath: String, projectId: String): String = inputPath
}
