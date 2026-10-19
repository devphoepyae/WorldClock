package com.worldclock.app.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worldclock.app.MainActivity
import com.worldclock.app.R
import com.worldclock.app.ui.WidgetRow
import com.worldclock.app.ui.WidgetUiState
import com.worldclock.app.ui.theme.DarkWorldClockPalette
import com.worldclock.app.ui.theme.LightWorldClockPalette

/**
 * The widget's drawing, kept apart from the widget itself so Glance's own unit
 * test harness can compose it directly.
 *
 * Glance composables are not Compose composables: they compile to `RemoteViews`,
 * which is why this cannot be screenshotted the way the app's screens are, and
 * why it is asserted on instead (spec section 12.3).
 *
 * The widget follows the device's light/dark setting rather than the app's
 * stored choice: a home screen belongs to the launcher, and a widget that
 * ignored the system theme would be the odd one out among its neighbours.
 */
@Composable
@GlanceComposable
fun WorldClockWidgetContent(
    state: WidgetUiState,
    modifier: GlanceModifier = GlanceModifier,
) {
    // Two containers rather than one, and the split is the point. The outer box
    // *is* the card: it is the view the launcher treats as the widget's
    // background, so the solid fill and the corner radius belong on it. The tap
    // goes on the column inside, because Glance lifts a clickable element into a
    // container of its own — anything left on the card here would be drawn
    // inside that lifted container rather than as the card itself.
    Box(
        modifier = modifier
            .fillMaxSize()
            // Names this view as the widget's background, which is what lets a
            // launcher round its corners and cross-fade it. Without it the
            // background a launcher reserves for a widget can stay transparent
            // and the card appears to float on the wallpaper.
            .appWidgetBackground()
            // A solid card, never the wallpaper: the app's own card token,
            // resolved against the device's light or dark setting so the widget
            // matches its neighbours on either home screen.
            .background(WidgetColors.Surface)
            .cornerRadius(WidgetGeometry.CornerRadius),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    horizontal = WidgetGeometry.HorizontalPadding,
                    vertical = WidgetGeometry.VerticalPadding,
                )
                // A tap anywhere on the card is never a dead end.
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            WidgetHeader(state.deviceTimeText)

            Spacer(GlanceModifier.height(WidgetGeometry.GapAboveRule))
            WidgetHairline()
            Spacer(GlanceModifier.height(WidgetGeometry.GapBelowRule))

            if (state.isEmpty) {
                WidgetEmptyState()
            } else {
                state.rows.forEach { row ->
                    WidgetCityRow(row)
                    Spacer(GlanceModifier.height(WidgetGeometry.RowSpacing))
                }
                if (state.hiddenCount > 0) {
                    WidgetHiddenCount(state.hiddenCount)
                }
            }

            // Pushes everything up, so a widget the user has stretched taller
            // keeps its content anchored to the top instead of floating in the
            // middle.
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetHeader(deviceTimeText: String) {
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                fontSize = WidgetGeometry.HeaderSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.Supporting,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            // The device's own clock, so every row below has something to be
            // relative to. Without it a row reading "01:45" is unanchored.
            text = deviceTimeText,
            style = TextStyle(
                fontSize = WidgetGeometry.HeaderSizeSp.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetColors.Supporting,
            ),
            maxLines = 1,
        )
    }
}

/**
 * One city.
 *
 * The children are left to be announced in reading order rather than merged
 * into a single description: `RemoteViews` has no equivalent of Compose's
 * merged semantics. That still reads correctly, because the weekday is only
 * hidden when the city shares the device's day — so whatever is on screen is
 * also what is spoken.
 */
@Composable
@GlanceComposable
private fun WidgetCityRow(row: WidgetRow) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth(),
    ) {
        Image(
            provider = ImageProvider(
                if (row.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
            ),
            contentDescription = context.getString(
                if (row.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
            ),
            colorFilter = ColorFilter.tint(WidgetColors.Supporting),
            modifier = GlanceModifier.size(12.dp),
        )

        Spacer(GlanceModifier.width(8.dp))

        Text(
            text = row.displayName,
            style = TextStyle(
                fontSize = WidgetGeometry.NameSizeSp.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetColors.Content,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        Spacer(GlanceModifier.width(6.dp))

        if (row.isAnotherDay) {
            Text(
                // Only the exception is labelled: on the day everyone shares,
                // the weekday would be noise.
                text = row.weekdayLabel,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.Supporting,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
        }

        Text(
            text = row.timeText,
            style = TextStyle(
                fontSize = WidgetGeometry.TimeSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.Content,
            ),
            maxLines = 1,
        )
    }
}

@Composable
@GlanceComposable
private fun WidgetEmptyState() {
    val context = LocalContext.current

    Text(
        text = context.getString(R.string.widget_empty),
        style = TextStyle(
            fontSize = WidgetGeometry.CountSizeSp.sp,
            fontWeight = FontWeight.Normal,
            color = WidgetColors.Supporting,
        ),
        maxLines = 2,
    )
}

@Composable
@GlanceComposable
private fun WidgetHiddenCount(hiddenCount: Int) {
    val context = LocalContext.current

    Text(
        // Counted, not dropped: a widget that silently omitted cities would be
        // lying about what the app holds.
        text = context.resources.getQuantityString(
            R.plurals.widget_more_cities,
            hiddenCount,
            hiddenCount,
        ),
        style = TextStyle(
            fontSize = WidgetGeometry.CountSizeSp.sp,
            fontWeight = FontWeight.Medium,
            color = WidgetColors.Supporting,
        ),
        maxLines = 1,
    )
}

/** A one-pixel rule, standing in for the Material divider the app never uses. */
@Composable
@GlanceComposable
private fun WidgetHairline() {
    Spacer(
        GlanceModifier
            .fillMaxWidth()
            .height(WidgetGeometry.RuleHeight)
            .background(WidgetColors.Hairline),
    )
}

/** The app's own tokens, offered to Glance as a day/night pair. */
private object WidgetColors {
    val Surface = dayNight(LightWorldClockPalette.card, DarkWorldClockPalette.card)
    val Content = dayNight(LightWorldClockPalette.foreground, DarkWorldClockPalette.foreground)
    val Supporting = dayNight(
        LightWorldClockPalette.mutedForeground,
        DarkWorldClockPalette.mutedForeground,
    )
    val Hairline = dayNight(LightWorldClockPalette.border, DarkWorldClockPalette.border)
}

private fun dayNight(day: Color, night: Color): ColorProvider =
    DayNightColorProvider(day = day, night = night)
