package com.worldclock.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldclock.app.ui.WorldClockUiState
import com.worldclock.app.ui.components.WorldMapTestTags
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviour of the world map screen: marker selection, the route to the city
 * detail screen, the empty state, and the accessibility contract.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class WorldMapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: WorldClockUiState = PreviewSamples.clockState(),
        onSelectCity: (String) -> Unit = {},
        onOpenCity: (String) -> Unit = {},
        onFindCityClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WorldClockTheme {
                WorldMapScreen(
                    state = state,
                    onSelectCity = onSelectCity,
                    onOpenCity = onOpenCity,
                    onAddClick = {},
                    onFindCityClick = onFindCityClick,
                )
            }
        }
    }

    @Test
    fun thereIsAMarkerForEverySavedCity() {
        render()

        listOf("tokyo", "sydney", "los-angeles", "new-york", "london").forEach { cityId ->
            composeRule.onNodeWithTag(WorldMapTestTags.marker(cityId)).assertExists()
        }
    }

    @Test
    fun tappingAMarkerSelectsThatCity() {
        var selected: String? = null
        render(onSelectCity = { selected = it })

        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).performClick()

        assertEquals("tokyo", selected)
    }

    @Test
    fun theSelectedMarkerIsTheActiveCity() {
        render(PreviewSamples.clockState(activeCityId = "new-york"))

        composeRule.onNodeWithTag(WorldMapTestTags.marker("new-york")).assertIsSelected()
    }

    @Test
    fun tappingACityPillSelectsThatCity() {
        var selected: String? = null
        render(onSelectCity = { selected = it })

        composeRule.onNodeWithTag(WorldMapTestTags.cityPill("sydney")).performClick()

        assertEquals("sydney", selected)
    }

    @Test
    fun tappingTheClockPanelOpensTheActiveCityDetail() {
        var opened: String? = null
        render(onOpenCity = { opened = it })

        composeRule.onNodeWithTag(WorldMapTestTags.SELECTED_CITY_PANEL).performClick()

        assertEquals("los-angeles", opened)
    }

    @Test
    fun everyMarkerCarriesADescriptionWithItsNameTimeAndOffset() {
        render()

        // Los Angeles is active at the reference instant: 08:40, UTC-8.
        composeRule
            .onNodeWithContentDescription("Los Angeles, 08:40, UTC-8, selected city")
            .assertExists()
        composeRule
            .onNodeWithContentDescription("Tokyo, 01:40, UTC+9")
            .assertExists()
    }

    @Test
    fun theMapItselfHasADescriptionForScreenReaders() {
        render()

        composeRule.onNodeWithTag(WorldMapTestTags.MAP_CANVAS, useUnmergedTree = true)
            .assertExists()
        composeRule
            .onNodeWithContentDescription("Schematic world map. UTC offset ruler.", substring = true)
            .assertExists()
    }

    @Test
    fun anEmptyCityListShowsAPromptInsteadOfMarkers() {
        var findCityTapped = false
        render(
            state = PreviewSamples.emptyState(),
            onFindCityClick = { findCityTapped = true },
        )

        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()
        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).assertDoesNotExist()

        composeRule.onNodeWithText("Find a city").performClick()

        assertEquals(true, findCityTapped)
    }

    @Test
    fun anEmptyCityListNeverCallsBackWithACity() {
        var selected: String? = null
        render(state = PreviewSamples.emptyState(), onSelectCity = { selected = it })

        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()

        assertNull(selected)
    }

    @Test
    fun selectingFromTheStripIsOfferedAsASingleSelectionGroup() {
        render()

        // The active pill reports itself selected; the others do not.
        composeRule.onNodeWithTag(WorldMapTestTags.cityPill("los-angeles")).assertIsSelected()
    }
}
