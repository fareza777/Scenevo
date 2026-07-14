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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val extras by viewModel.extras.collectAsStateWithLifecycle()
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
                    text = "RECENT CUTS  ·  long-press for actions",
                    color = ScenevoColors.MistDim,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(projects, key = { _, p -> p.id }) { _, project ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(motion.fade) + slideInVertically { it / 8 },
                        ) {
                            ProjectTile(
                                title = project.title,
                                meta = projectMeta(project),
                                status = project.status.name,
                                onClick = { onOpenProject(project.id) },
                                onLongClick = { viewModel.openActions(project) },
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

    extras.actionTarget?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::dismissActions,
            title = { Text(project.title) },
            text = { Text("Duplicate untuk variasi, atau hapus montage dari perangkat.") },
            confirmButton = {
                TextButton(onClick = viewModel::duplicateFromActions) {
                    Text("Duplicate", color = ScenevoColors.CueHot)
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = viewModel::requestDeleteFromActions) {
                        Text("Hapus", color = ScenevoColors.Danger)
                    }
                    TextButton(onClick = viewModel::dismissActions) {
                        Text("Batal", color = ScenevoColors.MistDim)
                    }
                }
            },
            containerColor = ScenevoColors.Panel,
            titleContentColor = ScenevoColors.Mist,
            textContentColor = ScenevoColors.MistDim,
        )
    }

    extras.pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Hapus montage?") },
            text = {
                Text("\"${project.title}\" akan dihapus dari perangkat. File export di Movies/Scenevo tetap ada.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Hapus", color = ScenevoColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text("Batal", color = ScenevoColors.MistDim)
                }
            },
            containerColor = ScenevoColors.Panel,
            titleContentColor = ScenevoColors.Mist,
            textContentColor = ScenevoColors.MistDim,
        )
    }
}

private fun projectMeta(project: Project): String {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(project.updatedAt))
    return "${project.scenes.size} scenes · ${project.aspectRatio.label} · $whenText"
}
