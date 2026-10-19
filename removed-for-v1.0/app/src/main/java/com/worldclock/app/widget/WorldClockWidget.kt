package com.worldclock.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.worldclock.app.di.appContainer
import com.worldclock.app.ui.mapping.buildWidgetUiState
import com.worldclock.app.ui.widget.WorldClockWidgetContent
import com.worldclock.app.ui.widget.widgetRowBudget
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

/**
 * The home-screen widget (spec section 5.11).
 *
 * Reads the same Proto DataStore the app writes — through the same repository,
 * so a first collection still seeds the first-launch defaults — and hands one
 * frozen snapshot to [WorldClockWidgetContent]. Composition itself therefore
 * does no I/O and no clock reads: every redraw is a fresh, consistent minute.
 *
 * The card's row budget is worked out here, not in the drawing, because it
 * depends on how much space the launcher has given *this* widget: the same card
 * shows fewer cities at its 4×2 footprint and more once the user stretches it.
 *
 * Redraws are driven by [scheduleWidgetTick] (once a minute) and by
 * [WidgetMirror] (immediately, whenever the saved cities change).
 */
class WorldClockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = context.appContainer.clockRepository.preferences.first()

        val heightDp = widgetHeightDp(context, id)
        val maxRows = widgetRowBudget(
            availableHeightDp = heightDp,
            savedCityCount = preferences.savedCityIds.size,
            fontScale = context.resources.configuration.fontScale,
        )
        Log.d(
            WIDGET_TAG,
            "Widget $id is ${if (heightDp > 0) "$heightDp dp tall" else "of unknown height"}" +
                "; drawing $maxRows row(s) of ${preferences.savedCityIds.size} saved cities",
        )

        val state = buildWidgetUiState(
            savedCityIds = preferences.savedCityIds,
            cityLabels = preferences.cityLabels,
            use24Hour = preferences.use24Hour,
            now = Instant.now(),
            // The device's own zone, the same reference the app's cards and the
            // meeting planner use.
            deviceZoneId = ZoneId.systemDefault(),
            maxRows = maxRows,
        )

        provideContent {
            WorldClockWidgetContent(state)
        }
    }
}

/** The tag the widget's one diagnostic line is written under. */
private const val WIDGET_TAG = "WorldClockWidget"

/**
 * The height the launcher has given this widget, in dp, or 0 when it has not
 * said.
 *
 * This is deliberately the platform's own `AppWidgetOptions` rather than Glance's
 * `LocalSize`, which is not populated below API 31. A launcher has carried this
 * height since API 16; on an old one it is a conservative bucket, which costs
 * some unused space at the bottom of the card rather than a row the launcher
 * cuts off.
 */
internal fun widgetHeightDp(context: Context, id: GlanceId): Int {
    val appWidgetId = (id as? AppWidgetId)?.appWidgetId ?: return 0

    val options = runCatching {
        AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    }.getOrNull() ?: return 0

    return options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
}
