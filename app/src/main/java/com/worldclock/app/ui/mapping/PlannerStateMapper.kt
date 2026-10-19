package com.worldclock.app.ui.mapping

import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.domain.time.CityTimeFormatter
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.domain.time.isWithinWorkingDay
import com.worldclock.app.ui.PlannerCityRow
import com.worldclock.app.ui.PlannerUiState
import com.worldclock.app.ui.WorldClockUiState
import java.time.LocalDate
import java.time.ZoneId

/**
 * Builds the meeting planner's state for one slot.
 *
 * The slot is a time of day **in the device's own zone** — the same reference
 * the city cards compare against — so "09:00" means nine in the morning where
 * the user is, and every other city is shown at that same instant. Like
 * [buildWorldClockUiState] this is pure, so the whole planner can be exercised
 * without a device and the screenshots are deterministic.
 *
 * @param minuteOfDay the chosen slot, in the device's own timezone
 */
fun buildPlannerUiState(
    state: WorldClockUiState,
    minuteOfDay: MinuteOfDay,
    deviceZoneId: ZoneId,
): PlannerUiState {
    // Today where the user is. Every city is then read at the instant that
    // falls at this time in the reference zone, which is what makes a city on
    // the other side of the date line show a different weekday.
    val referenceDate = LocalDate.ofInstant(state.now, deviceZoneId)
    val instant = minuteOfDay.onDate(referenceDate, deviceZoneId)

    val reference = CityTimeFormatter.snapshot(
        zoneId = deviceZoneId,
        instant = instant,
        use24Hour = state.use24Hour,
    )

    val rows = state.cities.mapNotNull { card ->
        val city = CityDirectory.find(card.id) ?: return@mapNotNull null
        val zoneId = ZoneId.of(city.timeZone)
        val time = CityTimeFormatter.snapshot(
            zoneId = zoneId,
            instant = instant,
            use24Hour = state.use24Hour,
        )

        PlannerCityRow(
            id = card.id,
            displayName = card.displayName,
            timeText = time.timeText,
            weekdayLabel = time.weekdayShort,
            isWorkingHours = isWithinWorkingDay(instant, zoneId),
            // The difference is measured at the chosen slot rather than "now":
            // a daylight-saving change between the two would otherwise make the
            // row disagree with the clock beside it.
            difference = TimeDifference.between(zoneId, deviceZoneId, instant),
        )
    }

    return PlannerUiState(
        minuteOfDay = minuteOfDay,
        referenceTimeText = reference.timeText,
        referenceDateText = "${reference.weekdayShort}, ${reference.dateText}",
        referenceOffsetLabel = reference.offsetLabel,
        rows = rows,
    )
}
