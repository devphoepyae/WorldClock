package com.worldclock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.domain.time.MinuteOfDay
import com.worldclock.app.domain.time.WORKING_DAY_END_HOUR
import com.worldclock.app.domain.time.WORKING_DAY_START_HOUR
import com.worldclock.app.ui.PlannerCityRow
import com.worldclock.app.ui.PlannerUiState
import com.worldclock.app.ui.components.CircleIconButton
import com.worldclock.app.ui.components.DaySlider
import com.worldclock.app.ui.components.MinTouchTarget
import com.worldclock.app.ui.components.timeDifferenceLabel
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import com.worldclock.app.ui.theme.WorldClockTheme

/**
 * Meeting planner: pick a time of day where you are, see what that is for every
 * saved city, at once.
 *
 * The slider is the whole feature. Dragging it moves the slot the rows are read
 * at, and every row moves with it, so the user can hunt for an hour that lands
 * inside the working day for as many cities as possible.
 *
 * @param onMinuteChange called on every slider movement — the rows are derived
 *   from the slot, so the update is live rather than on release
 */
@Composable
fun MeetingPlannerScreen(
    planner: PlannerUiState,
    onMinuteChange: (MinuteOfDay) -> Unit,
    onBack: () -> Unit,
    onFindCityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorldClockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        CircleIconButton(
            iconRes = R.drawable.ic_back,
            contentDescription = stringResource(R.string.detail_back),
            onClick = onBack,
            size = 44.dp,
            iconSize = 20.dp,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.planner_eyebrow),
            style = WorldClockTextStyles.Eyebrow,
            color = WorldClockColors.MutedForeground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.planner_title),
            style = WorldClockTextStyles.ScreenTitle,
            color = WorldClockColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.planner_local_label),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = planner.referenceTimeText,
            style = WorldClockTextStyles.PlannerClock,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.planner_reference_meta,
                planner.referenceDateText,
                planner.referenceOffsetLabel,
            ),
            style = WorldClockTextStyles.DetailLocalZone,
            color = WorldClockColors.MutedForeground,
        )

        Spacer(Modifier.height(24.dp))

        DaySlider(
            minuteOfDay = planner.minuteOfDay,
            readout = planner.referenceTimeText,
            onMinuteChange = onMinuteChange,
            contentDescription = stringResource(R.string.a11y_meeting_time_slider),
        )

        Spacer(Modifier.height(12.dp))

        WorkingHoursLegend()

        Spacer(Modifier.height(28.dp))

        HorizontalDivider(color = WorldClockColors.Border)

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.planner_section_label),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(4.dp))

        if (planner.rows.isEmpty()) {
            PlannerEmptyState(onFindCityClick = onFindCityClick)
        } else {
            planner.rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = WorldClockColors.Border)
                }
                PlannerRow(row = row)
            }
        }
    }
}

/**
 * Explains the shaded band and the marker on each row. The working day is the
 * planner's own judgement, so it is stated rather than applied silently.
 */
@Composable
private fun WorkingHoursLegend() {
    val range = stringResource(
        R.string.planner_working_range,
        WORKING_DAY_START_HOUR,
        WORKING_DAY_END_HOUR,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(
            filled = true,
            label = stringResource(R.string.planner_legend_working, range),
        )
        LegendItem(
            filled = false,
            label = stringResource(R.string.planner_legend_outside),
        )
    }
}

@Composable
private fun LegendItem(filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WorkingHoursMarker(filled = filled)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = WorldClockTextStyles.DstCaption,
            color = WorldClockColors.MutedForeground,
        )
    }
}

/** Filled inside the working day, outlined outside it. */
@Composable
private fun WorkingHoursMarker(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (filled) WorldClockColors.Primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (filled) WorldClockColors.Primary else WorldClockColors.MutedForeground,
                shape = CircleShape,
            ),
    )
}

/**
 * One city at the chosen slot. Informational, so it is a single merged node:
 * the time, the weekday and whether it is a civilised hour, in one swipe.
 */
@Composable
private fun PlannerRow(row: PlannerCityRow) {
    val workingLabel = stringResource(R.string.planner_within_hours)
    val outsideLabel = stringResource(R.string.planner_outside_hours)
    val description = stringResource(
        R.string.a11y_planner_row,
        row.displayName,
        // The weekday is part of the reading, not decoration: without it a
        // screen-reader user cannot tell that California is still on Friday.
        "${row.weekdayLabel} ${row.timeText}",
        if (row.isWorkingHours) workingLabel else outsideLabel,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayName,
                style = WorldClockTextStyles.PlannerRowName,
                color = WorldClockColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeDifferenceLabel(row.difference),
                style = WorldClockTextStyles.PlannerRowDifference,
                color = WorldClockColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = row.weekdayLabel,
            style = WorldClockTextStyles.CityWeekday,
            color = WorldClockColors.MutedForeground,
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = row.timeText,
            style = WorldClockTextStyles.PlannerRowTime,
            // A time outside the working day is drawn quietly rather than
            // hidden: it is still the answer, just not a convenient one.
            color = if (row.isWorkingHours) {
                WorldClockColors.Foreground
            } else {
                WorldClockColors.MutedForeground
            },
        )

        Spacer(Modifier.width(10.dp))

        WorkingHoursMarker(filled = row.isWorkingHours)
    }
}

@Composable
private fun PlannerEmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.planner_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.planner_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = WorldClockColors.MutedForeground,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(WorldClockShapes.medium)
                .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
                .clickable(onClick = onFindCityClick, role = Role.Button)
                .heightIn(min = MinTouchTarget)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = WorldClockColors.Foreground,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.planner_empty_action),
                style = WorldClockTextStyles.CityName,
                color = WorldClockColors.Foreground,
            )
        }
    }
}

@Preview(name = "Meeting planner 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenPreview() {
    WorldClockTheme {
        MeetingPlannerScreen(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)),
            onMinuteChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}

@Preview(name = "Meeting planner dark 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenDarkPreview() {
    WorldClockTheme(theme = AppTheme.TRUE_DARK) {
        MeetingPlannerScreen(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(18, 30)),
            onMinuteChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}

@Preview(name = "Meeting planner empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenEmptyPreview() {
    WorldClockTheme {
        MeetingPlannerScreen(
            planner = PreviewSamples.emptyPlannerState(),
            onMinuteChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}
