package com.worldclock.app.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Turns an IANA timezone plus an instant into the strings the UI shows.
 *
 * All offsets and daylight-saving shifts come from [java.time.ZoneRules] via
 * the zone's rules for that instant — offsets are never hard-coded (spec
 * section 6).
 */
object CityTimeFormatter {

    /** Sun from 06:00, moon from 18:00 (spec section 6). */
    private const val DAY_START_HOUR = 6
    private const val DAY_END_HOUR = 18

    private const val AM = "AM"
    private const val PM = "PM"

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun snapshot(zoneId: ZoneId, instant: Instant, use24Hour: Boolean): CityTime {
        val zoned = instant.atZone(zoneId)
        val hour24 = zoned.hour
        val minute = zoned.minute

        val hourText: String
        val periodText: String?
        if (use24Hour) {
            hourText = twoDigits(hour24)
            periodText = null
        } else {
            hourText = (if (hour24 % 12 == 0) 12 else hour24 % 12).toString()
            periodText = if (hour24 < 12) AM else PM
        }

        val minuteText = twoDigits(minute)
        val timeText = if (periodText == null) {
            "$hourText:$minuteText"
        } else {
            "$hourText:$minuteText $periodText"
        }

        return CityTime(
            offsetLabel = offsetLabel(zoneId, instant),
            timeText = timeText,
            hourText = hourText,
            minuteText = minuteText,
            periodText = periodText,
            weekdayShort = zoned.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            dateText = zoned.format(dateFormat),
            isDaytime = isDaytime(zoneId, instant),
        )
    }

    /**
     * The UTC offset in effect at [instant], e.g. "UTC+9", "UTC-8", "UTC+5:30"
     * or "UTC+0". Minutes are only shown when non-zero.
     */
    fun offsetLabel(zoneId: ZoneId, instant: Instant): String {
        val totalMinutes = zoneId.rules.getOffset(instant).totalSeconds / 60
        val sign = if (totalMinutes < 0) "-" else "+"
        val absolute = abs(totalMinutes)
        val hours = absolute / 60
        val minutes = absolute % 60
        return if (minutes == 0) {
            "UTC$sign$hours"
        } else {
            "UTC$sign$hours:${twoDigits(minutes)}"
        }
    }

    /** True between 06:00 and 17:59 local time. */
    fun isDaytime(zoneId: ZoneId, instant: Instant): Boolean {
        val hour = instant.atZone(zoneId).hour
        return hour in DAY_START_HOUR until DAY_END_HOUR
    }

    private fun twoDigits(value: Int): String = if (value < 10) "0$value" else value.toString()
}
