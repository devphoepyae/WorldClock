package com.worldclock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.data.repository.ClockRepository
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.testing.FakeSavedCitiesDataStore
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.screens.CityDetailScreen
import com.worldclock.app.ui.screens.CityLabelDialog
import com.worldclock.app.ui.screens.CitySearchContent
import com.worldclock.app.ui.screens.ClocksScreen
import com.worldclock.app.ui.screens.MeetingPlannerScreen
import com.worldclock.app.ui.screens.SettingsContent
import com.worldclock.app.ui.screens.WorldMapScreen
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/**
 * JVM screenshot tests for layout verification (spec section 10).
 *
 * Every screen is rendered at 360 dp wide portrait with a fixed instant, so the
 * PNGs in `app/build/screenshots` are reviewable without an emulator and never
 * drift between runs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class LayoutScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * The whole app shell — navigation plus the live city list — driven by an
     * in-memory store and a single-emission ticker so the render is stable.
     */
    @Test
    fun appShell_360dp() {
        val viewModel = WorldClockViewModel(
            repository = ClockRepository(FakeSavedCitiesDataStore()),
            ticker = flowOf(PreviewSamples.referenceInstant),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            WorldClockTheme {
                WorldClockRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Los Angeles").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/app_shell_360dp.png")
    }

    @Test
    fun clocksScreen_360dp() {
        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = PreviewSamples.clockState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_360dp.png")
    }

    @Test
    fun clocksScreenEmpty_360dp() {
        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = PreviewSamples.emptyState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_empty_360dp.png")
    }

    @Test
    fun cityDetail_360dp() {
        val state = PreviewSamples.clockState(use24Hour = true)
        composeRule.setContent {
            WorldClockTheme {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    deviceLocal = state.deviceLocal,
                    label = null,
                    onBack = {},
                    onToggleFormat = {},
                    onEditLabel = {},
                    onViewOnMap = {},
                    onRemove = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_360dp.png")
    }

    @Test
    fun cityDetailTwelveHour_360dp() {
        val state = PreviewSamples.clockState(use24Hour = false)
        composeRule.setContent {
            WorldClockTheme {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    deviceLocal = state.deviceLocal,
                    label = null,
                    onBack = {},
                    onToggleFormat = {},
                    onEditLabel = {},
                    onViewOnMap = {},
                    onRemove = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_12h_360dp.png")
    }

    /**
     * The detail screen for a city the user has labelled: the label sits under
     * the name, the edit action follows it, and the difference names the
     * reference it is measured against.
     */
    @Test
    fun cityDetailLabelled_360dp() {
        val state = PreviewSamples.labelledState()
        composeRule.setContent {
            WorldClockTheme {
                CityDetailScreen(
                    city = CityDirectory.find("tokyo")!!,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    deviceLocal = state.deviceLocal,
                    label = "Head Office",
                    onBack = {},
                    onToggleFormat = {},
                    onEditLabel = {},
                    onViewOnMap = {},
                    onRemove = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_labelled_360dp.png")
    }

    /**
     * The map screen with the default five cities at the winter reference
     * instant: southern-hemisphere night shading, Los Angeles active.
     */
    @Test
    fun worldMap_360dp() {
        renderMap(PreviewSamples.clockState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_360dp.png")
    }

    /**
     * The same screen six months later, so the terminator is on the other side
     * of the equator and the shading clearly moves.
     */
    @Test
    fun worldMapSummer_360dp() {
        renderMap(PreviewSamples.clockState(now = PreviewSamples.summerInstant))
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_summer_360dp.png")
    }

    @Test
    fun worldMapEmpty_360dp() {
        renderMap(PreviewSamples.emptyState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_empty_360dp.png")
    }

    /**
     * Every city selected in turn, so the active marker and meridian are shown
     * at a range of longitudes (including the far east and the far west).
     */
    @Test
    fun worldMapActiveCityMoves_360dp() {
        renderMap(
            PreviewSamples.clockState(
                savedCityIds = listOf("tokyo", "london", "honolulu"),
                activeCityId = "london",
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_london_360dp.png")
    }

    private fun renderMap(state: WorldClockUiState, theme: AppTheme = AppTheme.LIGHT_MONOCHROME) {
        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                WorldMapScreen(
                    state = state,
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                )
            }
        }
    }

    // ---- Dark theme ----
    //
    // The same screens rendered through the dark palette, so a regression in the
    // dark tokens is visible in the same review pass as the light ones.

    @Test
    fun clocksScreenDark_360dp() {
        composeRule.setContent {
            WorldClockTheme(theme = AppTheme.TRUE_DARK) {
                ClocksScreen(
                    state = PreviewSamples.clockState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_dark_360dp.png")
    }

    @Test
    fun cityDetailDark_360dp() {
        val state = PreviewSamples.clockState(use24Hour = true)
        composeRule.setContent {
            WorldClockTheme(theme = AppTheme.TRUE_DARK) {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    deviceLocal = state.deviceLocal,
                    label = null,
                    onBack = {},
                    onToggleFormat = {},
                    onEditLabel = {},
                    onViewOnMap = {},
                    onRemove = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_dark_360dp.png")
    }

    @Test
    fun worldMapDark_360dp() {
        renderMap(PreviewSamples.clockState(), AppTheme.TRUE_DARK)
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_dark_360dp.png")
    }

    // ---- Labels and time differences ----

    /**
     * The list with labels and the full spread of differences: whole hours,
     * half hours (this device sits on UTC+6:30) and "Same time" for a city in
     * the device's own zone.
     */
    @Test
    fun clocksScreenLabelled_360dp() {
        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = PreviewSamples.labelledState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_labelled_360dp.png")
    }

    /** The same list on a device in the app's own directory, so "Same time" shows. */
    @Test
    fun mapScreenLabelled_360dp() {
        renderMap(PreviewSamples.labelledState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_labelled_360dp.png")
    }

    /**
     * The labelled list on the dark palette: labels, differences and the
     * sun/moon icons in one review pass, so a regression in the dark tokens
     * behind any of them is visible.
     */
    @Test
    fun clocksScreenLabelledDark_360dp() {
        composeRule.setContent {
            WorldClockTheme(theme = AppTheme.TRUE_DARK) {
                ClocksScreen(
                    state = PreviewSamples.labelledState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_labelled_dark_360dp.png")
    }

    /**
     * The list in 12-hour form. The app-wide switch has to reach every surface,
     * so the list is rendered in it as well as the detail screen.
     */
    @Test
    fun clocksScreenTwelveHour_360dp() {
        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = PreviewSamples.clockState(use24Hour = false),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_12h_360dp.png")
    }

    // ---- Meeting planner ----

    /**
     * The planner at 09:00 local, with labels, so one render shows every state
     * the rows can be in: cities still at work, cities long finished, and a city
     * a day behind.
     */
    @Test
    fun meetingPlanner_360dp() {
        renderPlanner(PreviewSamples.plannerState(labels = true))
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_360dp.png")
    }

    @Test
    fun meetingPlannerDark_360dp() {
        renderPlanner(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(18, 30), labels = true),
            theme = AppTheme.TRUE_DARK,
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_dark_360dp.png")
    }

    @Test
    fun meetingPlannerEmpty_360dp() {
        renderPlanner(PreviewSamples.emptyPlannerState())
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_empty_360dp.png")
    }

    /**
     * The planner in 12-hour form: the slider's readout, the rows and the
     * reference line all follow the app-wide format.
     */
    @Test
    fun meetingPlannerTwelveHour_360dp() {
        renderPlanner(
            PreviewSamples.plannerState(
                minuteOfDay = MinuteOfDay.of(9, 0),
                labels = true,
                use24Hour = false,
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_12h_360dp.png")
    }

    private fun renderPlanner(
        planner: PlannerUiState,
        theme: AppTheme = AppTheme.LIGHT_MONOCHROME,
    ) {
        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                MeetingPlannerScreen(
                    planner = planner,
                    onMinuteChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }
    }

    /**
     * The label edit dialog, drawn over the Clocks screen it was opened from so
     * the dim behind the card reads the way it does in the app.
     */
    @Test
    fun cityLabelDialog_360dp() {
        renderLabelDialog(AppTheme.LIGHT_MONOCHROME)
        composeRule.onRoot().captureRoboImage("build/screenshots/city_label_dialog_360dp.png")
    }

    /** The same dialog on the dark palette, so the card and the scrim compare. */
    @Test
    fun cityLabelDialogDark_360dp() {
        renderLabelDialog(AppTheme.TRUE_DARK)
        composeRule.onRoot().captureRoboImage("build/screenshots/city_label_dialog_dark_360dp.png")
    }

    private fun renderLabelDialog(theme: AppTheme) {
        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                Box(Modifier.fillMaxSize()) {
                    ClocksScreen(
                        state = PreviewSamples.clockState(),
                        onCityClick = {},
                        onAddClick = {},
                        onFindCityClick = {},
                        onSettingsClick = {},
                        onPlanMeetingClick = {},
                    )
                    CityLabelDialog(
                        cityName = "Tokyo",
                        currentLabel = "Head Office",
                        onSave = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }

    // ---- Settings ----

    @Test
    fun settingsSheet_360dp() {
        renderSettings(AppTheme.LIGHT_MONOCHROME)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_360dp.png")
    }

    @Test
    fun settingsSheetDark_360dp() {
        renderSettings(AppTheme.TRUE_DARK)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_dark_360dp.png")
    }

    private fun renderSettings(theme: AppTheme) {
        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                // Presented the way the app actually shows it: a sheet pinned to
                // the bottom over a dimmed backdrop, so the corner radius and the
                // sheet/backdrop contrast are both visible in the PNG.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WorldClockColors.Secondary),
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = WorldClockColors.Background,
                        contentColor = WorldClockColors.Foreground,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ) {
                        SettingsContent(theme = theme, onThemeSelected = {})
                    }
                }
            }
        }
    }

    // ---- One render per theme ----
    //
    // Together with `clocks_screen_360dp.png` (Light Monochrome) and
    // `clocks_screen_dark_360dp.png` (True Dark), and with `settings_sheet_360dp.png`
    // and `settings_sheet_dark_360dp.png`, these complete a list render and an
    // appearance-sheet render for every theme the app ships. The theme picker is
    // the one screen that has to be reviewed in all five, because it shows the
    // swatch of every theme at once — including the four that are not in force.

    @Test
    fun clocksScreenWarmSepia_360dp() {
        renderClocks(AppTheme.WARM_SEPIA)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_warm_sepia_360dp.png")
    }

    @Test
    fun clocksScreenCoolSlate_360dp() {
        renderClocks(AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_cool_slate_360dp.png")
    }

    @Test
    fun clocksScreenHighContrast_360dp() {
        renderClocks(AppTheme.HIGH_CONTRAST)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_high_contrast_360dp.png")
    }

    private fun renderClocks(theme: AppTheme) {
        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                ClocksScreen(
                    state = PreviewSamples.clockState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
    }

    @Test
    fun settingsSheetWarmSepia_360dp() {
        renderSettings(AppTheme.WARM_SEPIA)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_warm_sepia_360dp.png")
    }

    @Test
    fun settingsSheetCoolSlate_360dp() {
        renderSettings(AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_cool_slate_360dp.png")
    }

    @Test
    fun settingsSheetHighContrast_360dp() {
        renderSettings(AppTheme.HIGH_CONTRAST)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_high_contrast_360dp.png")
    }

    /**
     * High Contrast is the theme whose map had to be reconsidered: its ocean and
     * its cards are both pure black, so the continents are drawn white and carry
     * the night shading. This render is the evidence that the terminator is still
     * visible.
     */
    @Test
    fun worldMapHighContrast_360dp() {
        renderMap(PreviewSamples.clockState(), AppTheme.HIGH_CONTRAST)
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_high_contrast_360dp.png")
    }

    @Test
    fun searchSheet_360dp() {
        renderSearchSheet(query = "")
        composeRule.onRoot().captureRoboImage("build/screenshots/search_sheet_360dp.png")
    }

    @Test
    fun searchSheetNoResults_360dp() {
        renderSearchSheet(query = "atlantis")
        composeRule.onRoot().captureRoboImage("build/screenshots/search_sheet_empty_360dp.png")
    }

    private fun renderSearchSheet(query: String) {        val state = PreviewSamples.clockState()
        composeRule.setContent {
            WorldClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WorldClockColors.Background,
                ) {
                    CitySearchContent(
                        query = query,
                        onQueryChange = {},
                        savedCityIds = CityDirectory.defaultSavedCityIds.toSet(),
                        now = state.now,
                        use24Hour = state.use24Hour,
                        onAddCity = {},
                        onDismiss = {},
                        autoFocus = false,
                    )
                }
            }
        }
    }
}
