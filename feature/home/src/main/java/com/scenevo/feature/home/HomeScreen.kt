package com.scenevo.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scenevo.core.designsystem.component.AtmosphereHeader
import com.scenevo.core.designsystem.component.EmptyState
import com.scenevo.core.designsystem.component.ProjectTile
import com.scenevo.core.designsystem.component.ScenevoBackdrop
import com.scenevo.core.designsystem.component.ScenevoPrimaryButton
import com.scenevo.core.designsystem.theme.LocalScenevoMotion
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.domain.model.Project
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeRoute(
    onCreateProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val motion = LocalScenevoMotion.current

    ScenevoBackdrop {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            AtmosphereHeader(
                title = "Cut montages on device.",
                subtitle = "Naskah → scene → suara → export. Tanpa kredit render, tanpa upload wajib.",
                trailing = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = ScenevoColors.MistDim,
                        )
                    }
                },
            )

            if (projects.isEmpty()) {
                EmptyState(
                    title = "Belum ada montage",
                    body = "Mulai dari naskah. Scenevo menyusun scene secara lokal — privacy-first.",
                    actionLabel = "Buat montage baru",
                    onAction = onCreateProject,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = "RECENT CUTS",
                    color = ScenevoColors.MistDim,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(projects, key = { _, p -> p.id }) { index, project ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(motion.fade) + slideInVertically { it / 8 },
                        ) {
                            ProjectTile(
                                title = project.title,
                                meta = projectMeta(project),
                                status = project.status.name,
                                onClick = { onOpenProject(project.id) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                ScenevoPrimaryButton(
                    text = if (projects.isEmpty()) "Mulai sekarang" else "Montage baru",
                    onClick = onCreateProject,
                )
            }
        }
    }
}

private fun projectMeta(project: Project): String {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(project.updatedAt))
    return "${project.scenes.size} scenes · ${project.aspectRatio.label} · $whenText"
}
