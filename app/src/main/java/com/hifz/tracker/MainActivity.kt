package com.hifz.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hifz.tracker.ui.HifzTheme
import com.hifz.tracker.ui.HomeScreen
import com.hifz.tracker.ui.LocalArabic
import com.hifz.tracker.ui.MemorizeScreen
import com.hifz.tracker.ui.MemorizedScreen
import com.hifz.tracker.ui.ReviewScreen
import com.hifz.tracker.ui.SettingsScreen
import com.hifz.tracker.ui.SurahDetailScreen
import com.hifz.tracker.ui.tr

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: HifzViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            HifzTheme {
                CompositionLocalProvider(
                    LocalArabic provides state.settings.arabic,
                    LocalLayoutDirection provides if (state.settings.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    HifzRoot(vm, state)
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun HifzRoot(vm: HifzViewModel, state: UiState) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(
        Tab("home", tr("Today", "اليوم"), Icons.Filled.Home),
        Tab("memorize", tr("Memorize", "الحفظ"), Icons.Filled.Edit),
        Tab("memorized", tr("Memorized", "المحفوظ"), Icons.Filled.Star),
        Tab("review", tr("Review", "المراجعة"), Icons.Filled.Refresh),
        Tab("settings", tr("Settings", "الإعدادات"), Icons.Filled.Settings),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route?.startsWith("surah") != true) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (!state.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val openSurah: (Int) -> Unit = { id -> nav.navigate("surah/$id") }
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(state, vm, onOpenSurah = openSurah, onOpenReview = { nav.goTab("review") })
            }
            composable("memorize") { MemorizeScreen(state, onOpenSurah = openSurah) }
            composable("memorized") { MemorizedScreen(state, onOpenSurah = openSurah) }
            composable("review") { ReviewScreen(state, vm) }
            composable("settings") { SettingsScreen(state, vm) }
            composable(
                "surah/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 1
                SurahDetailScreen(id, state, vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
