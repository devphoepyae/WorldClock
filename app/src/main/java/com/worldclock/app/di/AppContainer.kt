package com.worldclock.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.worldclock.app.WorldClockApp
import com.worldclock.app.data.prefs.SavedCities
import com.worldclock.app.data.prefs.UserPreferencesSerializer
import com.worldclock.app.data.repository.ClockRepository
import com.worldclock.app.domain.time.SecondTicker

/**
 * Lightweight manual dependency container.
 *
 * The app is intentionally small, so it uses plain constructor injection wired
 * by hand instead of a DI framework. Every instance is process-scoped and
 * created lazily.
 */
interface AppContainer {
    /** Application context, used for DataStore and other process-scoped needs. */
    val applicationContext: Context

    /** Saved cities, active city and display preference. */
    val clockRepository: ClockRepository

    /** One-second, second-aligned time source. */
    val secondTicker: SecondTicker
}

/**
 * Default [AppContainer] used at runtime.
 */
class DefaultAppContainer(
    override val applicationContext: Context,
) : AppContainer {

    private val userPreferencesDataStore: DataStore<SavedCities> by lazy {
        DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SavedCities.getDefaultInstance() },
            produceFile = { applicationContext.dataStoreFile(DATA_STORE_FILE_NAME) },
        )
    }

    override val clockRepository: ClockRepository by lazy {
        ClockRepository(userPreferencesDataStore)
    }

    override val secondTicker: SecondTicker by lazy { SecondTicker() }

    private companion object {
        const val DATA_STORE_FILE_NAME = "user_preferences.pb"
    }
}

/**
 * Returns the process-wide [AppContainer] from any [Context] inside the app.
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as WorldClockApp).container
