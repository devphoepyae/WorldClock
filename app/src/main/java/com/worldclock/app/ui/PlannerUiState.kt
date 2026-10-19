package com.worldclock.app.ui

import androidx.compose.runtime.Immutable
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.domain.time.TimeDifference

/**
 * One city's line in the meeting planner: what the clock reads there at the
 * chosen slot, and whether that is a plausible hour to meet.
 */
@Immutable
data class PlannerCityRow(
    val id: String,
    /** Name with the user's own label, e.g. "Tokyo (Head Office)". */
    val displayName: String,
    /** Local time at the chosen slot, e.g. "11:30" or "11:30 AM". */
    val timeText: String,
    /** Weekday there, e.g. "Sun" — the only way to see that a city is a day behind. */
    val weekdayLabel: String,
    /** True when the local time falls inside the working day. */
    val isWorkingHours: Boolean,
    /** The city's offset from the device, at the chosen slot. */
    val difference: TimeDifference,
)

/**
 * Everything the meeting planner renders.
 *
 * @param minuteOfDay the chosen slot, in the device's own timezone
 * @param referenceTimeText the slot as a clock reading, e.g. "09:00"
 * @param referenceDateText the slot's date, e.g. "Sun, 21 Jan"
 * @param referenceOffsetLabel the device's UTC offset at the slot, e.g. "UTC+6:30"
 */
@Immutable
data class PlannerUiState(
    val minuteOfDay: MinuteOfDay = MinuteOfDay.StartOfDay,
    val referenceTimeText: String = "",
    val referenceDateText: String = "",
    val referenceOffsetLabel: String = "",
    val rows: List<PlannerCityRow> = emptyList(),
)
