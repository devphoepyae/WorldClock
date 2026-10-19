package com.worldclock.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.repository.ClockRepository
import com.worldclock.app.testing.FakeSavedCitiesDataStore
import com.worldclock.app.ui.components.WorldMapTestTags
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * Navigation and back behaviour across the app shell: the bottom tabs, the route
 * from a map marker into the city detail screen, and the return trip — including
 * the phase 2 destinations that hang off the Clocks screen: the meeting planner
 * and the appearance sheet.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class WorldClockNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderShell() {
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
    }

    /** The shell with nothing saved, for the planner's own empty state. */
    private fun renderEmptyShell() {
        val repository = ClockRepository(FakeSavedCitiesDataStore())
        runBlocking {
            CityDirectory.defaultSavedCityIds.forEach { repository.removeCity(it) }
        }
        val viewModel = WorldClockViewModel(
            repository = repository,
            ticker = flowOf(PreviewSamples.referenceInstant),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        composeRule.setContent {
            WorldClockTheme {
                WorldClockRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("No cities yet").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The phase 2 planner is part of the main flow: it opens from the list and
     * the back action returns to it rather than to the app's start destination.
     */
    @Test
    fun theClocksScreenOpensTheMeetingPlannerAndBackReturnsToTheList() {
        renderShell()

        composeRule.onNodeWithText("PLAN A MEETING").performClick()
        composeRule.onNodeWithText("YOUR CITIES AT THIS TIME").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("YOUR CITIES").assertIsDisplayed()
    }

    /** With nothing saved the planner hands the user back to the city search. */
    @Test
    fun thePlannerWithNothingSavedOffersTheCitySearch() {
        renderEmptyShell()

        composeRule.onNodeWithText("PLAN A MEETING").performClick()
        composeRule.onNodeWithText("No cities to plan for").assertIsDisplayed()

        composeRule.onNodeWithText("Find a city").performClick()

        composeRule.onNodeWithText("Add a city").assertIsDisplayed()
    }

    /**
     * The appearance picker hangs off the Clocks screen's gear action, and the
     * sheet reports which theme is in force.
     */
    @Test
    fun theSettingsActionOpensTheAppearanceSheet() {
        renderShell()

        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Light Monochrome").assertIsSelected()
    }

    @Test
    fun theMapTabOpensTheWorldMap() {
        renderShell()

        composeRule.onNodeWithContentDescription("World map").performClick()

        composeRule.onNodeWithText("TAP A MARKER TO SELECT · SHADED AREA IS NIGHT").assertIsDisplayed()
    }

    @Test
    fun theClocksTabReturnsToTheCityList() {
        renderShell()

        composeRule.onNodeWithContentDescription("World map").performClick()
        composeRule.onNodeWithContentDescription("Clocks").performClick()

        composeRule.onNodeWithText("YOUR CITIES").assertIsDisplayed()
    }

    @Test
    fun selectingAMapMarkerMakesThatCityActive() {
        renderShell()

        composeRule.onNodeWithContentDescription("World map").performClick()
        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).performClick()

        // The selected-city panel and pill both follow the new active city.
        composeRule.onNodeWithText("Tokyo").assertIsDisplayed()
        composeRule.onNodeWithText("01:40").assertIsDisplayed()
    }

    @Test
    fun theSelectedCityCardOpensTheDetailScreenAndBackReturnsToTheMap() {
        renderShell()

        composeRule.onNodeWithContentDescription("World map").performClick()
        composeRule.onNodeWithTag(WorldMapTestTags.SELECTED_CITY_PANEL).performClick()

        // The detail screen, opened from the map.
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithText("View on map").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        // Back goes to the map, not the app's start destination.
        composeRule.onNodeWithText("TAP A MARKER TO SELECT · SHADED AREA IS NIGHT").assertIsDisplayed()
    }

    @Test
    fun aCityFromTheListOpensTheDetailScreenAndBackReturnsToTheList() {
        renderShell()

        composeRule.onNodeWithText("Tokyo").performClick()
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("YOUR CITIES").assertIsDisplayed()
    }

    @Test
    fun theSearchActionOpensTheSheetFromTheMap() {
        renderShell()

        composeRule.onNodeWithContentDescription("World map").performClick()
        composeRule.onNodeWithContentDescription("Search cities").performClick()

        composeRule.onNodeWithText("Add a city").assertIsDisplayed()
    }
}
