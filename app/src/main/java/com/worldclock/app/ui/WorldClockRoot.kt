package com.worldclock.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.worldclock.app.R
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.repository.PrefsError
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.ui.components.WorldClockBottomBar
import com.worldclock.app.ui.components.WorldClockTab
import com.worldclock.app.ui.mapping.buildPlannerUiState
import com.worldclock.app.ui.navigation.Routes
import com.worldclock.app.ui.screens.CityDetailScreen
import com.worldclock.app.ui.screens.CityLabelDialog
import com.worldclock.app.ui.screens.CitySearchSheet
import com.worldclock.app.ui.screens.ClocksScreen
import com.worldclock.app.ui.screens.MeetingPlannerScreen
import com.worldclock.app.ui.screens.SettingsSheet
import com.worldclock.app.ui.screens.WorldMapScreen
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockTheme

/**
 * App shell: bottom navigation, the navigation graph, the city search sheet and
 * the city label dialog.
 *
 * The search action opens a bottom sheet; Clocks and Map are destinations.
 * Selecting a city opens its detail screen.
 */
@Composable
fun WorldClockRoot(
    modifier: Modifier = Modifier,
    viewModel: WorldClockViewModel = viewModel(factory = WorldClockViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearchSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    // City whose label is being edited. The id rather than the label itself, so
    // the dialog always opens on the stored value.
    var labelDialogCityId by rememberSaveable { mutableStateOf<String?>(null) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedTab = if (currentRoute == Routes.MAP) WorldClockTab.Map else WorldClockTab.Clocks

    val readErrorMessage = stringResource(R.string.error_storage_read)
    val writeErrorMessage = stringResource(R.string.error_storage_write)

    // Persistence failures are surfaced as a non-blocking message.
    LaunchedEffect(viewModel) {
        viewModel.errors.collect { error ->
            val message = when (error) {
                PrefsError.ReadFailed -> readErrorMessage
                PrefsError.WriteFailed -> writeErrorMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateToTab: (WorldClockTab) -> Unit = { tab ->
        val route = when (tab) {
            WorldClockTab.Clocks -> Routes.CLOCKS
            WorldClockTab.Map -> Routes.MAP
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The shell is a stack so the label dialog can sit above the bottom bar as
    // well as the current destination.
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = WorldClockColors.Background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                WorldClockBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = navigateToTab,
                    onSearchClick = { showSearchSheet = true },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.CLOCKS,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable(Routes.CLOCKS) {
                    ClocksScreen(
                        state = state,
                        onCityClick = { city ->
                            viewModel.selectCity(city.id)
                            navController.navigate(Routes.cityDetail(city.id))
                        },
                        onAddClick = { showSearchSheet = true },
                        onFindCityClick = { showSearchSheet = true },
                        onSettingsClick = { showSettingsSheet = true },
                        onPlanMeetingClick = { navController.navigate(Routes.PLANNER) },
                    )
                }

                composable(Routes.PLANNER) {
                    // The planner opens on "now" so it agrees with the Clocks
                    // screen, and is the user's from the first drag. Null means
                    // "not touched yet"; it is saved so a rotation does not hand
                    // a chosen slot back to the clock.
                    var plannerMinute by rememberSaveable { mutableStateOf<Int?>(null) }
                    val zone = viewModel.deviceZone
                    val minuteOfDay = MinuteOfDay(
                        plannerMinute ?: MinuteOfDay
                            .ofInstant(state.now, zone)
                            .snappedToGrid()
                            .value,
                    )

                    MeetingPlannerScreen(
                        planner = buildPlannerUiState(
                            state = state,
                            minuteOfDay = minuteOfDay,
                            deviceZoneId = zone,
                        ),
                        onMinuteChange = { plannerMinute = it.value },
                        onBack = { navController.popBackStack() },
                        onFindCityClick = { showSearchSheet = true },
                    )
                }

                composable(Routes.MAP) {
                    WorldMapScreen(
                        state = state,
                        onSelectCity = viewModel::selectCity,
                        onOpenCity = { cityId ->
                            viewModel.selectCity(cityId)
                            navController.navigate(Routes.cityDetail(cityId))
                        },
                        onAddClick = { showSearchSheet = true },
                        onFindCityClick = { showSearchSheet = true },
                    )
                }

                composable(
                    route = Routes.CITY_DETAIL,
                    arguments = listOf(navArgument(Routes.CITY_ID_ARG) { type = NavType.StringType }),
                ) { entry ->
                    val cityId = entry.arguments?.getString(Routes.CITY_ID_ARG).orEmpty()
                    val city = CityDirectory.find(cityId)
                    if (city == null) {
                        // Unknown id (should not happen): leave the screen.
                        LaunchedEffect(cityId) { navController.popBackStack() }
                    } else {
                        CityDetailScreen(
                            city = city,
                            now = state.now,
                            use24Hour = state.use24Hour,
                            deviceLocal = state.deviceLocal,
                            label = state.cities.firstOrNull { it.id == cityId }?.label,
                            onBack = { navController.popBackStack() },
                            onToggleFormat = viewModel::setUse24Hour,
                            onEditLabel = { labelDialogCityId = cityId },
                            onViewOnMap = { navigateToTab(WorldClockTab.Map) },
                            onRemove = {
                                viewModel.removeCity(cityId)
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }

        if (showSearchSheet) {
            CitySearchSheet(
                now = state.now,
                use24Hour = state.use24Hour,
                savedCityIds = state.cities.map { it.id }.toSet(),
                onAddCity = { cityId ->
                    viewModel.addCity(cityId)
                    showSearchSheet = false
                    navigateToTab(WorldClockTab.Clocks)
                },
                onDismiss = { showSearchSheet = false },
            )
        }

        if (showSettingsSheet) {
            SettingsSheet(
                theme = state.theme,
                onThemeSelected = viewModel::setTheme,
                onDismiss = { showSettingsSheet = false },
            )
        }

        // Resolved from the live list, so the dialog closes itself if the city is
        // removed while it is open.
        val labelDialogCity = labelDialogCityId?.let { id ->
            state.cities.firstOrNull { it.id == id }
        }
        if (labelDialogCity != null) {
            CityLabelDialog(
                cityName = labelDialogCity.name,
                currentLabel = labelDialogCity.label,
                onSave = { label ->
                    viewModel.setCityLabel(labelDialogCity.id, label)
                    labelDialogCityId = null
                },
                onDismiss = { labelDialogCityId = null },
            )
        }
    }
}

@Preview(name = "App shell 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WorldClockRootPreview() {
    WorldClockTheme {
        WorldClockRoot()
    }
}
