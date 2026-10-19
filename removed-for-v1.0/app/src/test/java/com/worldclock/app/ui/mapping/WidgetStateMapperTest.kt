package com.worldclock.app.ui.mapping

import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.ui.WidgetUiState
import com.worldclock.app.ui.samples.PreviewSamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * The widget's contents: fewer fields than a city card, but the same times —
 * and one honest decision about which cities fit.
 *
 * Reference instant is the previews' `2024-01-20T16:40:00Z` in `Asia/Yangon`
 * (UTC+6:30), which is 23:10 on Saturday 20 January. Tokyo and Sydney are
 * already into Sunday; Los Angeles, New York and London are not.
 */
class WidgetStateMapperTest {

    private fun widget(
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        labels: Map<String, String> = emptyMap(),
        use24Hour: Boolean = true,
        maxRows: Int = WIDGET_MAX_ROWS,
    ): WidgetUiState = buildWidgetUiState(
        savedCityIds = savedCityIds,
        cityLabels = labels,
        use24Hour = use24Hour,
        now = PreviewSamples.referenceInstant,
        deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        maxRows = maxRows,
    )

    private fun row(state: WidgetUiState, id: String) = state.rows.single { it.id == id }

    @Test
    fun everySavedCityGetsOneRowInSavedOrder() {
        val state = widget()

        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london"),
            state.rows.map { it.id },
        )
        assertEquals("Tokyo", row(state, "tokyo").displayName)
        assertEquals(0, state.hiddenCount)
        assertFalse(state.isEmpty)
    }

    @Test
    fun eachRowReadsThatCitysClockAtTheInstantTheWidgetWasDrawn() {
        val state = widget()

        assertEquals("01:40", row(state, "tokyo").timeText)
        assertEquals("03:40", row(state, "sydney").timeText)
        assertEquals("08:40", row(state, "los-angeles").timeText)
        assertEquals("11:40", row(state, "new-york").timeText)
        assertEquals("16:40", row(state, "london").timeText)
        // The header carries the device's own clock, the row every other row is
        // read against.
        assertEquals("23:10", state.deviceTimeText)
    }

    @Test
    fun aCityOnAnotherDayIsMarkedAndTheRestAreNot() {
        val state = widget()

        // Yangon is on Saturday, so only the cities that have rolled over are
        // labelled — a bare "01:40" would otherwise read as this morning.
        assertTrue(row(state, "tokyo").isAnotherDay)
        assertTrue(row(state, "sydney").isAnotherDay)
        assertFalse(row(state, "los-angeles").isAnotherDay)
        assertFalse(row(state, "new-york").isAnotherDay)
        assertFalse(row(state, "london").isAnotherDay)
    }

    @Test
    fun theRowsCarryTheWeekdayTheyWouldNeedIfTheLabelIsShown() {
        val state = widget()

        // Always carried, even when the label is not drawn, so the state stays
        // complete for anything reading it.
        assertEquals("Sun", row(state, "tokyo").weekdayLabel)
        assertEquals("Sat", row(state, "london").weekdayLabel)
    }

    @Test
    fun theSunOrMoonFollowsEachCitysOwnLocalHour() {
        val state = widget()

        assertFalse(row(state, "tokyo").isDaytime)
        assertFalse(row(state, "sydney").isDaytime)
        assertTrue(row(state, "los-angeles").isDaytime)
        assertTrue(row(state, "new-york").isDaytime)
        assertTrue(row(state, "london").isDaytime)
    }

    @Test
    fun theAppWideFormatIsUsedForEveryTime() {
        val twelveHour = widget(use24Hour = false)

        assertEquals("1:40 AM", row(twelveHour, "tokyo").timeText)
        assertEquals("4:40 PM", row(twelveHour, "london").timeText)
        assertEquals("11:10 PM", twelveHour.deviceTimeText)
    }

    @Test
    fun theUsersLabelsComeThroughToTheWidget() {
        val state = widget(labels = mapOf("tokyo" to "Head Office"))

        assertEquals("Tokyo (Head Office)", row(state, "tokyo").displayName)
        assertEquals("London", row(state, "london").displayName)
    }

    @Test
    fun theRowBudgetIsCountedRatherThanQuietlyDropped() {
        val eightCities = listOf(
            "tokyo",
            "sydney",
            "los-angeles",
            "new-york",
            "london",
            "paris",
            "dubai",
            "mumbai",
        )

        val full = widget(savedCityIds = eightCities)
        assertEquals(WIDGET_MAX_ROWS, full.rows.size)
        assertEquals(2, full.hiddenCount)
        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london", "paris"),
            full.rows.map { it.id },
        )

        // A shorter widget is the same rule with a different budget.
        val short = widget(savedCityIds = eightCities, maxRows = 3)
        assertEquals(3, short.rows.size)
        assertEquals(5, short.hiddenCount)
    }

    @Test
    fun anEmptyListSaysSoRatherThanDrawingAnEmptyCard() {
        val state = widget(savedCityIds = emptyList())

        assertTrue(state.rows.isEmpty())
        assertTrue(state.isEmpty)
        assertEquals(0, state.hiddenCount)
        // The device's own clock is still there to anchor the header.
        assertEquals("23:10", state.deviceTimeText)
    }

    @Test
    fun aCityIdTheDirectoryNoLongerKnowsIsDroppedRatherThanCrashing() {
        val state = widget(savedCityIds = listOf("atlantis", "tokyo"))

        assertEquals(listOf("tokyo"), state.rows.map { it.id })
        assertEquals(0, state.hiddenCount)
    }

    @Test
    fun theWidgetAndTheClocksScreenCanNeverDisagreeAboutATime() {
        val state = widget()
        val screen = buildWorldClockUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            activeCityId = null,
            use24Hour = true,
            now = PreviewSamples.referenceInstant,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        // The widget runs the same mapper, so this is the property that keeps it
        // honest: same city, same instant, same string.
        assertEquals(screen.cities.size, state.rows.size)
        screen.cities.zip(state.rows).forEach { (card, row) ->
            assertEquals(card.id, row.id)
            assertEquals(card.displayName, row.displayName)
            assertEquals(card.timeText, row.timeText)
            assertEquals(card.isDaytime, row.isDaytime)
        }
    }

    @Test
    fun theDeviceZoneIsTheOnlyReferenceTheWidgetUses() {
        // Read in London instead of Yangon, the same instant produces different
        // "another day" labels: nothing is pinned to the device's own date
        // beyond the comparison itself.
        val inLondon = buildWidgetUiState(
            savedCityIds = listOf("tokyo"),
            use24Hour = true,
            now = PreviewSamples.referenceInstant,
            deviceZoneId = ZoneId.of("Europe/London"),
        )

        // 16:40 in London on the Saturday, 01:40 on the Sunday in Tokyo.
        assertEquals("01:40", inLondon.rows.single().timeText)
        assertTrue(inLondon.rows.single().isAnotherDay)
        assertEquals("16:40", inLondon.deviceTimeText)
    }
}
