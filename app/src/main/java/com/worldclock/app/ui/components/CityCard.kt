package com.worldclock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles

/**
 * Everything a city card needs to render.
 *
 * The clock list and the world map share this model: the list reads the top
 * block, the map uses [latitude]/[longitude] to place the marker and the
 * selected-city card.
 */
@Immutable
data class CityCardModel(
    val id: String,
    /** e.g. "UTC+9" */
    val offsetLabel: String,
    /** e.g. "Thu" */
    val weekdayLabel: String,
    val name: String,
    val region: String,
    /** e.g. "01:40" */
    val timeText: String,
    val isDaytime: Boolean,
    val isActive: Boolean,
    /** The user's own label for this city, e.g. "Head Office"; `null` when unset. */
    val label: String? = null,
    /** How this city's clock compares with the device's own, in whole minutes. */
    val difference: TimeDifference = TimeDifference.None,
    /** Degrees north of the equator; used only to place the map marker. */
    val latitude: Double = 0.0,
    /** Degrees east of the prime meridian; used only to place the map marker. */
    val longitude: Double = 0.0,
) {
    /** "Tokyo (Head Office)" when a label is set, otherwise just the name. */
    val displayName: String get() = label?.let { "$name ($it)" } ?: name
}

/**
 * A single row in the city list.
 *
 * The active city is drawn as a black card with light text; every other city
 * uses the card colour with a hairline border.
 *
 * The bottom-right line compares the city with the device's own time, so the
 * card answers "how far from me is this?" without opening anything.
 */
@Composable
fun CityCard(
    model: CityCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (model.isActive) WorldClockColors.Primary else WorldClockColors.Card
    val contentColor =
        if (model.isActive) WorldClockColors.PrimaryForeground else WorldClockColors.Foreground
    val supportingColor =
        if (model.isActive) WorldClockColors.OnPrimaryMuted else WorldClockColors.MutedForeground

    val differenceText = timeDifferenceLabel(model.difference)
    val differenceDescription =
        stringResource(R.string.a11y_difference_from_your_time, differenceText)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 93.dp)
            .clip(WorldClockShapes.medium)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (model.isActive) WorldClockColors.Primary else WorldClockColors.Border,
                shape = WorldClockShapes.medium,
            )
            .clickable(
                onClickLabel = stringResource(R.string.a11y_open_city_detail, model.name),
                onClick = onClick,
            )
            // 14 dp rather than 18 dp. The difference line makes the card taller
            // than it used to be, and with the min-height clamp the card still
            // measures about 93 dp.
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        // Three independent lines rather than one shared grid: a long label or a
        // long difference then only ever shortens itself, never the city name.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.offsetLabel,
                style = WorldClockTextStyles.CityOffset,
                color = supportingColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (model.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                    ),
                    contentDescription = stringResource(
                        if (model.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                    ),
                    tint = supportingColor,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = model.weekdayLabel,
                    style = WorldClockTextStyles.CityWeekday,
                    color = supportingColor,
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Row {
            Text(
                text = model.displayName,
                style = WorldClockTextStyles.CityName,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = model.timeText,
                style = WorldClockTextStyles.CityTime,
                color = contentColor,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.region,
                style = WorldClockTextStyles.CityRegion,
                color = supportingColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = differenceText,
                style = WorldClockTextStyles.CityDifference,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Spoken in full, because "+2 hrs ahead" on its own does not
                // say what it is ahead of.
                modifier = Modifier.semantics {
                    contentDescription = differenceDescription
                },
            )
        }
    }
}
