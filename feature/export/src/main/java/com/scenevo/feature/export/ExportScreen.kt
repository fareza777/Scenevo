package com.scenevo.feature.export

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.scenevo.core.designsystem.component.BrandMark
import com.scenevo.core.designsystem.component.ScenevoBackdrop
import com.scenevo.core.designsystem.component.ScenevoPrimaryButton
import com.scenevo.core.designsystem.component.ScenevoSecondaryButton
import com.scenevo.core.designsystem.component.ScreenSection
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.domain.model.RenderStatus

@Composable
fun ExportRoute(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val animatedProgress by animateFloatAsState(targetValue = state.progress, label = "exportProgress")
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startIfNeeded()
    }

    ScenevoBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BrandMark(compact = true)
            Spacer(Modifier.height(8.dp))
            ScreenSection(
                eyebrow = "Export bay",
                title = "On-device render",
                body = "Visuals, ducked music, burn-in subtitle, MP4 + SRT — semua di HP kamu.",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ScenevoColors.Panel)
                    .border(1.dp, ScenevoColors.Line, RoundedCornerShape(18.dp))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = ScenevoColors.Cue,
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ScenevoColors.Cue,
                        trackColor = ScenevoColors.Line,
                        strokeCap = StrokeCap.Round,
                    )
                    Text(state.message, style = MaterialTheme.typography.bodyLarge, color = ScenevoColors.Mist)
                    state.publishMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = ScenevoColors.Signal)
                    }
                    state.outputPath?.let {
                        Text("File", style = MaterialTheme.typography.labelMedium, color = ScenevoColors.Cue)
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = ScenevoColors.MistDim)
                    }
                    state.error?.let {
                        Text(it, color = ScenevoColors.Danger)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            when (state.status) {
                RenderStatus.COMPLETED -> {
                    if (state.shareReady) {
                        ScenevoPrimaryButton(
                            text = "Bagikan video",
                            onClick = {
                                viewModel.consumeShareIntent()?.let { intent ->
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share Scenevo export"),
                                    )
                                }
                            },
                        )
                    }
                    ScenevoSecondaryButton("Selesai", onClick = onBack)
                }
                RenderStatus.FAILED, RenderStatus.CANCELLED -> {
                    ScenevoPrimaryButton("Kembali", onClick = onBack)
                }
                else -> {
                    ScenevoSecondaryButton("Tutup", onClick = onBack)
                }
            }
        }
    }
}
