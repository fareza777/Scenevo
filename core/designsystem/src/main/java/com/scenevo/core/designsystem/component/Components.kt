package com.scenevo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScenevoBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScenevoColors.StageGradient),
    ) {
        SoftVignette(modifier = Modifier.fillMaxSize())
        GrainOverlay(modifier = Modifier.fillMaxSize())
        FilmPerforationStrip(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().height(20.dp))
        CueSweep(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(140.dp))
        content()
        FilmPerforationStrip(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(20.dp))
    }
}

@Composable
private fun SoftVignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    ScenevoColors.Ink.copy(alpha = 0.55f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.35f),
                radius = size.maxDimension * 0.85f,
            ),
        )
    }
}

@Composable
private fun GrainOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 28.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < size.width) {
                drawCircle(
                    color = ScenevoColors.Mist.copy(alpha = 0.018f),
                    radius = 1.1f,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
            row++
        }
    }
}

@Composable
private fun CueSweep(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cue")
    val shift by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8200, easing = LinearEasing),
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
                    ScenevoColors.Cue.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                startX = w * (shift - 0.28f),
                endX = w * (shift + 0.28f),
            ),
        )
    }
}

@Composable
fun FilmPerforationStrip(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(ScenevoColors.FilmGate)) {
        val holeW = 11.dp.toPx()
        val holeH = 8.dp.toPx()
        val gap = 13.dp.toPx()
        var x = 10.dp.toPx()
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.98f else 1f, label = "btnScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ScenevoColors.Ink,
            disabledContainerColor = ScenevoColors.PanelLift,
            disabledContentColor = ScenevoColors.MistDim,
        ),
        contentPadding = PaddingValues(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            listOf(ScenevoColors.CueHot, ScenevoColors.Cue, ScenevoColors.CueDeep),
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(ScenevoColors.PanelLift, ScenevoColors.PanelLift),
                        )
                    },
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ScenevoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) ScenevoColors.Mist else ScenevoColors.Line,
        )
    }
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(ScenevoColors.CueHot, ScenevoColors.CueDeep),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(if (compact) 14.dp else 18.dp)) {
                val r = size.minDimension / 2f
                drawCircle(color = ScenevoColors.Ink.copy(alpha = 0.25f), radius = r)
                drawCircle(color = ScenevoColors.Ink, radius = r * 0.35f)
            }
        }
        Column {
            Text(
                text = "SCENEVO",
                style = if (compact) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.displayMedium
                },
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
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ScenevoColors.Cue),
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        ReelMark()
        Spacer(Modifier.height(22.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = ScenevoColors.Mist)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.MistDim)
        Spacer(Modifier.height(28.dp))
        ScenevoPrimaryButton(text = actionLabel, onClick = onAction)
    }
}

@Composable
private fun ReelMark() {
    val transition = rememberInfiniteTransition(label = "reel")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reelAngle",
    )
    Canvas(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(1.dp, ScenevoColors.Line, CircleShape)
            .background(ScenevoColors.Panel),
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2.4f
        drawCircle(color = ScenevoColors.Line, radius = r, center = c)
        drawCircle(color = ScenevoColors.FilmGate, radius = r * 0.55f, center = c)
        for (i in 0 until 6) {
            val a = Math.toRadians((angle + i * 60.0))
            val p = Offset(
                c.x + (r * 0.78f * cos(a)).toFloat(),
                c.y + (r * 0.78f * sin(a)).toFloat(),
            )
            drawCircle(color = ScenevoColors.Cue.copy(alpha = 0.55f), radius = 3.dp.toPx(), center = p)
        }
        drawCircle(color = ScenevoColors.Cue, radius = 4.dp.toPx(), center = c)
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
                        .clip(RoundedCornerShape(10.dp))
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
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
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
fun SegmentedChoice(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScenevoColors.Panel)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) {
                            ScenevoColors.Cue.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) ScenevoColors.CueHot else ScenevoColors.MistDim,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectTile(
    title: String,
    meta: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "tileScale")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(ScenevoColors.PanelLift.copy(alpha = 0.55f), ScenevoColors.Panel),
                ),
            )
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(18.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                },
            )
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(status)
            Text(
                text = "OPEN →",
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.MistDim,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = ScenevoColors.Mist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(text = meta, style = MaterialTheme.typography.bodyMedium, color = ScenevoColors.MistDim)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ScenevoColors.Line),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(statusProgress(status))
                    .height(3.dp)
                    .background(ScenevoColors.Cue),
            )
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status.uppercase()) {
        "EXPORTED" -> ScenevoColors.Signal
        "FAILED", "RENDERING" -> ScenevoColors.CueHot
        else -> ScenevoColors.Cue
    }
    Text(
        text = status.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun statusProgress(status: String): Float = when (status.uppercase()) {
    "DRAFT" -> 0.25f
    "READY" -> 0.55f
    "RENDERING" -> 0.75f
    "EXPORTED" -> 1f
    "FAILED" -> 0.4f
    else -> 0.35f
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

@Composable
fun FilmPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ScenevoColors.PanelSheen)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = { content() },
    )
}

@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (accent) ScenevoColors.Cue.copy(alpha = 0.14f) else ScenevoColors.PanelLift,
            )
            .border(
                1.dp,
                if (accent) ScenevoColors.Cue.copy(alpha = 0.45f) else ScenevoColors.Line,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (accent) ScenevoColors.CueHot else ScenevoColors.Mist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

val ScreenPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
