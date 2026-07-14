package com.scenevo.feature.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scenevo.core.designsystem.component.BrandMark
import com.scenevo.core.designsystem.component.ScenevoBackdrop
import com.scenevo.core.designsystem.component.ScenevoPrimaryButton
import com.scenevo.core.designsystem.component.ScenevoSecondaryButton
import com.scenevo.core.designsystem.component.ScreenSection
import com.scenevo.core.designsystem.component.StepChipRow
import com.scenevo.core.designsystem.theme.ScenevoColors

@Composable
fun CreateRoute(
    onDone: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickVisuals = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        viewModel.attachVisuals(uris.map { it.toString() })
    }
    val pickMusic = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.attachMusic(uri.toString())
    }

    ScenevoBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BrandMark(compact = true)
            Spacer(Modifier.height(16.dp))
            StepChipRow(
                steps = listOf("Script", "Scenes", "Visuals", "Voice", "Music"),
                currentIndex = state.step,
            )
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 5 }) togetherWith
                        (fadeOut() + slideOutHorizontally { -it / 5 })
                },
                modifier = Modifier.weight(1f),
                label = "createStep",
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (step) {
                        0 -> {
                            ScreenSection(
                                eyebrow = "Step 01",
                                title = "Tulis naskah",
                                body = "Scene splitting jalan lokal — tanpa cloud.",
                            )
                            ScenevoField(
                                value = state.title,
                                onValueChange = viewModel::updateTitle,
                                label = "Judul montage",
                                singleLine = true,
                            )
                            ScenevoField(
                                value = state.script,
                                onValueChange = viewModel::updateScript,
                                label = "Script / naskah",
                                modifier = Modifier.height(220.dp),
                            )
                            ScenevoPrimaryButton("Pecah jadi scenes", onClick = viewModel::splitScenes)
                        }
                        1 -> {
                            ScreenSection(
                                eyebrow = "Step 02",
                                title = "Scene plan",
                                body = "${state.scenes.size} beat sudah dijadwalkan.",
                            )
                            state.scenes.forEachIndexed { index, scene ->
                                SceneBlock(
                                    index = index + 1,
                                    text = scene.text,
                                    durationLabel = "${"%.1f".format(scene.durationMs / 1000f)}s",
                                )
                            }
                            ScenevoPrimaryButton("Lanjut visuals", onClick = viewModel::nextStep)
                            ScenevoSecondaryButton("Kembali", onClick = viewModel::prevStep)
                        }
                        2 -> {
                            ScreenSection(
                                eyebrow = "Step 03",
                                title = "Visual lokal + stock",
                                body = "Galeri offline, atau Pexels (consent + Wi‑Fi di Settings).",
                            )
                            StatPanel(
                                value = "${state.attachedCount}",
                                label = "file terpasang",
                            )
                            ScenevoPrimaryButton(
                                "Pilih dari galeri",
                                onClick = { pickVisuals.launch(arrayOf("image/*", "video/*")) },
                            )
                            ScenevoField(
                                value = state.stockQuery,
                                onValueChange = viewModel::updateStockQuery,
                                label = "Cari stock (Pexels)",
                                singleLine = true,
                            )
                            ScenevoSecondaryButton(
                                if (state.isSearchingStock) "Searching…" else "Cari stock",
                                onClick = viewModel::searchStock,
                                enabled = !state.isSearchingStock && !state.isCachingStock,
                            )
                            state.stockResults.take(6).forEach { photo ->
                                ScenevoSecondaryButton(
                                    text = "${photo.photographer} · ${photo.width}×${photo.height}",
                                    onClick = { viewModel.attachStock(photo) },
                                    enabled = !state.isCachingStock,
                                )
                            }
                            if (state.isCachingStock) {
                                Text("Caching stock to device…", color = ScenevoColors.Signal)
                            }
                            ScenevoPrimaryButton(
                                "Lanjut voice",
                                onClick = viewModel::continueFromVisuals,
                                enabled = state.attachedCount > 0 && !state.isCachingStock,
                            )
                            ScenevoSecondaryButton("Kembali", onClick = viewModel::prevStep)
                        }
                        3 -> {
                            ScreenSection(
                                eyebrow = "Step 04",
                                title = "Narasi offline",
                                body = "Opsional. Android TTS / Piper — bisa dilewati.",
                            )
                            if (state.voiceStatus != null) {
                                Text(state.voiceStatus!!, color = ScenevoColors.Signal)
                            }
                            ScenevoPrimaryButton(
                                if (state.isSynthesizing) "Generating…" else "Generate TTS offline",
                                onClick = viewModel::synthesizeVoice,
                                enabled = !state.isSynthesizing,
                            )
                            ScenevoPrimaryButton("Lanjut music", onClick = viewModel::nextStep)
                            ScenevoSecondaryButton("Kembali", onClick = viewModel::prevStep)
                        }
                        else -> {
                            ScreenSection(
                                eyebrow = "Step 05",
                                title = "Musik latar",
                                body = "Opsional. Volume di-duck di bawah narasi saat export.",
                            )
                            StatPanel(
                                value = state.musicTrack?.displayName ?: "—",
                                label = "track terpilih",
                            )
                            if (state.musicTrack != null) {
                                Text(
                                    "Volume ${(state.musicVolume * 100).toInt()}%",
                                    color = ScenevoColors.MistDim,
                                )
                                Slider(
                                    value = state.musicVolume,
                                    onValueChange = viewModel::setMusicVolume,
                                    valueRange = 0.05f..0.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ScenevoColors.Cue,
                                        activeTrackColor = ScenevoColors.Cue,
                                        inactiveTrackColor = ScenevoColors.Line,
                                    ),
                                )
                            }
                            ScenevoPrimaryButton(
                                "Pilih audio",
                                onClick = { pickMusic.launch(arrayOf("audio/*")) },
                            )
                            ScenevoPrimaryButton(
                                "Simpan & buka editor",
                                onClick = { viewModel.saveProject(onDone) },
                                enabled = !state.isSaving,
                            )
                            ScenevoSecondaryButton("Lewati musik & simpan", onClick = {
                                viewModel.clearMusic()
                                viewModel.saveProject(onDone)
                            })
                            ScenevoSecondaryButton("Kembali", onClick = viewModel::prevStep)
                        }
                    }

                    if (state.error != null) {
                        Text(state.error!!, color = ScenevoColors.Danger)
                    }
                    ScenevoSecondaryButton("Batalkan", onClick = onBack)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ScenevoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ScenevoColors.Cue,
            unfocusedBorderColor = ScenevoColors.Line,
            focusedLabelColor = ScenevoColors.Cue,
            unfocusedLabelColor = ScenevoColors.MistDim,
            cursorColor = ScenevoColors.Cue,
            focusedTextColor = ScenevoColors.Mist,
            unfocusedTextColor = ScenevoColors.Mist,
            focusedContainerColor = ScenevoColors.Panel,
            unfocusedContainerColor = ScenevoColors.Panel,
        ),
    )
}

@Composable
private fun SceneBlock(index: Int, text: String, durationLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScenevoColors.Panel)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "SCENE %02d · %s".format(index, durationLabel),
            style = MaterialTheme.typography.labelMedium,
            color = ScenevoColors.Cue,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.Mist)
    }
}

@Composable
private fun StatPanel(value: String, label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScenevoColors.Panel)
            .border(1.dp, ScenevoColors.Line, RoundedCornerShape(14.dp))
            .padding(18.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = ScenevoColors.Mist)
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = ScenevoColors.MistDim)
    }
}
