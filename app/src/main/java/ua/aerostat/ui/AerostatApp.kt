package ua.aerostat.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ua.aerostat.ui.screens.EditorScreen
import ua.aerostat.ui.screens.HomeScreen
import ua.aerostat.ui.screens.InverseScreen
import ua.aerostat.ui.screens.Loading
import ua.aerostat.ui.screens.ProjectsScreen
import ua.aerostat.ui.screens.ReferenceScreen
import ua.aerostat.ui.screens.ResultsScreen
import ua.aerostat.ui.screens.SettingsScreen
import ua.aerostat.ui.screens.WizardScreen
import ua.aerostat.ui.theme.AerostatTheme
import ua.aerostat.ui.theme.ThemeMode

private enum class TopDest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", Uk.NAV_HOME, Icons.Filled.Home),
    PROJECTS("projects", Uk.NAV_PROJECTS, Icons.AutoMirrored.Filled.List),
    REFERENCE("reference", Uk.NAV_REFERENCE, Icons.Filled.Info),
    SETTINGS("settings", Uk.NAV_SETTINGS, Icons.Filled.Settings),
}

private const val WIZARD = "wizard"
private const val INVERSE = "inverse"
private const val EDITOR = "editor/{id}"
private const val RESULTS = "results/{id}"

@Composable
fun AerostatApp(onThemeResolved: (Boolean) -> Unit = {}, vm: AppViewModel = viewModel()) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    AerostatTheme(themeMode) {
        val dark = ua.aerostat.ui.theme.isDark(themeMode)
        SideEffect { onThemeResolved(dark) }
        AppScaffold(vm, themeMode)
    }
}

@Composable
private fun AppScaffold(vm: AppViewModel, themeMode: ThemeMode) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val isTop = TopDest.entries.any { it.route == route } || route == null

    fun goTop(d: TopDest) = nav.navigate(d.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Row(Modifier.fillMaxSize()) {
        if (wide) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.testTag("nav_rail")) {
                TopDest.entries.forEach { d ->
                    NavigationRailItem(
                        selected = route == d.route,
                        onClick = { goTop(d) },
                        icon = { Icon(d.icon, contentDescription = null) },
                        label = { Text(d.label) },
                        modifier = Modifier.testTag("nav_${d.route}"),
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!wide && isTop) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.testTag("nav_bar")) {
                        TopDest.entries.forEach { d ->
                            NavigationBarItem(
                                selected = route == d.route,
                                onClick = { goTop(d) },
                                icon = { Icon(d.icon, contentDescription = null) },
                                label = { Text(d.label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.sp)) },
                                modifier = Modifier.testTag("nav_${d.route}"),
                            )
                        }
                    }
                }
            },
        ) { inner ->
            AppNavHost(nav, vm, themeMode, Modifier.padding(inner).consumeWindowInsets(inner))
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, vm: AppViewModel, themeMode: ThemeMode, modifier: Modifier) {
    val projects = vm.projects
    NavHost(nav, startDestination = TopDest.HOME.route, modifier = modifier) {
        composable(TopDest.HOME.route) {
            HomeScreen(
                projects = projects,
                onNewProject = { nav.navigate(WIZARD) },
                onInverse = { nav.navigate(INVERSE) },
                onOpenProject = { nav.navigate("editor/$it") },
                onAllProjects = {
                    nav.navigate(TopDest.PROJECTS.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(TopDest.PROJECTS.route) {
            ProjectsScreen(
                projects = projects,
                onOpen = { nav.navigate("editor/$it") },
                onNew = { nav.navigate(WIZARD) },
                onDuplicate = { vm.duplicate(it) },
                onDelete = { vm.delete(it) },
            )
        }
        composable(TopDest.REFERENCE.route) { ReferenceScreen() }
        composable(TopDest.SETTINGS.route) { SettingsScreen(themeMode, vm::setThemeMode) }
        composable(INVERSE) {
            InverseScreen(
                input = vm.inverse,
                onChange = vm::updateInverse,
                onCreateProject = { pi ->
                    val id = vm.createFromInput(pi)
                    nav.navigate("editor/$id") { popUpTo(INVERSE) { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(WIZARD) {
            if (projects == null) Loading() else WizardScreen(
                defaultName = vm.nextDefaultName(),
                onCreate = { name, shape, variant ->
                    val id = vm.create(name, shape, variant)
                    nav.navigate("editor/$id") { popUpTo(WIZARD) { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(EDITOR) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            if (projects == null) Loading() else EditorScreen(
                project = projects.firstOrNull { it.id == id },
                onBack = { nav.popBackStack() },
                onInputChange = { t -> vm.updateInput(id, t) },
                onRename = { vm.rename(id, it) },
                onOpenResults = { nav.navigate("results/$id") },
            )
        }
        composable(RESULTS) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            if (projects == null) Loading() else ResultsScreen(
                project = projects.firstOrNull { it.id == id },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
