package com.worldclock.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.worldclock.app.WorldClockApp

/**
 * The widget's entry point: the component the launcher and the platform talk to.
 *
 * Besides the callbacks Glance needs, this receiver owns the minute ticker's
 * lifetime — armed when the first copy of the widget is added, re-armed on every
 * platform update (which is also how it survives a reboot), and cancelled when
 * the last copy is removed. It also starts [WidgetMirror], so a widget added
 * while the app is not running still tracks later changes to the saved cities.
 */
class WorldClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WorldClockWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startMirror(context)
        scheduleWidgetTick(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // The platform asks for an update on every reboot and every resize, and
        // alarms do not survive a reboot: re-arming here is what puts the ticker
        // back after one.
        startMirror(context)
        scheduleWidgetTick(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetTick(context)
    }

    private fun startMirror(context: Context) {
        (context.applicationContext as? WorldClockApp)?.startWidgetMirror()
    }
}
