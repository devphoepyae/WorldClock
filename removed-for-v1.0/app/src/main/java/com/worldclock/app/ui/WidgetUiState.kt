package com.worldclock.app.ui

import androidx.compose.runtime.Immutable

/**
 * One city line on the home-screen widget.
 *
 * Deliberately smaller than a [com.worldclock.app.ui.components.CityCardModel]:
 * a widget has no room for a region or an offset, so it carries only what the
 * card shows at a glance — the name, the time, and whether it is a different
 * day or night over there than it is here.
 */
@Immutable
data class WidgetRow(
    val id: String,
    /** "Tokyo (Head Office)" when a label is set, otherwise just the name. */
    val displayName: String,
    /** e.g. "01:45" */
    val timeText: String,
    /** e.g. "Sat" — the city's own weekday, always carried (see [isAnotherDay]). */
    val weekdayLabel: String,
    /** Sun between 06:00 and 17:59 local time; moon otherwise. */
    val isDaytime: Boolean,
    /**
     * True when the city is on a different calendar day from the device.
     *
     * Without it a bare "01:45" reads as tomorrow morning; with it the weekday
     * prints beside the time, which is the whole reason a world clock is worth
     * putting on a home screen.
     */
    val isAnotherDay: Boolean,
)

/**
 * Everything the home-screen widget draws at one minute.
 *
 * @param deviceTimeText the device's own time, shown in the header as the
 *   anchor every other row is relative to
 * @param hiddenCount cities that did not fit in the widget's row budget
 */
@Immutable
data class WidgetUiState(
    val deviceTimeText: String,
    val rows: List<WidgetRow> = emptyList(),
    val hiddenCount: Int = 0,
) {
    /** No cities are saved, so the widget says so rather than looking broken. */
    val isEmpty: Boolean get() = rows.isEmpty()
}
