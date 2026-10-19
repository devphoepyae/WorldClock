package com.worldclock.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The planner's arithmetic: the 24-hour scale, the 15-minute grid, the
 * working-day rule, and the two daylight-saving corners.
 */
class MeetingTimeTest {

    private val yangon = ZoneId.of("Asia/Yangon")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun hoursAndMinutesSplitTheDayUp() {
        val time = MinuteOfDay.of(9, 45)

        assertEquals(585, time.value)
        assertEquals(9, time.hours)
        assertEquals(45, time.minutes)

        assertEquals(0, MinuteOfDay.StartOfDay.hours)
        assertEquals(12, MinuteOfDay.Noon.hours)
        assertEquals(23, MinuteOfDay(MinuteOfDay.MAX_VALUE).hours)
        assertEquals(59, MinuteOfDay(MinuteOfDay.MAX_VALUE).minutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aTimeOfDayBeyondMidnightIsRejected() {
        MinuteOfDay(1440)
    }

    @Test
    fun theGridIsEveryQuarterHourAndStopsAtTheLastSlotOfTheDay() {
        assertEquals(540, MinuteOfDay.of(9, 7).snappedToGrid().value)
        assertEquals(555, MinuteOfDay.of(9, 8).snappedToGrid().value)
        assertEquals(15, MinuteOfDay.of(9, 8).snappedToGrid().minutes)
        assertEquals(540, MinuteOfDay.of(9, 0).snappedToGrid().value)

        // 23:59 snaps back to the last slot rather than rolling into tomorrow.
        assertEquals(1425, MinuteOfDay(1439).snappedToGrid().value)
        assertEquals(23, MinuteOfDay(1425).snappedToGrid().hours)
        assertEquals(45, MinuteOfDay(1425).snappedToGrid().minutes)

        // Already on the grid: unchanged, so dragging cannot drift.
        assertEquals(585, MinuteOfDay(585).snappedToGrid().value)
    }

    @Test
    fun theGridCoversTheWholeDayInQuarterHourSteps() {
        assertEquals(1425, MinuteOfDay.LAST_GRID_VALUE)
        // Compose counts the steps *between* the two ends, hence 94 for the 96
        // quarter hours from 00:00 to 23:45.
        assertEquals(94, MinuteOfDay.GRID_STEPS)

        var count = 0
        for (value in 0..MinuteOfDay.LAST_GRID_VALUE step MinuteOfDay.STEP_MINUTES) count++
        assertEquals(96, count)
        assertEquals(MinuteOfDay.GRID_STEPS + 2, count)
    }

    @Test
    fun aSlotBecomesAnInstantInTheGivenZone() {
        val date = LocalDate.of(2024, 1, 20)

        assertEquals(
            Instant.parse("2024-01-20T02:30:00Z"),
            MinuteOfDay.of(9, 0).onDate(date, yangon),
        )
        // 09:00 in New York is 14:00 UTC that day.
        assertEquals(
            Instant.parse("2024-01-20T14:00:00Z"),
            MinuteOfDay.of(9, 0).onDate(date, newYork),
        )
    }

    @Test
    fun aSlotInTheSpringForwardGapMovesForwardByTheLengthOfTheGap() {
        // New York loses 02:00–02:59 on 10 March 2024, so 02:30 does not exist.
        val instant = MinuteOfDay.of(2, 30).onDate(LocalDate.of(2024, 3, 10), newYork)

        assertEquals(Instant.parse("2024-03-10T07:30:00Z"), instant)
        // Which reads as 03:30 local — the clock simply skips the missing hour.
        assertEquals(3, MinuteOfDay.ofInstant(instant, newYork).hours)
    }

    @Test
    fun aSlotTheClocksPassThroughTwiceTakesTheEarlierOffset() {
        // New York repeats 01:00–01:59 on 3 November 2024.
        val instant = MinuteOfDay.of(1, 30).onDate(LocalDate.of(2024, 11, 3), newYork)

        assertEquals(Instant.parse("2024-11-03T05:30:00Z"), instant)
        assertEquals(MinuteOfDay.of(1, 30), MinuteOfDay.ofInstant(instant, newYork))
    }

    @Test
    fun theInstantRoundTripsBackToTheSameSlot() {
        val date = LocalDate.of(2024, 1, 20)
        val zones = listOf("Asia/Yangon", "Asia/Kolkata", "Asia/Kathmandu", "UTC")

        for (zone in zones) {
            val zoneId = ZoneId.of(zone)
            for (hour in 0..23) {
                val slot = MinuteOfDay.of(hour, 30)
                assertEquals(slot, MinuteOfDay.ofInstant(slot.onDate(date, zoneId), zoneId))
            }
        }
    }

    @Test
    fun theWorkingDayIsNineToSixInclusiveOfNineAndExclusiveOfSix() {
        assertFalse(isWithinWorkingDay(MinuteOfDay.of(8, 59)))
        assertTrue(isWithinWorkingDay(MinuteOfDay.of(9, 0)))
        assertTrue(isWithinWorkingDay(MinuteOfDay.of(12, 0)))
        assertTrue(isWithinWorkingDay(MinuteOfDay.of(17, 59)))
        assertFalse(isWithinWorkingDay(MinuteOfDay.of(18, 0)))
        assertFalse(isWithinWorkingDay(MinuteOfDay.of(23, 45)))
        assertFalse(isWithinWorkingDay(MinuteOfDay.StartOfDay))
    }

    @Test
    fun theWorkingDayIsReadInTheCitysOwnZoneNotTheReferences() {
        // 09:00 in Yangon is 21:30 the previous day in New York.
        val nineInYangon = MinuteOfDay.of(9, 0).onDate(LocalDate.of(2024, 1, 20), yangon)

        assertTrue(isWithinWorkingDay(nineInYangon, yangon))
        assertFalse(isWithinWorkingDay(nineInYangon, newYork))
    }
}
