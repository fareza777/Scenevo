package com.scenevo.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scenevo.feature.create.CreateRoute
import com.scenevo.feature.editor.EditorRoute
import com.scenevo.feature.export.ExportRoute
import com.scenevo.feature.home.HomeRoute
import com.scenevo.feature.settings.SettingsRoute

object Routes {
    const val HOME = "home"
    const val CREATE = "create"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{projectId}"
    const val EXPORT = "export/{projectId}"

    fun editor(projectId: String) = "editor/$projectId"
    fun export(projectId: String) = "export/$projectId"
}

@Composable
fun ScenevoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                onCreateProject = { navController.navigate(Routes.CREATE) },
                onOpenProject = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CREATE) {
            CreateRoute(
                onDone = { id ->
                    navController.navigate(Routes.editor(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            EditorRoute(
                onExport = { id -> navController.navigate(Routes.export(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            ExportRoute(onBack = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
    }
}
