package com.worldclock.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.worldclock.app.WorldClockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The one action [WidgetTickerReceiver] acts on. */
const val WIDGET_TICK_ACTION: String = "com.worldclock.app.action.WIDGET_TICK"

private const val TICK_REQUEST_CODE = 0x5743
private const val MILLIS_PER_MINUTE = 60_000L

/**
 * The widget's minute ticker.
 *
 * `RemoteViews` cannot tick on their own, so the widget is redrawn by an alarm
 * that re-arms itself at the next minute boundary and only while at least one
 * copy of the widget is on a home screen.
 *
 * [`AlarmManager.setAndAllowWhileIdle`] rather than `setExactAndAllowWhileIdle`,
 * deliberately: the exact variant needs the `SCHEDULE_EXACT_ALARM` special
 * permission on API 31+, and a clock widget is not worth asking the user for it.
 * The trade is that while the device is asleep in Doze the platform throttles the
 * alarm to roughly once every nine minutes, so a widget left alone on a sleeping
 * device can show a stale minute — and catches up as soon as it wakes or is
 * looked at. The trade is recorded in the specification (section 5.11).
 */
fun scheduleWidgetTick(
    context: Context,
    // Injected so the two decisions below can both be exercised: the presence
    // check is the only part that needs a real widget host.
    widgetsExist: Boolean = hasHomeScreenWidgets(context),
) {
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return

    if (!widgetsExist) {
        // Nothing to draw: stop the ticker rather than waking the device for
        // an empty screen.
        alarms.cancel(tickIntent(context))
        return
    }

    alarms.setAndAllowWhileIdle(
        AlarmManager.RTC,
        nextMinuteBoundary(System.currentTimeMillis()),
        tickIntent(context),
    )
}

/**
 * The next whole minute after [nowMillis].
 *
 * A clock that ran to the minute it was already in would be a minute behind, so
 * the boundary is always strictly ahead — including when the tick arrives
 * exactly on one.
 */
internal fun nextMinuteBoundary(nowMillis: Long): Long =
    nowMillis - (nowMillis % MILLIS_PER_MINUTE) + MILLIS_PER_MINUTE

/** Stops the ticker; called when the last widget is removed. */
fun cancelWidgetTick(context: Context) {
    context.getSystemService(AlarmManager::class.java)?.cancel(tickIntent(context))
}

/** True while at least one copy of the widget is on a home screen. */
fun hasHomeScreenWidgets(context: Context): Boolean =
    AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, WorldClockWidgetReceiver::class.java))
        .isNotEmpty()

private fun tickIntent(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        TICK_REQUEST_CODE,
        Intent(context, WidgetTickerReceiver::class.java).setAction(WIDGET_TICK_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

/**
 * Redraws the widget once, then arms the next minute.
 *
 * `updateAll` talks to the widget host, so it is a suspending call that must not
 * run on the main thread: `goAsync` keeps the broadcast alive until it finishes.
 */
class WidgetTickerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WIDGET_TICK_ACTION) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                WorldClockWidget().updateAll(appContext)
                // A tick also means the process is awake and the widget is
                // wanted, so the saved-cities mirror is worth having too.
                (appContext as? WorldClockApp)?.startWidgetMirror()
            } finally {
                pendingResult.finish()
                // Re-armed after the redraw, so a slow redraw cannot make the
                // next minute arrive early and double-up.
                scheduleWidgetTick(appContext)
            }
        }
    }
}
