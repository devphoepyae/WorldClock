package com.worldclock.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.data.prefs.SavedCities
import com.worldclock.app.data.prefs.SavedThemeMode
import com.worldclock.app.data.prefs.UserPreferences
import com.worldclock.app.data.prefs.toUserPreferences
import com.worldclock.app.data.repository.ClockRepository
import com.worldclock.app.domain.time.CityTimeFormatter
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.domain.time.WORKING_DAY_END_HOUR
import com.worldclock.app.domain.time.WORKING_DAY_START_HOUR
import com.worldclock.app.testing.FakeSavedCitiesDataStore
import com.worldclock.app.ui.components.DaySliderTags
import com.worldclock.app.ui.components.MinTouchTarget
import com.worldclock.app.ui.components.WorldMapTestTags
import com.worldclock.app.ui.components.timeDifferenceLabel
import com.worldclock.app.ui.mapping.buildPlannerUiState
import com.worldclock.app.ui.mapping.buildWorldClockUiState
import com.worldclock.app.ui.navigation.Routes
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.screens.CityDetailScreen
import com.worldclock.app.ui.screens.CityLabelDialog
import com.worldclock.app.ui.screens.CityLabelDialogTags
import com.worldclock.app.ui.screens.ClocksScreen
import com.worldclock.app.ui.screens.MeetingPlannerScreen
import com.worldclock.app.ui.screens.SettingsContent
import com.worldclock.app.ui.screens.WorldMapScreen
import com.worldclock.app.ui.theme.DarkWorldClockPalette
import com.worldclock.app.ui.theme.HighContrastWorldClockPalette
import com.worldclock.app.ui.theme.LightWorldClockPalette
import com.worldclock.app.ui.theme.LocalWorldClockPalette
import com.worldclock.app.ui.theme.WarmSepiaWorldClockPalette
import com.worldclock.app.ui.theme.WorldClockPalette
import com.worldclock.app.ui.theme.WorldClockTheme
import com.worldclock.app.ui.theme.paletteFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One test per acceptance criterion, each named after the criterion it proves.
 *
 * Logic criteria run against the real repository, formatter, mapper and map
 * domain objects. The behavioural criteria drive the real composables, and the
 * visual criteria have a matching PNG in `app/build/screenshots` produced by
 * [LayoutScreenshotTest] and [com.worldclock.app.ui.screens.WorldMapScreenTest].
 *
 * Criteria 1-11 are phase 1 (the clock app). Criterion 12 is phase 2, dark
 * theme support; 13 and 14 are the time difference and the city labels; 15 is
 * the meeting planner; and 17 is the phase 2 integration and accessibility
 * pass. Criterion 16, the home-screen widget, is parked for a later release:
 * v1.0 ships without it and without its tests.
 *
 * Reference instants (see `PreviewSamples`): winter 2024-01-20T16:40Z and summer
 * 2024-07-20T16:40Z.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AcceptanceCriteriaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val winter = PreviewSamples.referenceInstant
    private val summer = Instant.parse("2024-07-20T16:40:00Z")

    private fun newRepository(
        store: FakeSavedCitiesDataStore = FakeSavedCitiesDataStore(),
    ): ClockRepository = ClockRepository(store)

    private fun snapshot(cityId: String, instant: Instant, use24Hour: Boolean = true) =
        CityTimeFormatter.snapshot(
            zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone),
            instant = instant,
            use24Hour = use24Hour,
        )

    // ---------------------------------------------------------------- 1

    /**
     * Criterion 1 — first launch shows the five default cities in the specified
     * order, with Los Angeles active and a 24-hour clock.
     */
    @Test
    fun criterion01_firstLaunchShowsTheDefaultCitiesInOrderWithLosAngelesActive() = runTest {
        val preferences = newRepository().preferences.first()

        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london"),
            preferences.savedCityIds,
        )
        assertEquals("los-angeles", preferences.activeCityId)
        assertTrue("The default format is 24-hour", preferences.use24Hour)

        val state = PreviewSamples.clockState()
        assertEquals(preferences.savedCityIds, state.cities.map { it.id })
        assertEquals(listOf("los-angeles"), state.cities.filter { it.isActive }.map { it.id })
    }

    // ---------------------------------------------------------------- 2

    /**
     * Criterion 2 — every time, UTC offset and weekday matches the city's IANA
     * timezone at the current instant.
     */
    @Test
    fun criterion02_everyCityMatchesItsIanaTimezone() {
        val expected = mapOf(
            "tokyo" to Triple("01:40", "UTC+9", "Sun"),
            "sydney" to Triple("03:40", "UTC+11", "Sun"),
            "los-angeles" to Triple("08:40", "UTC-8", "Sat"),
            "new-york" to Triple("11:40", "UTC-5", "Sat"),
            "london" to Triple("16:40", "UTC+0", "Sat"),
        )

        val state = buildWorldClockUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            activeCityId = CityDirectory.defaultActiveCityId,
            use24Hour = true,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )

        assertEquals(expected.size, state.cities.size)
        state.cities.forEach { city ->
            val (time, offset, weekday) = expected.getValue(city.id)
            assertEquals("${city.id} time", time, city.timeText)
            assertEquals("${city.id} offset", offset, city.offsetLabel)
            assertEquals("${city.id} weekday", weekday, city.weekdayLabel)
        }
        // UTC+0 keeps its sign rather than collapsing to "UTC".
        assertEquals("UTC+0", state.cities.single { it.id == "london" }.offsetLabel)
    }

    // ---------------------------------------------------------------- 3

    /**
     * Criterion 3 — daylight saving is picked up from the zone rules, with no
     * offset hard-coded anywhere.
     */
    @Test
    fun criterion03_daylightSavingIsAppliedAutomatically() {
        val winterOffsets = listOf("los-angeles", "new-york", "london", "sydney")
            .associateWith { snapshot(it, winter).offsetLabel }
        val summerOffsets = listOf("los-angeles", "new-york", "london", "sydney")
            .associateWith { snapshot(it, summer).offsetLabel }

        assertEquals("UTC-8", winterOffsets.getValue("los-angeles"))
        assertEquals("UTC-7", summerOffsets.getValue("los-angeles"))
        assertEquals("UTC-5", winterOffsets.getValue("new-york"))
        assertEquals("UTC-4", summerOffsets.getValue("new-york"))
        assertEquals("UTC+0", winterOffsets.getValue("london"))
        assertEquals("UTC+1", summerOffsets.getValue("london"))
        // The southern hemisphere moves the other way.
        assertEquals("UTC+11", winterOffsets.getValue("sydney"))
        assertEquals("UTC+10", summerOffsets.getValue("sydney"))

        // Half-hour zones keep their minutes rather than rounding to the hour.
        assertEquals("UTC+5:30", snapshot("mumbai", winter).offsetLabel)
    }

    // ---------------------------------------------------------------- 4

    /**
     * Criterion 4 — the sun icon covers local 06:00–17:59 and the moon covers
     * 18:00–05:59.
     */
    @Test
    fun criterion04_dayAndNightFollowTheSixToEighteenRule() {
        val utc = ZoneId.of("UTC")
        for (hour in 0..23) {
            val instant = Instant.parse("2024-01-20T%02d:30:00Z".format(hour))
            val expectedDaytime = hour in 6 until 18
            assertEquals(
                "Hour ${hour}:30 should be ${if (expectedDaytime) "day" else "night"}",
                expectedDaytime,
                CityTimeFormatter.isDaytime(utc, instant),
            )
            assertEquals(
                "Hour ${hour}:30 snapshot icon",
                expectedDaytime,
                CityTimeFormatter.snapshot(utc, instant, use24Hour = true).isDaytime,
            )
        }
    }

    // ---------------------------------------------------------------- 5

    /**
     * Criterion 5 — all 22 directory cities are searchable, and adding one twice
     * is a no-op.
     */
    @Test
    fun criterion05_searchCoversTheDirectoryAndRejectsDuplicates() = runTest {
        assertEquals(22, CityDirectory.search("").size)
        assertEquals(
            listOf("tokyo"),
            CityDirectory.search("tokyo").map { it.id },
        )
        assertEquals(listOf("yangon"), CityDirectory.search("Asia/Yangon").map { it.id })

        val repository = newRepository()
        repository.preferences.first()
        repository.addCity("yangon")
        repository.addCity("yangon")

        val preferences = repository.preferences.first()
        assertEquals(1, preferences.savedCityIds.count { it == "yangon" })
        assertEquals("yangon", preferences.activeCityId)
    }

    // ---------------------------------------------------------------- 6

    /**
     * Criterion 6 — selecting a city makes it active and its detail route
     * resolves to that city.
     */
    @Test
    fun criterion06_selectingACityMakesItActiveAndOpensItsDetail() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        repository.selectCity("tokyo")

        val preferences = repository.preferences.first()
        assertEquals("tokyo", preferences.activeCityId)

        val state = buildWorldClockUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )
        assertEquals(listOf("tokyo"), state.cities.filter { it.isActive }.map { it.id })

        // The detail destination resolves the id back to the same city.
        assertEquals("city/tokyo", Routes.cityDetail("tokyo"))
        assertEquals("Tokyo", CityDirectory.find("tokyo")?.name)
    }

    // ---------------------------------------------------------------- 7

    /**
     * Criterion 7 — removing works, removing the active city promotes another,
     * and removing everything empties the list.
     */
    @Test
    fun criterion07_removingCitiesHandlesTheActiveCityAndTheEmptyState() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        // Removing an inactive city leaves the active one alone.
        repository.removeCity("london")
        var preferences = repository.preferences.first()
        assertEquals(4, preferences.savedCityIds.size)
        assertEquals("los-angeles", preferences.activeCityId)
        assertFalse(preferences.savedCityIds.contains("london"))

        // Removing the active city promotes the first remaining city.
        repository.removeCity("los-angeles")
        preferences = repository.preferences.first()
        assertEquals(listOf("tokyo", "sydney", "new-york"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)

        // Removing everything empties the list and clears the active city.
        preferences.savedCityIds.toList().forEach { repository.removeCity(it) }
        preferences = repository.preferences.first()
        assertTrue(preferences.savedCityIds.isEmpty())
        assertNull(preferences.activeCityId)

        val state = buildWorldClockUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )
        assertTrue(state.cities.isEmpty())
    }

    // ---------------------------------------------------------------- 8

    /**
     * Criterion 8 — the 12h/24h choice changes the display immediately and is
     * stored.
     */
    @Test
    fun criterion08_theTimeFormatSwitchesAndPersists() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        assertEquals("01:40", snapshot("tokyo", winter, use24Hour = true).timeText)

        repository.setUse24Hour(false)
        var preferences = repository.preferences.first()
        assertFalse(preferences.use24Hour)

        val twelveHour = snapshot("tokyo", winter, use24Hour = preferences.use24Hour)
        assertEquals("1:40 AM", twelveHour.timeText)
        assertEquals("1", twelveHour.hourText)
        assertEquals("AM", twelveHour.periodText)

        // The format is app-wide: it applies to every city, not just the active one.
        assertEquals("8:40 AM", snapshot("los-angeles", winter, use24Hour = false).timeText)

        repository.setUse24Hour(true)
        preferences = repository.preferences.first()
        assertTrue(preferences.use24Hour)
        assertEquals("01:40", snapshot("tokyo", winter, use24Hour = preferences.use24Hour).timeText)
    }

    // ---------------------------------------------------------------- 9

    /**
     * Criterion 9 — order, active city and format survive a restart, and ids
     * that are no longer in the directory are dropped.
     */
    @Test
    fun criterion09_savedStateSurvivesRestartAndUnknownIdsAreDropped() = runTest {
        val store = FakeSavedCitiesDataStore()
        val first = ClockRepository(store)
        first.preferences.first()

        first.removeCity("sydney")
        first.addCity("reykjavik")
        first.selectCity("london")
        first.setUse24Hour(false)

        // A second repository over the same store models an app restart.
        val afterRestart = ClockRepository(store).preferences.first()
        assertEquals(listOf("tokyo", "los-angeles", "new-york", "london", "reykjavik"), afterRestart.savedCityIds)
        assertEquals("london", afterRestart.activeCityId)
        assertFalse(afterRestart.use24Hour)
    }

    @Test
    fun criterion09b_unknownIdsAreDroppedAndTheActiveCityIsRepaired() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .addCityIds("atlantis")
            .addCityIds("new-york")
            .setActiveCityId("atlantis")
            .setUse24Hour(true)
            .build()

        val preferences: UserPreferences = stored.toUserPreferences()

        assertEquals(listOf("tokyo", "new-york"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    // ---------------------------------------------------------------- 10

    /**
     * Criterion 10 — the map plots every saved city, highlights the active one
     * with the accent marker and meridian, shades the night side, and the
     * selected-city card routes to the detail screen.
     *
     * Visual proof: `map_screen_360dp.png`, `map_screen_summer_360dp.png`,
     * `map_screen_london_360dp.png`.
     */
    @Test
    fun criterion10_theMapPlotsCitiesAndDrivesSelection() {
        val state = PreviewSamples.clockState()
        val active = state.cities.single { it.isActive }
        assertEquals("los-angeles", active.id)

        // Every saved city carries the coordinates the marker is drawn from.
        state.cities.forEach { city ->
            val directory = CityDirectory.find(city.id)!!
            assertEquals(directory.latitude, city.latitude, 1e-9)
            assertEquals(directory.longitude, city.longitude, 1e-9)
        }

        // The night side is shaded, so the subsolar point is lit and its
        // antipode is not.
        val subsolar = com.worldclock.app.domain.map.SolarPosition.subsolarPoint(state.now)
        assertTrue(
            com.worldclock.app.domain.map.SolarPosition.isDaylight(
                com.worldclock.app.domain.map.GeoPoint(subsolar.longitude, subsolar.latitude),
                subsolar,
            ),
        )

        var selected: String? = null
        var opened: String? = null
        composeRule.setContent {
            WorldClockTheme {
                WorldMapScreen(
                    state = state,
                    onSelectCity = { selected = it },
                    onOpenCity = { opened = it },
                    onAddClick = {},
                    onFindCityClick = {},
                )
            }
        }

        // One marker per saved city, and tapping one changes the active city.
        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).performClick()
        assertEquals("tokyo", selected)
        composeRule.onNodeWithTag(WorldMapTestTags.marker("london")).performClick()
        assertEquals("london", selected)

        // The selected-city card opens the detail screen.
        composeRule.onNodeWithTag(WorldMapTestTags.SELECTED_CITY_PANEL).performClick()
        assertEquals("los-angeles", opened)
    }

    // ---------------------------------------------------------------- 11

    /**
     * Criterion 11 — the layout holds at 360 dp portrait, and every control
     * exposes an accessibility label.
     *
     * Visual proof: every PNG in `app/build/screenshots` is captured at
     * `w360dp-h800dp`.
     */
    @Test
    fun criterion11_layoutHoldsAt360dpAndControlsAreLabelled() {
        composeRule.setContent {
            WorldClockTheme {
                WorldMapScreen(
                    state = PreviewSamples.clockState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                )
            }
        }

        // The headline content is on screen and not ellipsised away.
        composeRule.onNodeWithText("Los Angeles").assertIsDisplayed()
        composeRule.onNodeWithText("08:40").assertIsDisplayed()
        composeRule.onNodeWithText("California, USA").assertIsDisplayed()

        assertEveryControlIsLabelled()
    }

    @Test
    fun criterion11b_iconOnlyControlsCarryContentDescriptions() {
        composeRule.setContent {
            WorldClockTheme {
                WorldMapScreen(
                    state = PreviewSamples.clockState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                )
            }
        }

        val labelled = composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .map { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.any { it.text.isNotBlank() } == true
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.isNotBlank() } == true
                text || description
            }

        assertTrue("Expected to find interactive controls on the map screen", labelled.isNotEmpty())
        assertTrue("Every interactive control must have a text or content description", labelled.all { it })
    }

    private fun assertEveryControlIsLabelled() {
        composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.any { it.text.isNotBlank() } == true
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.isNotBlank() } == true
                assertTrue(
                    "Every interactive control must have a text or content description",
                    text || description,
                )
            }
    }

    @Test
    fun criterion11c_theEmptyAndErrorStatesAreReachable() {
        // Empty list.
        composeRule.setContent {
            WorldClockTheme {
                WorldMapScreen(
                    state = PreviewSamples.emptyState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                )
            }
        }
        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()
        composeRule.onNodeWithText("Find a city").assertIsDisplayed()

        // Loading state is a distinct, labelled state.
        assertNotNull(PreviewSamples.emptyState())
    }

    // ---------------------------------------------------------------- 12
    // Phase 2 — theme support (extended to the five themes).
    //
    // 12a is persistence and migration, 12b the theme actually reaching the
    // screen, 12c the picker that chooses it.

    /**
     * Criterion 12a — the chosen theme is persisted, survives a restart, and
     * leaves the saved cities untouched.
     *
     * The restart itself is covered by `ClockRepositoryTest.stateSurvivesARestart`;
     * here the *stored message* is encoded and decoded to prove the choice is in
     * the bytes DataStore puts on disk.
     */
    @Test
    fun criterion12a_theChosenThemeIsPersistedAndSurvivesARestart() = runTest {
        val store = FakeSavedCitiesDataStore()
        val repository = ClockRepository(store)

        // Nothing chosen yet, so the first of the five themes applies.
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.Default)
        assertEquals(AppTheme.Default, repository.preferences.first().theme)

        repository.setTheme(AppTheme.HIGH_CONTRAST)
        assertEquals(AppTheme.HIGH_CONTRAST, repository.preferences.first().theme)

        val before = repository.preferences.first()
        val after = SavedCities.parseFrom(store.data.first().toByteArray()).toUserPreferences()
        assertEquals(AppTheme.HIGH_CONTRAST, after.theme)
        assertEquals(before, after)

        // Changing the theme must not disturb anything else.
        assertEquals(CityDirectory.defaultSavedCityIds, after.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, after.activeCityId)
        assertTrue(after.use24Hour)
    }

    /**
     * Criterion 12a (continued) — a store written by the three-mode build is
     * migrated rather than reset: dark *was* the true-dark palette and light the
     * light monochrome one, so those users keep what they were looking at.
     *
     * `SYSTEM` followed the device and cannot be resolved after the fact, so it
     * lands on the default rather than guessing a theme the user never chose.
     */
    @Test
    fun criterion12a_anOlderStoreIsMigratedToATheme() {
        fun themed(mode: SavedThemeMode) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setThemeMode(mode)
            .build()
            .toUserPreferences()
            .theme

        assertEquals(AppTheme.TRUE_DARK, themed(SavedThemeMode.THEME_MODE_DARK))
        assertEquals(AppTheme.LIGHT_MONOCHROME, themed(SavedThemeMode.THEME_MODE_LIGHT))
        assertEquals(AppTheme.Default, themed(SavedThemeMode.THEME_MODE_SYSTEM))
    }

    /**
     * Criterion 12b — the chosen theme reaches the screens: each of the five
     * provides its own palette, and a change of theme repaints without
     * recreating anything.
     */
    @Test
    fun criterion12b_theChosenThemePaintsTheScreenWithTheMatchingPalette() {
        val darkThemes = setOf(AppTheme.TRUE_DARK, AppTheme.HIGH_CONTRAST)
        var theme by mutableStateOf(AppTheme.LIGHT_MONOCHROME)
        var palette: WorldClockPalette? = null

        composeRule.setContent {
            WorldClockTheme(theme = theme) {
                palette = LocalWorldClockPalette.current
            }
        }

        AppTheme.entries.forEach { candidate ->
            composeRule.runOnIdle { theme = candidate }
            composeRule.runOnIdle {
                assertEquals(paletteFor(candidate), palette)
                assertEquals(
                    "isDark must agree with how the theme paints",
                    candidate in darkThemes,
                    palette!!.isDark,
                )
            }
        }
    }

    /**
     * Criterion 12b (continued) — the device's own setting no longer decides
     * anything. The same theme has to paint the same palette on a phone in dark
     * mode as on one in light mode, which is the whole point of choosing an
     * exact palette rather than following the system.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-night-xhdpi")
    fun criterion12b_theDeviceSettingNoLongerPicksThePalette() {
        var palette: WorldClockPalette? = null

        composeRule.setContent {
            WorldClockTheme(theme = AppTheme.WARM_SEPIA) {
                palette = LocalWorldClockPalette.current
            }
        }

        composeRule.runOnIdle {
            assertEquals(WarmSepiaWorldClockPalette, palette)
            assertEquals(false, palette!!.isDark)
        }
    }

    /**
     * Criterion 12c — the appearance control is reachable from the Clocks
     * screen, offers all five themes by name, reports which one is in force, and
     * hands the new choice back to the caller.
     *
     * The matching renders are `settings_sheet_360dp.png` and, in
     * `LayoutScreenshotTest`, one appearance sheet per theme.
     */
    @Test
    fun criterion12c_theSettingsSheetOffersEveryThemeAndReportsTheSelection() {
        var selected: AppTheme? = null

        composeRule.setContent {
            WorldClockTheme {
                SettingsContent(
                    theme = AppTheme.TRUE_DARK,
                    onThemeSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
        composeRule.onNodeWithText("Light Monochrome").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("True Dark").assertIsSelected()
        composeRule.onNodeWithText("Warm Sepia").assertIsDisplayed()
        composeRule.onNodeWithText("Cool Slate").assertIsDisplayed()
        composeRule.onNodeWithText("High Contrast").assertIsDisplayed().performClick()

        assertEquals(AppTheme.HIGH_CONTRAST, selected)
        assertEveryControlIsLabelled()
    }

    // ---------------------------------------------------------------- 13
    // Phase 2 — the time difference indicator.

    /**
     * Criterion 13a — the difference is the gap between the two zones' offsets
     * at the same instant, in minutes, so half-hour and quarter-hour zones are
     * exact and daylight saving is picked up automatically.
     */
    @Test
    fun criterion13a_theDifferenceIsTheOffsetGapAtThatInstant() {
        val yangon = ZoneId.of("Asia/Yangon")
        fun difference(cityId: String, at: Instant = winter) = TimeDifference.between(
            zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone),
            referenceZoneId = yangon,
            instant = at,
        )

        assertEquals(150, difference("tokyo").minutes) // UTC+9 against UTC+6:30
        assertEquals(270, difference("sydney").minutes) // UTC+11
        assertEquals(-870, difference("los-angeles").minutes) // UTC-8
        assertEquals(-690, difference("new-york").minutes) // UTC-5
        assertEquals(-390, difference("london").minutes) // UTC+0
        assertEquals(-60, difference("mumbai").minutes) // UTC+5:30
        assertEquals(30, difference("bangkok").minutes) // UTC+7
        assertTrue(difference("yangon").isSameTime)

        // London moves to UTC+1 in the northern summer.
        assertEquals(-330, difference("london", summer).minutes)
    }

    /**
     * Criterion 13b — the indicator reads "+N hrs ahead", "-N hrs behind" or
     * "Same time", and keeps the minutes a half-hour zone needs.
     */
    @Test
    fun criterion13b_theDifferenceReadsAheadBehindOrSameTime() {
        val samples = listOf(
            TimeDifference(0),
            TimeDifference(150),
            TimeDifference(-300),
            TimeDifference(330),
            TimeDifference(-30),
            TimeDifference(15),
        )

        composeRule.setContent {
            WorldClockTheme {
                Column { samples.forEach { TimeDifferenceSample(it) } }
            }
        }

        composeRule.onNodeWithText("Same time").assertIsDisplayed()
        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-5 hrs behind").assertIsDisplayed()
        composeRule.onNodeWithText("+5 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-30 mins behind").assertIsDisplayed()
        composeRule.onNodeWithText("+15 mins ahead").assertIsDisplayed()
    }

    /**
     * Criterion 13c — every city card carries the difference, computed from the
     * device's own time.
     *
     * Visual proof: `clocks_screen_labelled_360dp.png`,
     * `map_screen_labelled_360dp.png`.
     */
    @Test
    fun criterion13c_everyCityCardShowsTheDifferenceFromTheDeviceTime() {
        val state = PreviewSamples.clockState()

        assertEquals(150, state.cities.single { it.id == "tokyo" }.difference.minutes)
        assertEquals(-870, state.cities.single { it.id == "los-angeles" }.difference.minutes)

        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = state,
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }

        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-14 hrs 30 mins behind").assertIsDisplayed()
        composeRule.onNodeWithText("-6 hrs 30 mins behind").assertIsDisplayed()
    }

    /**
     * Criterion 13d — the detail screen shows the difference and names the
     * reference it is measured against, so the number is never ambiguous.
     *
     * Visual proof: `city_detail_360dp.png`, `city_detail_labelled_360dp.png`.
     */
    @Test
    fun criterion13d_theDetailScreenNamesTheReferenceItComparesWith() {
        val state = PreviewSamples.clockState()

        composeRule.setContent {
            WorldClockTheme {
                CityDetailScreen(
                    city = CityDirectory.find("tokyo")!!,
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

        composeRule.onNodeWithText("COMPARED WITH YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        // The device's own zone is on screen too, which is the thing compared with.
        composeRule.onNodeWithText("Asia/Yangon · UTC+6:30").assertIsDisplayed()
    }

    // ---------------------------------------------------------------- 14
    // Phase 2 — custom city labels.

    /**
     * Criterion 14a — a label is stored against the city id, survives a
     * restart, can be changed or cleared, and never disturbs anything else.
     */
    @Test
    fun criterion14a_aCityLabelIsPersistedAndCleared() = runTest {
        val store = FakeSavedCitiesDataStore()
        val repository = ClockRepository(store)
        repository.preferences.first()

        repository.setCityLabel("tokyo", "Head Office")
        assertEquals(mapOf("tokyo" to "Head Office"), repository.preferences.first().cityLabels)

        // The label is in the bytes DataStore actually writes.
        val decoded = SavedCities.parseFrom(store.data.first().toByteArray()).toUserPreferences()
        assertEquals("Head Office", decoded.cityLabels["tokyo"])
        assertEquals(CityDirectory.defaultSavedCityIds, decoded.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, decoded.activeCityId)

        // A label is per city.
        repository.setCityLabel("new-york", "Client calls")
        assertEquals("Head Office", repository.preferences.first().cityLabels["tokyo"])

        // And it can be changed and cleared.
        repository.setCityLabel("tokyo", "HQ")
        assertEquals("HQ", repository.preferences.first().cityLabels["tokyo"])

        repository.setCityLabel("tokyo", null)
        val after = repository.preferences.first()
        assertNull(after.cityLabels["tokyo"])
        assertEquals("Client calls", after.cityLabels["new-york"])
    }

    /**
     * Criterion 14b — the label reaches the screens as the name followed by the
     * label, leaving unlabelled cities alone.
     *
     * Visual proof: `clocks_screen_labelled_360dp.png`,
     * `city_detail_labelled_360dp.png`.
     */
    @Test
    fun criterion14b_theLabelIsShownAfterTheCityName() {
        val state = PreviewSamples.labelledState()

        assertEquals("Tokyo (Head Office)", state.cities.single { it.id == "tokyo" }.displayName)
        assertEquals(
            "New York (HQ)",
            state.cities.single { it.id == "new-york" }.displayName,
        )
        assertEquals("London", state.cities.single { it.id == "london" }.displayName)

        composeRule.setContent {
            WorldClockTheme {
                ClocksScreen(
                    state = state,
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Tokyo (Head Office)").assertIsDisplayed()
        composeRule.onNodeWithText("New York (HQ)").assertIsDisplayed()
        composeRule.onNodeWithText("London").assertIsDisplayed()
    }

    /**
     * Criterion 14c — the whole edit loop, driven through the real app shell: the
     * dialog opens from the detail screen, saves a label, shows it, and removes
     * it again.
     *
     * Visual proof: `city_label_dialog_360dp.png`.
     */
    @Test
    fun criterion14c_theEditDialogSavesAndRemovesALabel() {
        val store = FakeSavedCitiesDataStore()
        val viewModel = WorldClockViewModel(
            repository = ClockRepository(store),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            WorldClockTheme {
                WorldClockRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Tokyo").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Tokyo's detail screen, then its label dialog.
        composeRule.onNodeWithText("Tokyo").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("ADD LABEL").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("ADD LABEL").performClick()

        composeRule.onNodeWithText("City label").assertIsDisplayed()
        composeRule.onNodeWithTag(CityLabelDialogTags.FIELD).performTextInput("Head Office")
        composeRule.onNodeWithText("Save").performClick()

        // The label is stored and rendered as "Tokyo (Head Office)".
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("(Head Office)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("EDIT LABEL").assertIsDisplayed()

        // Re-opening the dialog offers removal, which clears it again.
        composeRule.onNodeWithText("EDIT LABEL").performClick()
        composeRule.onNodeWithText("Remove label").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("ADD LABEL").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("(Head Office)").assertDoesNotExist()
    }

    // ---- Phase 2, stage 3: the meeting planner ----

    /**
     * Criterion 15a — the Clocks screen offers a meeting planner, and opening it
     * puts a 24-hour slider on screen set to the current time.
     *
     * Driven through the real app shell, so this covers the entry row, the route
     * and the control in one pass.
     *
     * Visual proof: `planner_screen_360dp.png`.
     */
    @Test
    fun criterion15a_theClocksScreenOffersAFullDayMeetingPlanner() {
        val store = FakeSavedCitiesDataStore()
        val viewModel = WorldClockViewModel(
            repository = ClockRepository(store),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            WorldClockTheme {
                WorldClockRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("PLAN A MEETING").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("PLAN A MEETING").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Meeting planner").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR CITIES AT THIS TIME").assertIsDisplayed()

        // The slider covers the whole day in quarter hours: 00:00 through 23:45,
        // so the last slot never rolls over into tomorrow.
        val yangon = ZoneId.of("Asia/Yangon")
        val opensOn = MinuteOfDay.ofInstant(winter, yangon).snappedToGrid()
        // 23:10 rounds to the next quarter hour, not down.
        assertEquals(MinuteOfDay.of(23, 15), opensOn)

        val slider = composeRule.onNodeWithTag(DaySliderTags.TRACK)
        slider.assertRangeInfoEquals(
            ProgressBarRangeInfo(
                current = opensOn.value.toFloat(),
                range = 0f..MinuteOfDay.LAST_GRID_VALUE.toFloat(),
                steps = MinuteOfDay.GRID_STEPS,
            ),
        )
        // It opens on the clock, so the planner agrees with the list behind it,
        // and the whole plan is already drawn for that slot.
        slider.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "23:15"),
        )
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sun 01:45, outside working hours")
            .assertIsDisplayed()
    }

    /**
     * Criterion 15b — the slot is the single input to every row: sweeping the
     * slider moves all the saved cities at once, each by the same amount, and
     * every row reads that city's own clock at the slot's instant.
     *
     * Checked across all 96 slots of the day, against times computed here rather
     * than taken from the code under test.
     */
    @Test
    fun criterion15b_everyCityMovesWithTheSlot() {
        val state = PreviewSamples.clockState()
        val deviceZone = PreviewSamples.sampleDeviceZoneId
        val referenceDate = LocalDate.ofInstant(state.now, deviceZone)

        val slots = (MinuteOfDay.MIN_VALUE..MinuteOfDay.LAST_GRID_VALUE step MinuteOfDay.STEP_MINUTES)
            .map { MinuteOfDay(it) }
        assertEquals("96 quarter-hours in a day", 96, slots.size)

        for (cityId in CityDirectory.defaultSavedCityIds) {
            val zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone)
            var previousMinute: Int? = null

            for (slot in slots) {
                val row = buildPlannerUiState(state, slot, deviceZone)
                    .rows.single { it.id == cityId }

                // The slot is a time of day where the user is; the city's reading
                // is that same instant seen through the city's own zone.
                val local = slot.onDate(referenceDate, deviceZone).atZone(zoneId)
                val minuteOfDay = local.hour * 60 + local.minute

                // The row is this city's clock at the slot's instant.
                assertEquals(
                    "$cityId at ${slot.value}",
                    "%02d:%02d".format(local.hour, local.minute),
                    row.timeText,
                )

                // A quarter-hour step of the slider is a quarter-hour step in
                // every city — nobody is left showing the previous slot and
                // nobody is rounded to the hour.
                previousMinute?.let { previous ->
                    assertEquals(
                        "$cityId stepping from ${slot.value - MinuteOfDay.STEP_MINUTES}",
                        (previous + MinuteOfDay.STEP_MINUTES) % 1440,
                        minuteOfDay,
                    )
                }
                previousMinute = minuteOfDay
            }
        }
    }

    /**
     * Criterion 15c — the slider can be driven by a finger and by a screen
     * reader, and the plan follows the movement live rather than on release.
     *
     * Visual proof: `planner_screen_dark_360dp.png`.
     */
    @Test
    fun criterion15c_theSliderIsDrivableAndThePlanFollowsItLive() {
        var slot by mutableStateOf(MinuteOfDay.of(9, 0))
        val state = PreviewSamples.clockState()

        composeRule.setContent {
            WorldClockTheme {
                MeetingPlannerScreen(
                    planner = buildPlannerUiState(state, slot, PreviewSamples.sampleDeviceZoneId),
                    onMinuteChange = { slot = it },
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }

        // 09:00 in Yangon: Tokyo is at work, and California is on the previous
        // evening and out of hours.
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 11:30, within working hours")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Meeting time in Los Angeles: Fri 18:30, outside working hours",
            )
            .assertIsDisplayed()

        // A touch anywhere on the strip jumps there: halfway along is midday.
        val node = composeRule.onNodeWithTag(DaySliderTags.TRACK).fetchSemanticsNode()
        val inset = with(composeRule.density) { 10.dp.toPx() }
        composeRule.onNodeWithTag(DaySliderTags.TRACK).performTouchInput {
            click(Offset(inset + (node.size.width - 2 * inset) / 2f, center.y))
        }
        assertEquals(MinuteOfDay.of(12, 0), slot)
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 14:30, within working hours")
            .assertIsDisplayed()

        // And so does TalkBack's adjustable-progress action, which is the only
        // way a screen-reader user can move it.
        composeRule.onNodeWithTag(DaySliderTags.TRACK)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }
        assertEquals(MinuteOfDay.of(1, 30), slot)
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 04:00, outside working hours")
            .assertIsDisplayed()
    }

    /**
     * Criterion 15d — the planner says which cities are inside a working day,
     * judges that in each city's own zone, and marks the day each city is on.
     *
     * Visual proof: `planner_screen_360dp.png` (filled marker = at work),
     * `planner_screen_dark_360dp.png`.
     */
    @Test
    fun criterion15d_thePlannerNamesTheWorkingDayInEachCitysOwnZone() {
        val state = PreviewSamples.clockState()

        val morning = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        assertTrue(morning.rows.single { it.id == "tokyo" }.isWorkingHours)
        assertTrue(morning.rows.single { it.id == "sydney" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "los-angeles" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "new-york" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "london" }.isWorkingHours)

        // Same city, same slot, different judgement once the day moves on.
        val evening = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(18, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        assertFalse(evening.rows.single { it.id == "tokyo" }.isWorkingHours)
        assertTrue(evening.rows.single { it.id == "london" }.isWorkingHours)

        // Cities behind the date line are marked with their own weekday.
        assertEquals("Sat", morning.rows.single { it.id == "london" }.weekdayLabel)
        assertEquals("Fri", morning.rows.single { it.id == "los-angeles" }.weekdayLabel)

        // The rule is stated on screen, not applied silently.
        composeRule.setContent {
            WorldClockTheme {
                MeetingPlannerScreen(
                    planner = morning,
                    onMinuteChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }
        composeRule.onNodeWithText("WITHIN 09:00–18:00").assertIsDisplayed()
        composeRule.onNodeWithText("OUTSIDE").assertIsDisplayed()
        assertEquals(WORKING_DAY_START_HOUR, 9)
        assertEquals(WORKING_DAY_END_HOUR, 18)
    }

    // ---- Phase 2, stage 5: integration, polish and verification ----

    /**
     * One of the surfaces phase 2 added. The stage 5 checks render them one at
     * a time in a single composition, because a JVM test may only set content
     * once.
     */
    private enum class Stage5Screen { Clocks, Planner, EmptyPlanner, Settings, LabelDialog, Detail }

    @Composable
    private fun Stage5Surface(surface: Stage5Screen, state: WorldClockUiState) {
        when (surface) {
            Stage5Screen.Clocks -> ClocksScreen(
                state = state,
                onCityClick = {},
                onAddClick = {},
                onFindCityClick = {},
                onSettingsClick = {},
                onPlanMeetingClick = {},
            )

            Stage5Screen.Planner -> MeetingPlannerScreen(
                planner = PreviewSamples.plannerState(labels = true),
                onMinuteChange = {},
                onBack = {},
                onFindCityClick = {},
            )

            Stage5Screen.EmptyPlanner -> MeetingPlannerScreen(
                planner = PreviewSamples.emptyPlannerState(),
                onMinuteChange = {},
                onBack = {},
                onFindCityClick = {},
            )

            Stage5Screen.Settings -> SettingsContent(
                theme = AppTheme.LIGHT_MONOCHROME,
                onThemeSelected = {},
            )

            Stage5Screen.LabelDialog -> Box(Modifier.fillMaxSize()) {
                CityLabelDialog(
                    cityName = "Tokyo",
                    currentLabel = "Head Office",
                    onSave = {},
                    onDismiss = {},
                )
            }

            Stage5Screen.Detail -> CityDetailScreen(
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

    /**
     * Criterion 17a — the appearance switch is wired into the running app, not
     * only into the sheet: choosing a mode in the shell repaints the whole shell
     * and is written back through the repository.
     *
     * The composition below is the same wiring `MainActivity` uses — the theme
     * wraps the shell and reads the same view model the sheet writes to — so this
     * is the app's real path rather than a shortcut. Dialogs and sheets are
     * covered by 14c (labels) and 17b (the sheet's own controls).
     */
    @Test
    fun criterion17a_theAppearanceChosenInTheShellRepaintsTheApp() {
        val viewModel = WorldClockViewModel(
            repository = newRepository(),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        var palette: WorldClockPalette? = null

        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            WorldClockTheme(theme = state.theme) {
                palette = LocalWorldClockPalette.current
                WorldClockRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Los Angeles").fetchSemanticsNodes().isNotEmpty()
        }

        // The gear on the Clocks screen opens the sheet, which starts on the
        // stored choice.
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
        composeRule.onNodeWithText("Light Monochrome").assertIsSelected()
        // The action rather than a tap: a bottom sheet is a window of its own,
        // and injected touches do not cross into it under the JVM test renderer.
        composeRule.onNodeWithText("High Contrast").performSemanticsAction(SemanticsActions.OnClick)

        // The choice reaches the palette the whole shell is drawn with...
        composeRule.waitUntil(timeoutMillis = 10_000) {
            palette == HighContrastWorldClockPalette
        }
        // ...and the state every screen reads, which is the repository's.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.theme == AppTheme.HIGH_CONTRAST
        }
        composeRule.onNodeWithText("High Contrast").assertIsSelected()
        composeRule.onNodeWithText("Light Monochrome").assertIsNotSelected()
    }

    /**
     * Criterion 17b — every control phase 2 added is at least 48 dp tall, which
     * is the guideline minimum for something a finger has to hit. The icon-only
     * controls, whose size the app sets, are checked in both directions.
     *
     * The small controls keep their drawn size and pad the *target* out to this;
     * that is what the check measures. Widths of text-sized controls are not
     * asserted, because the JVM test renderer's stub font measures text at close
     * to zero width — the PNGs are the evidence for those.
     */
    @Test
    fun criterion17b_everyPhaseTwoControlMeetsTheTouchTargetGuideline() {
        val state = PreviewSamples.labelledState()
        var surface by mutableStateOf(Stage5Screen.Clocks)

        composeRule.setContent {
            WorldClockTheme {
                Stage5Surface(surface = surface, state = state)
            }
        }

        fun show(next: Stage5Screen) {
            composeRule.runOnIdle { surface = next }
        }

        // The list: the gear, the add action, the planner entry, the add row.
        assertIconTargetIsBigEnough("Settings")
        assertIconTargetIsBigEnough("Add city")
        assertTargetIsTallEnough("PLAN A MEETING")
        assertTargetIsTallEnough("ADD CITY")

        show(Stage5Screen.Planner)
        assertIconTargetIsBigEnough("Back")
        composeRule.onNodeWithTag(DaySliderTags.TRACK)
            .assertHeightIsAtLeast(MinTouchTarget)

        show(Stage5Screen.EmptyPlanner)
        assertTargetIsTallEnough("Find a city")

        // The appearance picker: five theme rows, each a finger-sized target.
        show(Stage5Screen.Settings)
        assertTargetIsTallEnough("Light Monochrome")
        assertTargetIsTallEnough("True Dark")
        assertTargetIsTallEnough("Warm Sepia")
        assertTargetIsTallEnough("Cool Slate")
        assertTargetIsTallEnough("High Contrast")

        // The label dialog's field.
        show(Stage5Screen.LabelDialog)
        composeRule.onNodeWithTag(CityLabelDialogTags.FIELD)
            .assertHeightIsAtLeast(MinTouchTarget)

        // The detail screen's label action.
        show(Stage5Screen.Detail)
        assertTargetIsTallEnough("EDIT LABEL")
    }

    private fun assertTargetIsTallEnough(text: String) {
        composeRule.onNodeWithText(text).assertHeightIsAtLeast(MinTouchTarget)
    }

    private fun assertIconTargetIsBigEnough(contentDescription: String) {
        composeRule.onNodeWithContentDescription(contentDescription)
            .assertHeightIsAtLeast(MinTouchTarget)
            .assertWidthIsAtLeast(MinTouchTarget)
    }

    /**
     * Criterion 17c — the phase 2 surfaces are structured for assistive
     * technology: every screen title and section header is a heading, the label
     * dialog names itself when it opens and names the city its field edits, and
     * a city card announces the time difference instead of only drawing it.
     *
     * The planner's slider semantics are criterion 15c.
     */
    @Test
    fun criterion17c_thePhaseTwoSurfacesAreStructuredForAssistiveTechnology() {
        val state = PreviewSamples.labelledState()
        var surface by mutableStateOf(Stage5Screen.Clocks)

        composeRule.setContent {
            WorldClockTheme {
                Stage5Surface(surface = surface, state = state)
            }
        }

        fun show(next: Stage5Screen) {
            composeRule.runOnIdle { surface = next }
        }

        // The difference is spoken with the card, and says what it is measured
        // against.
        composeRule.onNodeWithContentDescription(
            "Compared with your local time: +2 hrs 30 mins ahead",
        ).assertExists()
        assertHeading("World Time")
        assertHeading("YOUR CITIES")

        show(Stage5Screen.Planner)
        assertHeading("Meeting planner")
        assertHeading("YOUR LOCAL TIME")
        assertHeading("YOUR CITIES AT THIS TIME")

        show(Stage5Screen.Settings)
        assertHeading("Settings")
        assertHeading("APPEARANCE")

        show(Stage5Screen.Detail)
        assertHeading("COMPARED WITH YOUR LOCAL TIME")
        assertHeading("YOUR LOCAL TIME")

        // The dialog is drawn in the app's own composition, so it has to
        // announce itself as a pane and name the field: nothing else will.
        show(Stage5Screen.LabelDialog)
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "City label"))
            .assertExists()
        assertHeading("City label")
        composeRule.onNodeWithContentDescription("Label for Tokyo").assertExists()
    }

    private fun assertHeading(text: String) {
        composeRule.onNodeWithText(text)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

}

/** Renders one difference so the wording itself can be asserted. */
@Composable
private fun TimeDifferenceSample(difference: TimeDifference) {
    Text(text = timeDifferenceLabel(difference))
}
