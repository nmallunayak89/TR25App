package com.postaltools.tr25.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.postaltools.tr25.ui.edit.EditorScreen
import com.postaltools.tr25.ui.home.HomeScreen
import com.postaltools.tr25.ui.list.BillListScreen
import com.postaltools.tr25.ui.list.ListMode
import com.postaltools.tr25.ui.preview.PreviewScreen
import com.postaltools.tr25.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DIARY = "diary"
    fun list(mode: ListMode) = "list/${mode.name}"
    fun edit(id: Long, month: String = "") = "edit/$id?month=$month"
    fun preview(id: Long, action: String = "") = "preview/$id?action=$action"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNew = { nav.navigate(Routes.edit(0)) },
                onList = { nav.navigate(Routes.list(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onDiary = { nav.navigate(Routes.DIARY) },
            )
        }
        composable(
            "list/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = runCatching { ListMode.valueOf(entry.arguments?.getString("mode") ?: "") }
                .getOrDefault(ListMode.VIEW)
            BillListScreen(
                mode = mode,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(it)) },
                onPreview = { id, action -> nav.navigate(Routes.preview(id, action)) },
                onNew = { nav.navigate(Routes.edit(0)) },
            )
        }
        composable(
            "edit/{id}?month={month}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("month") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            EditorScreen(
                billId = id,
                month = entry.arguments?.getString("month") ?: "",
                onBack = { nav.popBackStack() },
                onGenerated = { savedId ->
                    // Replace the editor with the preview so "back" returns to where the user started.
                    nav.navigate(Routes.preview(savedId)) {
                        popUpTo("edit/{id}?month={month}") { inclusive = true }
                    }
                },
            )
        }
        composable(
            "preview/{id}?action={action}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("action") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            PreviewScreen(
                billId = entry.arguments?.getLong("id") ?: 0L,
                action = entry.arguments?.getString("action") ?: "",
                onBack = { nav.popBackStack() },
                onEdit = { id ->
                    nav.navigate(Routes.edit(id)) {
                        popUpTo("preview/{id}?action={action}") { inclusive = true }
                    }
                },
                onSavedDone = {
                    nav.navigate(Routes.list(ListMode.VIEW)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.DIARY) {
            com.postaltools.tr25.ui.diary.DiaryScreen(
                onBack = { nav.popBackStack() },
                onCreateBill = { m -> nav.navigate(Routes.edit(0, m)) },
                onReports = { m -> nav.navigate("reports/$m") },
            )
        }
        composable("reports/{month}", arguments = listOf(navArgument("month") { type = NavType.StringType })) { entry ->
            com.postaltools.tr25.ui.diary.ReportsScreen(
                month = entry.arguments?.getString("month") ?: "",
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
