package com.scenevo.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ScenevoDarkColors = darkColorScheme(
    primary = ScenevoColors.Cue,
    onPrimary = ScenevoColors.Ink,
    secondary = ScenevoColors.MistDim,
    onSecondary = ScenevoColors.Ink,
    tertiary = ScenevoColors.CueDeep,
    background = ScenevoColors.Ink,
    onBackground = ScenevoColors.Mist,
    surface = ScenevoColors.Stage,
    onSurface = ScenevoColors.Mist,
    surfaceVariant = ScenevoColors.Panel,
    onSurfaceVariant = ScenevoColors.MistDim,
    outline = ScenevoColors.Line,
    error = ScenevoColors.Danger,
    onError = ScenevoColors.Mist,
)

private val ScenevoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
    ),
)

class ScenevoMotion {
    val enter = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
    val fade = tween<Float>(durationMillis = 420)
    val slide = tween<Int>(durationMillis = 380)
}

val LocalScenevoMotion = staticCompositionLocalOf { ScenevoMotion() }

@Composable
fun ScenevoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScenevoMotion provides ScenevoMotion()) {
        MaterialTheme(
            colorScheme = ScenevoDarkColors,
            typography = ScenevoTypography,
            content = content,
        )
    }
}
