package com.worldclock.app

import android.app.Application
import com.worldclock.app.di.AppContainer
import com.worldclock.app.di.DefaultAppContainer

/**
 * Application entry point.
 *
 * Owns the single [AppContainer] instance for the process. The container is
 * created eagerly in [onCreate] so that it is ready before any Activity or
 * ViewModel asks for a dependency.
 *
 * The app used to arm a home-screen widget here: a minute tick and a mirror that
 * redrew the card whenever the saved cities changed. v1.0 ships without the
 * widget (spec section 5.11), so a launch now starts no background work at all.
 */
class WorldClockApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
