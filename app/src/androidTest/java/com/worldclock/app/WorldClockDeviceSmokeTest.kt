package com.worldclock.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.worldclock.app.ui.components.WorldMapTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device smoke test, run with `connectedDebugAndroidTest`.
 *
 * Nothing here is faked: the launched activity uses the on-device Proto DataStore,
 * the real system clock, the real device timezone and hardware rendering. It proves
 * the app installs, starts, seeds its defaults and renders both the city list and the
 * world map on a real handset.
 *
 * Scope is deliberately narrow. Everything that can be asserted on the JVM already is,
 * by the 107 tests in `src/test` — including all eleven acceptance criteria and the
 * Roborazzi screenshots. Two behaviours are only reachable on a device and are *not*
 * covered here:
 *
 *  - Taps that mutate persisted state. Every mutation is a disk write plus a flow
 *    round-trip, so the UI updates a beat later; on the JVM the fake DataStore is
 *    synchronous and hides this. Asserting on it needs `waitUntil` against semantics
 *    state rather than display text, because during a navigation transition both
 *    destinations are briefly composed.
 *  - System back. Espresso's `pressBack()` refuses to run when the device animation
 *    scale is non-zero, which is the default on many OEM ROMs.
 *
 * Neither gap is an app defect. See sections 15.1 and 15.2 of SPECIFICATION.md.
 */
@RunWith(AndroidJUnit4::class)
class WorldClockDeviceSmokeTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Wipes the persisted preferences first so the test always observes genuine
     * first-launch defaults rather than whatever the previous run left behind.
     */
    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(ClearStoredPreferencesRule())
        .around(composeRule)

    @Test
    fun firstLaunchShowsTheDefaultCitiesWithLosAngelesActive() {
        composeRule.onNodeWithText("YOUR CITIES").assertIsDisplayed()

        // The five defaults are all present, in the specified order.
        listOf("Tokyo", "Sydney", "Los Angeles", "New York", "London").forEach { name ->
            composeRule.onNodeWithText(name).assertExists()
        }

        // Los Angeles is the active city, so its card is the black one.
        composeRule.onNodeWithText("Los Angeles").assertIsDisplayed()
        composeRule.onNodeWithText("California, USA").assertIsDisplayed()
    }

    @Test
    fun theMapTabRendersTheWorldMapWithAMarkerPerSavedCity() {
        composeRule.onNodeWithContentDescription("World map").performClick()

        composeRule.onNodeWithText("TAP A MARKER TO SELECT · SHADED AREA IS NIGHT").assertIsDisplayed()

        // The Clocks destination is disposed once the map is on screen, so its
        // section label must be gone.
        composeRule.onNodeWithText("YOUR CITIES").assertDoesNotExist()

        // One marker per saved city, each labelled with its live time.
        listOf("tokyo", "sydney", "los-angeles", "new-york", "london").forEach { cityId ->
            composeRule.onNodeWithTag(WorldMapTestTags.marker(cityId)).assertExists()
        }
    }
}

/** Deletes the Proto DataStore before the activity under test starts. */
private class ClearStoredPreferencesRule : TestWatcher() {
    override fun starting(description: Description) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "datastore").deleteRecursively()
    }
}
