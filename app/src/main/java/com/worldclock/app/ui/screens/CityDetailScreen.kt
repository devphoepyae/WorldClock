package com.worldclock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.data.model.City
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.domain.time.CityTimeFormatter
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.ui.DeviceLocalTime
import com.worldclock.app.ui.components.CircleIconButton
import com.worldclock.app.ui.components.MinTouchTarget
import com.worldclock.app.ui.components.SegmentedOption
import com.worldclock.app.ui.components.WorldClockSegmentedControl
import com.worldclock.app.ui.components.timeDifferenceLabel
import com.worldclock.app.ui.samples.PreviewSamples
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import com.worldclock.app.ui.theme.WorldClockTheme
import java.time.Instant
import java.time.ZoneId

/**
 * City detail: the city's live clock, the 12h/24h switch, how it compares with
 * the device's own time, the user's own label for it, and the map / remove
 * actions (spec section 4.3).
 */
@Composable
fun CityDetailScreen(
    city: City,
    now: Instant,
    use24Hour: Boolean,
    deviceLocal: DeviceLocalTime?,
    label: String?,
    onBack: () -> Unit,
    onToggleFormat: (Boolean) -> Unit,
    onEditLabel: () -> Unit,
    onViewOnMap: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = remember(city.id) { ZoneId.of(city.timeZone) }

    val time = remember(city.id, now, use24Hour) {
        CityTimeFormatter.snapshot(zoneId = zoneId, instant = now, use24Hour = use24Hour)
    }

    // The device's own zone only leaves the loading state once, but it is keyed
    // so a travelling user gets the new difference as soon as the platform
    // reports it.
    val deviceZoneId = remember(deviceLocal?.zoneId) {
        deviceLocal?.let { ZoneId.of(it.zoneId) }
    }
    val difference = remember(city.id, now, deviceZoneId) {
        deviceZoneId?.let { TimeDifference.between(zoneId, it, now) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorldClockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                iconRes = R.drawable.ic_back,
                contentDescription = stringResource(R.string.detail_back),
                onClick = onBack,
                size = 44.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.weight(1f))
            WorldClockSegmentedControl(
                options = listOf(
                    SegmentedOption(value = false, label = stringResource(R.string.format_12h)),
                    SegmentedOption(value = true, label = stringResource(R.string.format_24h)),
                ),
                selectedValue = use24Hour,
                onValueSelected = onToggleFormat,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = time.offsetLabel,
            style = WorldClockTextStyles.CityOffset,
            color = WorldClockColors.MutedForeground,
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column {
                Text(
                    text = time.hourText,
                    style = WorldClockTextStyles.DetailClock,
                    color = WorldClockColors.Foreground,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = time.minuteText,
                        style = WorldClockTextStyles.DetailClock,
                        color = WorldClockColors.Foreground,
                    )
                    if (time.periodText != null) {
                        Text(
                            text = time.periodText,
                            style = WorldClockTextStyles.DetailClockPeriod,
                            color = WorldClockColors.MutedForeground,
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "${time.weekdayShort},",
                    style = WorldClockTextStyles.DetailDate,
                    color = WorldClockColors.MutedForeground,
                )
                Text(
                    text = time.dateText,
                    style = WorldClockTextStyles.DetailDate,
                    color = WorldClockColors.MutedForeground,
                )
                Spacer(Modifier.height(10.dp))
                Icon(
                    painter = painterResource(
                        if (time.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                    ),
                    contentDescription = stringResource(
                        if (time.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                    ),
                    tint = WorldClockColors.MutedForeground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = city.name,
            style = WorldClockTextStyles.DetailCityName,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = city.region,
            style = WorldClockTextStyles.DetailCityRegion,
            color = WorldClockColors.MutedForeground,
        )

        if (label != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.detail_city_label_format, label),
                style = WorldClockTextStyles.DetailCityLabel,
                color = WorldClockColors.Foreground,
            )
        }

        Spacer(Modifier.height(14.dp))
        EditLabelAction(cityName = city.name, hasLabel = label != null, onClick = onEditLabel)

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = WorldClockColors.Border)
        Spacer(Modifier.height(20.dp))

        // The reference is named outright, because "+2 hrs ahead" on its own
        // does not say what it is ahead of.
        if (difference != null) {
            Text(
                text = stringResource(R.string.detail_difference_label),
                style = WorldClockTextStyles.SectionLabel,
                color = WorldClockColors.MutedForeground,
                // A section header, so screen-reader users can jump between the
                // three blocks of this screen rather than swiping through them.
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = timeDifferenceLabel(difference),
                style = WorldClockTextStyles.DetailDifference,
                color = WorldClockColors.Foreground,
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = WorldClockColors.Border)
            Spacer(Modifier.height(20.dp))
        }

        Text(
            text = stringResource(R.string.detail_local_time_label),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        if (deviceLocal != null) {
            Text(
                text = "${deviceLocal.zoneId} · ${deviceLocal.offsetLabel}",
                style = WorldClockTextStyles.DetailLocalZone,
                color = WorldClockColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = deviceLocal.timeText,
                style = WorldClockTextStyles.DetailLocalTime,
                color = WorldClockColors.Foreground,
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onViewOnMap,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = WorldClockShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = WorldClockColors.Primary,
                contentColor = WorldClockColors.PrimaryForeground,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_globe),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.detail_view_on_map),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRemove,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = WorldClockShapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = WorldClockColors.MapAccent,
            ),
            border = BorderStroke(1.dp, WorldClockColors.MapAccent),
        ) {
            Text(
                text = stringResource(R.string.detail_remove),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * "ADD LABEL" / "EDIT LABEL": a small text action under the city name, which is
 * where the label itself is shown. Kept as a quiet text row rather than a
 * button so the screen still has one clear primary action.
 */
@Composable
private fun EditLabelAction(
    cityName: String,
    hasLabel: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(WorldClockShapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_edit_city_label, cityName),
                role = Role.Button,
                onClick = onClick,
            )
            // The row is a bare text action, so its drawn height is small; the
            // target is padded out to the minimum a finger needs (spec 9).
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = WorldClockColors.MutedForeground,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (hasLabel) R.string.label_edit else R.string.label_add),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
        )
    }
}

@Preview(name = "City detail 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CityDetailScreenPreview() {
    val state = PreviewSamples.labelledState()
    WorldClockTheme {
        CityDetailScreen(
            city = CityDirectory.find("tokyo")!!,
            now = state.now,
            use24Hour = state.use24Hour,
            deviceLocal = state.deviceLocal,
            label = "Head Office",
            onBack = {},
            onToggleFormat = {},
            onEditLabel = {},
            onViewOnMap = {},
            onRemove = {},
        )
    }
}
