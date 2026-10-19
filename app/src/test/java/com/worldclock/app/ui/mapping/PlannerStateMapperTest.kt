package com.worldclock.app.ui.mapping

import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.ui.PlannerUiState
import com.worldclock.app.ui.samples.PreviewSamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * The meeting planner's rows: one instant, read in every saved city at once.
 *
 * Reference slot is 09:00 on 20 Jan 2024 in the samples' device zone
 * (`Asia/Yangon`, UTC+6:30), which is 02:30 UTC.
 */
class PlannerStateMapperTest {

    private fun plannerAt(hour: Int, minute: Int = 0, labels: Boolean = false) =
        PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(hour, minute), labels = labels)

    private fun row(planner: PlannerUiState, id: String) = planner.rows.single { it.id == id }

    @Test
    fun theReferenceReadoutDescribesTheChosenSlotInTheDevicesOwnZone() {
        val planner = plannerAt(9)

        assertEquals(MinuteOfDay.of(9, 0), planner.minuteOfDay)
        assertEquals("09:00", planner.referenceTimeText)
        assertEquals("Sat, 20 Jan", planner.referenceDateText)
        assertEquals("UTC+6:30", planner.referenceOffsetLabel)
    }

    @Test
    fun everySavedCityIsReadAtTheSameInstant() {
        val planner = plannerAt(9)

        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london"),
            planner.rows.map { it.id },
        )
        assertEquals("11:30", row(planner, "tokyo").timeText)
        assertEquals("13:30", row(planner, "sydney").timeText)
        assertEquals("02:30", row(planner, "london").timeText)
    }

    @Test
    fun aCityOnTheOtherSideOfTheDateLineShowsItsOwnWeekday() {
        val planner = plannerAt(9)

        assertEquals("Sat", row(planner, "tokyo").weekdayLabel)
        assertEquals("Sat", row(planner, "london").weekdayLabel)
        // 18:30 and 21:30 the previous evening in California and New York.
        assertEquals("Fri", row(planner, "los-angeles").weekdayLabel)
        assertEquals("Fri", row(planner, "new-york").weekdayLabel)
        assertEquals("18:30", row(planner, "los-angeles").timeText)
        assertEquals("21:30", row(planner, "new-york").timeText)
    }

    @Test
    fun theWorkingDayIsJudgedInEachCitysOwnZone() {
        val nineInTheMorning = plannerAt(9)

        assertTrue(row(nineInTheMorning, "tokyo").isWorkingHours)
        assertTrue(row(nineInTheMorning, "sydney").isWorkingHours)
        // 18:30, 21:30 and 02:30 are not hours to book a meeting in.
        assertFalse(row(nineInTheMorning, "los-angeles").isWorkingHours)
        assertFalse(row(nineInTheMorning, "new-york").isWorkingHours)
        assertFalse(row(nineInTheMorning, "london").isWorkingHours)

        // Later in the day the picture turns over: Asia has gone home while the
        // European morning is getting started.
        val sixInTheEvening = plannerAt(18)
        assertEquals("20:30", row(sixInTheEvening, "tokyo").timeText)
        assertFalse(row(sixInTheEvening, "tokyo").isWorkingHours)
        assertEquals("11:30", row(sixInTheEvening, "london").timeText)
        assertTrue(row(sixInTheEvening, "london").isWorkingHours)
    }

    @Test
    fun everyRowCarriesItsOffsetFromTheReference() {
        val planner = plannerAt(9)

        assertEquals(TimeDifference(150), row(planner, "tokyo").difference)
        assertEquals(TimeDifference(270), row(planner, "sydney").difference)
        assertEquals(TimeDifference(-870), row(planner, "los-angeles").difference)
        assertEquals(TimeDifference(-690), row(planner, "new-york").difference)
        assertEquals(TimeDifference(-390), row(planner, "london").difference)
    }

    @Test
    fun theOffsetIsMeasuredAtTheChosenSlotNotAtNow() {
        // The British clocks go forward at 01:00 UTC on 31 March 2024, so the
        // gap between London and Yangon changes part-way through that morning.
        // A difference carried over from "now" would be an hour out from then on.
        val beforeTheChange = Instant.parse("2024-03-31T00:00:00Z")
        val planner = buildPlannerUiState(
            state = PreviewSamples.clockState(now = beforeTheChange),
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        assertEquals("Sun, 31 Mar", planner.referenceDateText)
        assertEquals("03:30", row(planner, "london").timeText)
        assertEquals(TimeDifference(-330), row(planner, "london").difference)

        // What the same comparison reads at the instant "now" — deliberately
        // different, which is the whole point.
        assertEquals(
            TimeDifference(-390),
            TimeDifference.between(
                ZoneId.of("Europe/London"),
                PreviewSamples.sampleDeviceZoneId,
                beforeTheChange,
            ),
        )
    }

    @Test
    fun everyCityInTheDirectoryIsUsableNotJustTheDefaults() {
        val planner = buildPlannerUiState(
            state = PreviewSamples.clockState(
                savedCityIds = listOf("mumbai", "honolulu", "new-york"),
            ),
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        // 02:30 UTC. Half-hour zones land on the slot exactly — Mumbai is
        // UTC+5:30, so the row reads 08:00 rather than a rounded 08:00-ish.
        assertEquals("08:00", row(planner, "mumbai").timeText)
        assertEquals(TimeDifference(-60), row(planner, "mumbai").difference)
        // Honolulu is a day behind and still on the previous afternoon.
        assertEquals("16:30", row(planner, "honolulu").timeText)
        assertEquals("Fri", row(planner, "honolulu").weekdayLabel)
    }

    @Test
    fun theUserLabelIsPartOfTheRowName() {
        val planner = plannerAt(9, labels = true)

        assertEquals("Tokyo (Head Office)", row(planner, "tokyo").displayName)
        assertEquals("New York (HQ)", row(planner, "new-york").displayName)
        assertEquals("London", row(planner, "london").displayName)
    }

    @Test
    fun theAppWideFormatIsUsedForEveryTime() {
        val planner = PreviewSamples.plannerState(
            minuteOfDay = MinuteOfDay.of(9, 0),
            use24Hour = false,
        )

        assertEquals("9:00 AM", planner.referenceTimeText)
        assertEquals("11:30 AM", row(planner, "tokyo").timeText)
        assertEquals("9:30 PM", row(planner, "new-york").timeText)
    }

    @Test
    fun anEmptyListPlansNothingRatherThanCrashing() {
        val planner = PreviewSamples.emptyPlannerState(minuteOfDay = MinuteOfDay.of(9, 0))

        assertTrue(planner.rows.isEmpty())
        assertEquals("09:00", planner.referenceTimeText)
    }
}
