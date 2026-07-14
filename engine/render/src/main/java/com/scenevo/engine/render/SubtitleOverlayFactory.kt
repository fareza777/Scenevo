package com.scenevo.engine.render

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.scenevo.domain.model.SubtitleStyle

object SubtitleOverlayFactory {

    fun create(text: String, style: SubtitleStyle): OverlayEffect? {
        if (!style.enabled || !style.burnIn || text.isBlank()) return null

        val spannable = SpannableString(text.trim()).apply {
            setSpan(
                ForegroundColorSpan(style.textColorArgb.toInt()),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                BackgroundColorSpan(style.backgroundArgb.toInt()),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                AbsoluteSizeSpan((style.fontSizeSp * 2.2f).toInt().coerceIn(28, 72), true),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        val overlay: TextureOverlay = TextOverlay.createStaticTextOverlay(spannable)
        return OverlayEffect(listOf(overlay))
    }
}
