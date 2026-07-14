package com.scenevo.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ScenevoColors {
    val Ink = Color(0xFF0B0D10)
    val Stage = Color(0xFF12151B)
    val Panel = Color(0xFF1A1F28)
    val PanelLift = Color(0xFF232A35)
    val Line = Color(0xFF2E3644)
    val Mist = Color(0xFFE7E2D8)
    val MistDim = Color(0xFFB8B2A6)
    val Cue = Color(0xFFE2A23A)
    val CueHot = Color(0xFFF0B84D)
    val CueDeep = Color(0xFF9A6B1C)
    val Signal = Color(0xFF6FCF97)
    val Danger = Color(0xFFE57373)
    val FilmGate = Color(0xFF0F1217)

    val StageGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1F28),
            Color(0xFF10141A),
            Color(0xFF080A0D),
        ),
    )

    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF242B38),
            Color(0xFF151922),
            Color(0xFF0B0D10),
        ),
    )

    val PanelSheen = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A323F),
            Panel,
        ),
    )

    val CueWash = Brush.horizontalGradient(
        colors = listOf(
            Cue.copy(alpha = 0.0f),
            Cue.copy(alpha = 0.18f),
            Cue.copy(alpha = 0.0f),
        ),
    )
}
