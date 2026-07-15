package com.scenevo.feature.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scenevo.core.designsystem.component.ActionChip
import com.scenevo.core.designsystem.component.BrandMark
import com.scenevo.core.designsystem.component.FilmPanel
import com.scenevo.core.designsystem.component.ScenevoBackdrop
import com.scenevo.core.designsystem.component.ScenevoPrimaryButton
import com.scenevo.core.designsystem.component.ScenevoSecondaryButton
import com.scenevo.core.designsystem.component.SegmentedChoice
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.Scene

@Composable
fun EditorRoute(
    onExport: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val project = state.project

    ScenevoBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            BrandMark(compact = true)
            Spacer(Modifier.height(8.dp))

            if (project == null) {
                Text("Loading cut…", color = ScenevoColors.MistDim)
                return@Column
            }

            Text(
                text = "EDITOR",
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.Cue,
            )
            Text(
                text = project.title,
                style = MaterialTheme.typography.headlineMedium,
                color = ScenevoColors.Mist,
            )
            Text(
                text = "${state.timeline?.clips?.size ?: 0} clips · " +
                    "${"%.1f".format((state.timeline?.totalDurationMs ?: 0) / 1000f)}s · " +
                    project.aspectRatio.label,
                style = MaterialTheme.typography.bodyMedium,
                color = ScenevoColors.MistDim,
            )
            Spacer(Modifier.height(10.dp))

            // Compact preview — full 9:16 height was pushing Play/Export off-screen.
            MontagePreviewPlayer(
                project = project,
                timeline = state.timeline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ScenevoColors.Line, RoundedCornerShape(18.dp)),
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "Aspect ratio",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScenevoColors.Cue,
                    )
                    Spacer(Modifier.height(6.dp))
                    SegmentedChoice(
                        options = AspectRatio.entries.map { it.label },
                        selectedIndex = AspectRatio.entries.indexOf(project.aspectRatio).coerceAtLeast(0),
                        onSelect = { index -> viewModel.setAspectRatio(AspectRatio.entries[index]) },
                    )
                }
                item {
                    FilmPanel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Burn-in subtitles",
                                color = ScenevoColors.Mist,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = project.subtitleStyle.enabled,
                                onCheckedChange = viewModel::setSubtitlesEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ScenevoColors.Ink,
                                    checkedTrackColor = ScenevoColors.Cue,
                                    uncheckedThumbColor = ScenevoColors.MistDim,
                                    uncheckedTrackColor = ScenevoColors.Line,
                                ),
                            )
                        }
                    }
                }
                itemsIndexed(
                    project.scenes.sortedBy { it.index },
                    key = { _, s -> s.id },
                ) { index, scene ->
                    SceneEditCard(
                        scene = scene,
                        canMoveUp = index > 0,
                        canMoveDown = index < project.scenes.lastIndex,
                        onMoveUp = { viewModel.moveScene(scene.id, -1) },
                        onMoveDown = { viewModel.moveScene(scene.id, 1) },
                        onNudgeDuration = { delta -> viewModel.nudgeDuration(scene.id, delta) },
                        onCycleMotion = { viewModel.cycleMotion(scene.id) },
                        onCycleTransition = { viewModel.cycleTransition(scene.id) },
                    )
                }
                val music = project.musicTrack
                if (music != null) {
                    item {
                        FilmPanel {
                            Text(
                                "Music · ${music.displayName}",
                                color = ScenevoColors.Mist,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "Ducked under voice · ${(music.volume * 100).toInt()}%",
                                color = ScenevoColors.MistDim,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Slider(
                                value = music.volume,
                                onValueChange = viewModel::setMusicVolume,
                                valueRange = 0.05f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ScenevoColors.Cue,
                                    activeTrackColor = ScenevoColors.Cue,
                                    inactiveTrackColor = ScenevoColors.Line,
                                ),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Sticky actions — always visible above nav bar.
            ScenevoPrimaryButton("Export video", onClick = { onExport(project.id) })
            Spacer(Modifier.height(8.dp))
            ScenevoSecondaryButton("Kembali", onClick = onBack)
        }
    }
}

@Composable
private fun SceneEditCard(
    scene: Scene,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onNudgeDuration: (Long) -> Unit,
    onCycleMotion: () -> Unit,
    onCycleTransition: () -> Unit,
) {
    FilmPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SCENE %02d".format(scene.index + 1),
                style = MaterialTheme.typography.labelMedium,
                color = ScenevoColors.Cue,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                Text("↑", color = if (canMoveUp) ScenevoColors.Mist else ScenevoColors.Line)
            }
            TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                Text("↓", color = if (canMoveDown) ScenevoColors.Mist else ScenevoColors.Line)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(scene.text, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.Mist)
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = { onNudgeDuration(-500L) }) {
                Text("−0.5s", color = ScenevoColors.MistDim)
            }
            Text(
                "%.1fs".format(scene.durationMs / 1000f),
                color = ScenevoColors.CueHot,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(48.dp),
            )
            TextButton(onClick = { onNudgeDuration(500L) }) {
                Text("+0.5s", color = ScenevoColors.MistDim)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionChip(
                label = scene.motion.name.lowercase().replace('_', ' '),
                onClick = onCycleMotion,
                modifier = Modifier.weight(1f),
            )
            ActionChip(
                label = "fx · ${scene.transition.name.lowercase().replace('_', ' ')}",
                onClick = onCycleTransition,
                accent = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
