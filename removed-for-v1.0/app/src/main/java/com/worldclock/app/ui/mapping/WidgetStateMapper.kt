package com.worldclock.app.ui.mapping

import com.worldclock.app.domain.time.CityTimeFormatter
import com.worldclock.app.ui.WidgetRow
import com.worldclock.app.ui.WidgetUiState
import java.time.Instant
import java.time.ZoneId

/**
 * How many cities the widget shows before it stops listing and starts counting.
 *
 * A widget is a fixed rectangle chosen by the user, not a scrollable list, so
 * the budget is explicit and the overflow is reported as "+N more" instead of
 * being dropped silently (spec section 5.11).
 */
const val WIDGET_MAX_ROWS: Int = 6

/**
 * Builds the widget's rendering state from the stored preferences.
 *
 * Runs the same production mapper the Clocks screen uses, so a widget and the
 * app can never disagree about a city's time, then trims it to the widget's
 * shape. Pure, so it is exercised directly by tests.
 */
fun buildWidgetUiState(
    savedCityIds: List<String>,
    use24Hour: Boolean,
    now: Instant,
    deviceZoneId: ZoneId,
    cityLabels: Map<String, String> = emptyMap(),
    maxRows: Int = WIDGET_MAX_ROWS,
): WidgetUiState {
    val state = buildWorldClockUiState(
        savedCityIds = savedCityIds,
        // The widget has no active city: nothing on a home screen is selected.
        activeCityId = null,
        use24Hour = use24Hour,
        now = now,
        deviceZoneId = deviceZoneId,
        cityLabels = cityLabels,
    )

    val deviceWeekday = CityTimeFormatter
        .snapshot(zoneId = deviceZoneId, instant = now, use24Hour = use24Hour)
        .weekdayShort

    val rows = state.cities.take(maxRows).map { city ->
        WidgetRow(
            id = city.id,
            displayName = city.displayName,
            timeText = city.timeText,
            weekdayLabel = city.weekdayLabel,
            isDaytime = city.isDaytime,
            isAnotherDay = city.weekdayLabel != deviceWeekday,
        )
    }

    return WidgetUiState(
        deviceTimeText = state.deviceLocal?.timeText.orEmpty(),
        rows = rows,
        hiddenCount = state.cities.size - rows.size,
    )
}
