package com.worldclock.app.ui.samples

import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.ui.PlannerUiState
import com.worldclock.app.ui.WorldClockUiState
import com.worldclock.app.ui.mapping.buildPlannerUiState
import com.worldclock.app.ui.mapping.buildWorldClockUiState
import java.time.Instant
import java.time.ZoneId

/**
 * Deterministic screen states for previews and screenshot tests.
 *
 * Uses a fixed instant so renders are reproducible, and the real mapper so the
 * sample data is produced by exactly the same code path as the app.
 */
object PreviewSamples {

    /**
     * 20 Jan 2024, 16:40 UTC — a winter instant chosen so the default five
     * cities read exactly like the reference mockups (01:40 / 03:40 / 08:40 /
     * 11:40 / 16:40).
     */
    val referenceInstant: Instant = Instant.parse("2024-01-20T16:40:00Z")

    /**
     * 20 Jul 2024, 04:40 UTC — the opposite season, so the day/night terminator
     * covers the northern hemisphere instead of the southern one.
     */
    val summerInstant: Instant = Instant.parse("2024-07-20T04:40:00Z")

    private val deviceZone: ZoneId = ZoneId.of("Asia/Yangon")

    fun clockState(
        use24Hour: Boolean = true,
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        activeCityId: String? = CityDirectory.defaultActiveCityId,
        now: Instant = referenceInstant,
        cityLabels: Map<String, String> = emptyMap(),
    ): WorldClockUiState = buildWorldClockUiState(
        savedCityIds = savedCityIds,
        activeCityId = activeCityId,
        use24Hour = use24Hour,
        now = now,
        deviceZoneId = deviceZone,
        cityLabels = cityLabels,
    )

    /**
     * The default list with a couple of labels, for label renders.
     *
     * Both labels are short on purpose: a label is the user's own text, so a
     * very long one is allowed to ellipsize rather than push the city name off
     * the card, and the samples should show the everyday case.
     */
    fun labelledState(use24Hour: Boolean = true): WorldClockUiState = clockState(
        use24Hour = use24Hour,
        cityLabels = mapOf(
            "tokyo" to "Head Office",
            "new-york" to "HQ",
        ),
    )

    fun emptyState(use24Hour: Boolean = true): WorldClockUiState = buildWorldClockUiState(
        savedCityIds = emptyList(),
        activeCityId = null,
        use24Hour = use24Hour,
        now = referenceInstant,
        deviceZoneId = deviceZone,
    )

    /**
     * The meeting planner for the default list at a chosen slot.
     *
     * @param labels true to include the sample city labels, so the planner's
     *   rows show the same names as the labelled clock renders
     */
    fun plannerState(
        minuteOfDay: MinuteOfDay = MinuteOfDay.of(9, 0),
        labels: Boolean = false,
        use24Hour: Boolean = true,
    ): PlannerUiState = buildPlannerUiState(
        state = if (labels) labelledState(use24Hour) else clockState(use24Hour = use24Hour),
        minuteOfDay = minuteOfDay,
        deviceZoneId = deviceZone,
    )

    /** The planner with nothing saved. */
    fun emptyPlannerState(
        minuteOfDay: MinuteOfDay = MinuteOfDay.of(9, 0),
    ): PlannerUiState = buildPlannerUiState(
        state = emptyState(),
        minuteOfDay = minuteOfDay,
        deviceZoneId = deviceZone,
    )

    /** The zone the samples treat as the device's own; Asia/Yangon is UTC+6:30. */
    val sampleDeviceZoneId: ZoneId get() = deviceZone
}
