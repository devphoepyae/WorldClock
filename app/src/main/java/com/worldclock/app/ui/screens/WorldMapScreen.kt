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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.ui.WorldClockUiState
import com.worldclock.app.ui.components.CircleIconButton
import com.worldclock.app.ui.components.CityCardModel
import com.worldclock.app.ui.components.WorldMapCanvas
import com.worldclock.app.ui.components.WorldMapTestTags
import com.worldclock.app.ui.components.timeDifferenceLabel
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import com.worldclock.app.ui.theme.WorldClockTheme

/**
 * "World Time" — the schematic world map (spec section 4.4).
 *
 * Shows the saved cities as markers, highlights the active one with a red
 * meridian and diamond, and surfaces the selected city's live clock. Tapping a
 * marker selects that city; tapping the clock panel opens its detail screen.
 */
@Composable
fun WorldMapScreen(
    state: WorldClockUiState,
    onSelectCity: (String) -> Unit,
    onOpenCity: (String) -> Unit,
    onAddClick: () -> Unit,
    onFindCityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCity = state.cities.firstOrNull { it.isActive } ?: state.cities.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorldClockColors.Background),
        verticalArrangement = Arrangement.Center,
    ) {
        if (activeCity != null) {
            MapCityStrip(
                cities = state.cities,
                activeCityId = activeCity.id,
                onSelectCity = onSelectCity,
                onAddClick = onAddClick,
            )

            Spacer(Modifier.height(24.dp))

            MapSelectedCityPanel(
                city = activeCity,
                onOpenCity = { onOpenCity(activeCity.id) },
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            MapHeader(
                onAddClick = onAddClick,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        WorldMapCanvas(
            cities = state.cities,
            now = state.now,
            onCityClick = { onSelectCity(it.id) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1.7f)
                .testTag(WorldMapTestTags.MAP_CANVAS),
        )

        Spacer(Modifier.height(16.dp))

        if (state.cities.isEmpty()) {
            MapEmptyState(onFindCityClick = onFindCityClick)
        } else {
            MapLegend()
            Spacer(Modifier.height(12.dp))
            MapCaption()
        }
    }
}

@Composable
private fun MapHeader(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.map_eyebrow),
                style = WorldClockTextStyles.Eyebrow,
                color = WorldClockColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.map_title),
                style = WorldClockTextStyles.ScreenTitle,
                color = WorldClockColors.Foreground,
            )
        }
        CircleIconButton(
            iconRes = R.drawable.ic_add,
            contentDescription = stringResource(R.string.a11y_add_city),
            onClick = onAddClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
    }
}

/**
 * Horizontal strip of the saved cities. Behaves like a radio group: exactly one
 * city is active, and the whole strip is exposed to assistive technology as a
 * single selection group.
 */
@Composable
private fun MapCityStrip(
    cities: List<CityCardModel>,
    activeCityId: String?,
    onSelectCity: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Keep the active city on screen: the strip is scrollable and the active
    // city may be the last one.
    LaunchedEffect(activeCityId, cities.size) {
        val index = cities.indexOfFirst { it.id == activeCityId }
        if (index >= 0) listState.scrollToItem(index)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(items = cities, key = { it.id }) { city ->
                CityStripPill(
                    city = city,
                    onClick = { onSelectCity(city.id) },
                    modifier = Modifier.testTag(WorldMapTestTags.cityPill(city.id)),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

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
private fun CityStripPill(
    city: CityCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (city.isActive) WorldClockColors.Primary else WorldClockColors.Card
    val content = if (city.isActive) {
        WorldClockColors.PrimaryForeground
    } else {
        WorldClockColors.Foreground
    }

    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(WorldClockShapes.small)
            .background(container)
            .border(
                width = 1.dp,
                color = if (city.isActive) WorldClockColors.Primary else WorldClockColors.Border,
                shape = WorldClockShapes.small,
            )
            .selectable(
                selected = city.isActive,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = city.displayName.uppercase(),
            style = WorldClockTextStyles.MapStripLabel,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The selected city's live clock. Tapping it opens the city detail screen.
 */
@Composable
private fun MapSelectedCityPanel(
    city: CityCardModel,
    onOpenCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(WorldMapTestTags.SELECTED_CITY_PANEL)
            .clip(WorldClockShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_open_city_detail, city.name),
                onClick = onOpenCity,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (city.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                    ),
                    contentDescription = stringResource(
                        if (city.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                    ),
                    tint = WorldClockColors.MutedForeground,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "${city.offsetLabel} · ${city.weekdayLabel}",
                    style = WorldClockTextStyles.MapCityOffset,
                    color = WorldClockColors.MutedForeground,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = city.timeText,
                style = WorldClockTextStyles.MapCityTime,
                color = WorldClockColors.Foreground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = city.displayName,
                style = WorldClockTextStyles.MapCityName,
                color = WorldClockColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = city.region,
                style = WorldClockTextStyles.MapCityRegion,
                color = WorldClockColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeDifferenceLabel(city.difference),
                style = WorldClockTextStyles.MapCityDifference,
                color = WorldClockColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.map_view_details),
                style = WorldClockTextStyles.SectionLabel,
                color = WorldClockColors.MutedForeground,
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = WorldClockColors.MutedForeground,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(-90f),
            )
        }
    }
}

@Composable
private fun MapCaption() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.map_caption),
            style = WorldClockTextStyles.MapCaption,
            color = WorldClockColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Key for the map: what the red diamond, the dark dot and the shading mean.
 * Text labels rather than colour alone, so the meaning does not depend on
 * colour perception.
 */
@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(labelRes = R.string.map_legend_selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .rotate(45f)
                    .background(WorldClockColors.MapAccent),
            )
        }
        LegendSpacer()
        LegendItem(labelRes = R.string.map_legend_saved) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(WorldClockColors.Foreground),
            )
        }
        LegendSpacer()
        LegendItem(labelRes = R.string.map_legend_night) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    // The wash is drawn over the map, not over the panel it is
                    // listed on, so on the darkest themes the swatch would
                    // otherwise be a black dot on a black background.
                    .background(WorldClockColors.MapNight)
                    .border(1.dp, WorldClockColors.Border, CircleShape),
            )
        }
    }
}

@Composable
private fun LegendSpacer() {
    Spacer(Modifier.width(16.dp))
}

@Composable
private fun LegendItem(
    labelRes: Int,
    swatch: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            style = WorldClockTextStyles.MapCaption,
            color = WorldClockColors.MutedForeground,
            maxLines = 1,
        )
    }
}

@Composable
private fun MapEmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.map_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.map_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = WorldClockColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(WorldClockShapes.medium)
                .background(WorldClockColors.Card)
                .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
                .clickable(
                    onClickLabel = stringResource(R.string.clocks_empty_action),
                    role = Role.Button,
                    onClick = onFindCityClick,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WorldClockColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = WorldClockColors.Foreground,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.map_empty_action),
                style = WorldClockTextStyles.CityName,
                color = WorldClockColors.Foreground,
            )
        }
    }
}

@Preview(name = "Map 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WorldMapScreenPreview() {
    WorldClockTheme {
        WorldMapScreen(
            state = PreviewSamples.clockState(),
            onSelectCity = {},
            onOpenCity = {},
            onAddClick = {},
            onFindCityClick = {},
        )
    }
}

@Preview(name = "Map empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WorldMapScreenEmptyPreview() {
    WorldClockTheme {
        WorldMapScreen(
            state = PreviewSamples.emptyState(),
            onSelectCity = {},
            onOpenCity = {},
            onAddClick = {},
            onFindCityClick = {},
        )
    }
}
