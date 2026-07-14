package com.scenevo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scenevo.core.designsystem.theme.LocalScenevoMotion
import com.scenevo.core.designsystem.theme.ScenevoColors

@Composable
fun ScenevoBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScenevoColors.StageGradient),
    ) {
        FilmPerforationStrip(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().height(18.dp))
        CueSweep(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(120.dp))
        content()
        FilmPerforationStrip(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(18.dp))
    }
}

@Composable
private fun CueSweep(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cue")
    val shift by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(7800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cueShift",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    ScenevoColors.Cue.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startX = w * (shift - 0.25f),
                endX = w * (shift + 0.25f),
            ),
        )
    }
}

@Composable
fun FilmPerforationStrip(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(ScenevoColors.FilmGate)) {
        val holeW = 10.dp.toPx()
        val holeH = 8.dp.toPx()
        val gap = 14.dp.toPx()
        var x = 8.dp.toPx()
        val y = (size.height - holeH) / 2f
        while (x < size.width) {
            drawRoundRect(
                color = ScenevoColors.Ink,
                topLeft = Offset(x, y),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
            x += holeW + gap
        }
    }
}

@Composable
fun ScenevoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ScenevoColors.Cue,
            contentColor = ScenevoColors.Ink,
            disabledContainerColor = ScenevoColors.PanelLift,
            disabledContentColor = ScenevoColors.MistDim,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ScenevoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = ScenevoColors.MistDim,
        )
    }
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = "SCENEVO",
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium,
            color = ScenevoColors.Cue,
        )
        if (!compact) {
            Text(
                text = "OFFLINE VIDEO MAKER",
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.MistDim,
            )
        }
    }
}

@Composable
fun AtmosphereHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val motion = LocalScenevoMotion.current
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(motion.fade) + slideInVertically { it / 8 },
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(ScenevoColors.HeroGradient)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    BrandMark()
                    trailing?.invoke()
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ScenevoColors.Mist,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ScenevoColors.MistDim,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(ScenevoColors.Cue, Color.Transparent),
                    ),
                ),
        )
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = ScenevoColors.Mist)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.MistDim)
        Spacer(Modifier.height(28.dp))
        ScenevoPrimaryButton(text = actionLabel, onClick = onAction)
    }
}

@Composable
fun StepChipRow(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, label ->
            val active = index == currentIndex
            val done = index < currentIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                active -> ScenevoColors.Cue.copy(alpha = 0.16f)
                                done -> ScenevoColors.PanelLift
                                else -> ScenevoColors.Panel
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                active -> ScenevoColors.Cue.copy(alpha = 0.55f)
                                else -> ScenevoColors.Line
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${index + 1}  $label",
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            active -> ScenevoColors.CueHot
                            done -> ScenevoColors.Mist
                            else -> ScenevoColors.MistDim
                        },
                    )
                }
                if (index != steps.lastIndex) {
                    Canvas(modifier = Modifier.width(12.dp).height(2.dp)) {
                        drawLine(
                            color = ScenevoColors.Line,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectTile(
    title: String,
    meta: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ScenevoColors.Panel)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = status.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.Cue,
            )
            Text(
                text = "OPEN →",
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.MistDim,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = ScenevoColors.Mist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(text = meta, style = MaterialTheme.typography.bodyMedium, color = ScenevoColors.MistDim)
    }
}

@Composable
fun ScreenSection(
    eyebrow: String,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = ScenevoColors.Cue,
        )
        Text(text = title, style = MaterialTheme.typography.headlineLarge, color = ScenevoColors.Mist)
        if (body != null) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.MistDim)
        }
    }
}

val ScreenPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
