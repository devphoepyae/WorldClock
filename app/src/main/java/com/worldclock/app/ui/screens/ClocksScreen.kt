package com.worldclock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.ui.DeviceLocalTime
import com.worldclock.app.ui.WorldClockUiState
import com.worldclock.app.ui.components.CircleIconButton
import com.worldclock.app.ui.components.CityCard
import com.worldclock.app.ui.components.CityCardModel
import com.worldclock.app.ui.components.MinTouchTarget
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import com.worldclock.app.ui.theme.WorldClockTheme

/**
 * "World Time" — the saved city list (spec section 4.1).
 *
 * Handles the loading, populated and empty states.
 */
@Composable
fun ClocksScreen(
    state: WorldClockUiState,
    onCityClick: (CityCardModel) -> Unit,
    onAddClick: () -> Unit,
    onFindCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlanMeetingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WorldClockColors.Background),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ClocksHeader(onAddClick = onAddClick, onSettingsClick = onSettingsClick) }

        item {
            ClocksSectionHeader(
                cityCount = state.cities.size,
                deviceLocal = state.deviceLocal,
            )
        }

        item { PlanMeetingRow(onClick = onPlanMeetingClick) }

        when {
            state.isLoading -> item { LoadingState() }

            state.cities.isEmpty() -> item { EmptyState(onFindCityClick = onFindCityClick) }

            else -> {
                items(items = state.cities, key = { it.id }) { city ->
                    CityCard(model = city, onClick = { onCityClick(city) })
                }
                item { AddCityRow(onClick = onAddClick) }
            }
        }

        item { DstCaption() }
    }
}

@Composable
private fun ClocksHeader(onAddClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.clocks_eyebrow),
                style = WorldClockTextStyles.Eyebrow,
                color = WorldClockColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.clocks_title),
                style = WorldClockTextStyles.ScreenTitle,
                color = WorldClockColors.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        CircleIconButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = stringResource(R.string.a11y_settings),
            onClick = onSettingsClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
        Spacer(Modifier.width(8.dp))
        CircleIconButton(
            iconRes = R.drawable.ic_add,
            contentDescription = stringResource(R.string.a11y_add_city),
            onClick = onAddClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
    }
}

@Composable
private fun ClocksSectionHeader(cityCount: Int, deviceLocal: DeviceLocalTime?) {
    val count = pluralStringResource(R.plurals.clocks_city_count, cityCount, cityCount)
    val caption = deviceLocal?.let { "$count · ${it.timeText}" } ?: count

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.clocks_section_label),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Text(
            text = caption,
            style = WorldClockTextStyles.SectionCount,
            color = WorldClockColors.MutedForeground,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.clocks_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = WorldClockColors.MutedForeground,
        )
    }
}

@Composable
private fun EmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_globe),
            contentDescription = null,
            tint = WorldClockColors.MutedForeground,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.clocks_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.clocks_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = WorldClockColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(WorldClockShapes.medium)
                .clickable(onClick = onFindCityClick, role = Role.Button)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(WorldClockColors.Card)
                    .border(1.dp, WorldClockColors.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = WorldClockColors.Foreground,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.clocks_empty_action),
                style = WorldClockTextStyles.CityName,
                color = WorldClockColors.Foreground,
            )
        }
    }
}

@Composable
private fun AddCityRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WorldClockShapes.medium)
            .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = WorldClockColors.MutedForeground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.clocks_add_city_action),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
        )
    }
}

/**
 * Entry point to the meeting planner.
 *
 * Placed directly under the section header rather than at the foot of the list,
 * because with a full list of cities an action down there would start life off
 * screen. It is deliberately styled like `ADD CITY` so the two read as a pair.
 */
@Composable
private fun PlanMeetingRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WorldClockShapes.medium)
            .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_meeting_planner),
                onClick = onClick,
            )
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = WorldClockColors.MutedForeground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.clocks_plan_action),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
        )
    }
}

@Composable
private fun DstCaption() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.clocks_dst_caption),
            style = WorldClockTextStyles.DstCaption,
            color = WorldClockColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Clocks 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ClocksScreenPreview() {
    WorldClockTheme {
        ClocksScreen(
            state = PreviewSamples.clockState(),
            onCityClick = {},
            onAddClick = {},
            onFindCityClick = {},
            onSettingsClick = {},
            onPlanMeetingClick = {},
        )
    }
}

@Preview(name = "Clocks empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ClocksScreenEmptyPreview() {
    WorldClockTheme {
        ClocksScreen(
            state = PreviewSamples.emptyState(),
            onCityClick = {},
            onAddClick = {},
            onFindCityClick = {},
            onSettingsClick = {},
            onPlanMeetingClick = {},
        )
    }
}
