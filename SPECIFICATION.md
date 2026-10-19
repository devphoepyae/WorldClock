# World Clock — Living Specification

> **Document status:** Living document. It is updated after every working session.
> **App id:** `com.worldclock.app`
> **Platform:** Native Android — Kotlin + Jetpack Compose (no React Native, no Expo, no WebView, no web page)
> **Phase:** 2 — complete: five switchable themes, time differences, city labels, meeting planner and the home-screen widget, with the integration/accessibility pass finished. Phase 1 (clocks + map, stages 0–3) is complete and verified.
> **Last updated:** 2026-10-18

---

## 0. How to read this document

This file is the single source of truth for what the app must do and what has actually
been built. It has three kinds of content, and they are labelled separately:

| Marker | Meaning |
| --- | --- |
| **REQUIREMENT** | A rule that must hold. Changing one requires an explicit decision. |
| **IMPLEMENTED** | What the code does today. Verified against the source tree. |
| **OPEN** | Unresolved. Needs a human decision. |

### 0.1 Section 11 provenance — RESOLVED

The original specification was supplied in conversation and the verbatim text of
**section 11 (acceptance criteria)** was not preserved in the working context, so the list in
[section 11](#11-acceptance-criteria) was reconstructed from the requirements that *were*
preserved.

**RESOLVED — 2026-10-17.** The user reviewed the reconstruction and confirmed it as correct
("လုံးဝ မှန်ကန်ပါတယ်" — *entirely correct*), with no changes requested. The
`criterionNN_*` test names are therefore final and the criteria are now REQUIREMENTs, not a
pending reconstruction.

### 0.2 Naming convention used for tests

Acceptance tests are named `criterionNN_description` so that a failing build points
directly at the requirement it breaks.

---

## 1. Product overview

A world clock: the user keeps an ordered list of cities, sees each city's current local
time, and can explore where those cities sit on a schematic globe with a live day/night
terminator.

Everything is offline. There is no backend, no account, no cloud sync, no push
notification, no weather, no online map tiles and no map-provider API key.

**REQUIREMENT — distribution:** local/debug builds only at this stage.

**REQUIREMENT — reference designs:** `sample_01.jpg` and `sample_02.jpg` in the project
root are the source of truth for layout. They must not be deleted.

---

## 2. Non-negotiable constraints

These were agreed up front and are not open for reinterpretation during implementation.

1. **Native Android.** Kotlin + Jetpack Compose.
2. **Material 3 for behaviour and accessibility only** — never for colour. The Material 3
   default purple/blue accent must not appear anywhere.
3. **Monochrome palette only.** No purple, no blue accent, no gradients. Phase 1 was
   light-only; phase 2 adds a true-dark palette (see [section 7.1](#71-colour)).
4. **No backend, no API, no accounts, no cloud sync, no push, no weather.**
5. **No online map tiles, no map provider, no API key.** The map is drawn in Compose
   `Canvas` from bundled vector data.
6. **Timezone maths uses `java.time`** (`ZoneId`, `ZonedDateTime`, `ZoneRules`) with
   **IANA ids only**. UTC offsets are **never** hard-coded; DST is automatic.
7. **Sun icon for local hour 06:00–17:59, moon icon for 18:00–05:59.**
8. **Persistence via Jetpack Proto DataStore:** ordered saved-city id list, active city id,
   `use24Hour`, and the theme mode. Restored on launch. Unknown ids are dropped. Failures
   surface a non-blocking, understandable message — never a silent swallow.
9. **English UI labels. Emoji are never used as UI icons.** Icon-only controls carry
   accessibility labels; selected and disabled states carry semantics.
10. **Must lay out on a 360 dp wide portrait screen without text clipping.**
11. **Inter typography** (Regular / Medium / SemiBold / Bold); base corner radius **16 dp**;
    city card height **≈ 93 dp**; horizontal page padding **≈ 24 dp**.

### 2.1 Agreed decisions (user-approved)

| Decision | Value |
| --- | --- |
| `applicationId` | `com.worldclock.app` |
| `minSdk` | 26 |
| `compileSdk` / `targetSdk` | 34 |
| 12h / 24h | Applies app-wide **and** persists |
| Map day/night shading | Included |
| Verification | JVM screenshot tests **plus** device end-to-end |
| Dependency injection | Manual `AppContainer` — **confirmed by the user**; Hilt rejected (keeps the app light, no APK growth, faster compile, existing tests already pass) |
| Instrumented / Espresso tests | **Deprioritised by the user** — flaky across OEM animation and timing settings. Unit + Roborazzi tests carry the coverage; the device suite is a minimal smoke test |
| Acceptance criteria | Section 11 wording **confirmed by the user** — see [section 0.1](#01-section-11-provenance--resolved) |
| Themes | **Five**, one tap each, persisted, default Light Monochrome (**user choice**, 2026-10-18 — replaced `System` / `Light` / `Dark`, so device-following is gone) |
| Theme picker location | Settings sheet opened from a gear action on the Clocks screen (**user choice**, 2026-10-17) |
| Theme tokens | Light Monochrome `#F3F3F1` / `#111111`; True Dark `#111111` / `#E1E1DE`; Warm Sepia `#F7F4EF` / `#2C2521`; Cool Slate `#EEF2F5` / `#1E293B`; High Contrast `#000000` / `#FFFFFF` (**user choice**, 2026-10-18) |
| Widget palette | Follows the **device's** light/dark setting, not the chosen theme — pre-existing behaviour, kept pending a decision (**user question outstanding**) |

---

## 3. Technology stack

**IMPLEMENTED** — exact versions in use.

### 3.1 Build

| Component | Version | Where |
| --- | --- | --- |
| Gradle wrapper | 8.11.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.7.3 | `build.gradle.kts` |
| Kotlin | 2.0.21 | `build.gradle.kts` |
| Compose compiler plugin | `org.jetbrains.kotlin.plugin.compose` 2.0.21 | `build.gradle.kts` |
| protobuf-gradle-plugin | 0.9.4 | `build.gradle.kts` |
| `buildToolsVersion` | 34.0.0 (pinned to the machine's install) | `app/build.gradle.kts` |
| JDK / `jvmTarget` | 17 | `app/build.gradle.kts` |
| Version catalog | none — versions are inline | — |

### 3.2 Runtime dependencies

| Artifact | Version |
| --- | --- |
| `androidx.compose:compose-bom` | 2024.09.00 |
| `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview` | from BOM |
| `androidx.compose.material3:material3` | from BOM |
| `androidx.core:core-ktx` | 1.13.1 |
| `androidx.activity:activity-compose` | 1.9.3 |
| `androidx.lifecycle:lifecycle-runtime-ktx` / `-runtime-compose` / `-viewmodel-compose` | 2.8.7 |
| `androidx.navigation:navigation-compose` | 2.8.4 |
| `androidx.datastore:datastore` | 1.1.1 |
| `androidx.glance:glance-appwidget` | 1.1.1 |
| `com.google.protobuf:protobuf-javalite` | 3.25.5 |
| `com.google.protobuf:protoc` | 3.25.5 |

### 3.3 Test dependencies

| Artifact | Version | Scope |
| --- | --- | --- |
| `junit:junit` | 4.13.2 | `testImplementation` |
| `androidx.compose.ui:ui-test-junit4` | from BOM | `test` + `androidTest` |
| `androidx.test.ext:junit` | 1.2.1 | `test` + `androidTest` |
| `org.robolectric:robolectric` | 4.14.1 | `testImplementation` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.8.1 | `testImplementation` |
| `io.github.takahirom.roborazzi:roborazzi` | 1.26.0 | `testImplementation` |
| `io.github.takahirom.roborazzi:roborazzi-compose` | 1.26.0 | `testImplementation` |
| `androidx.glance:glance-appwidget-testing` | 1.1.1 | `testImplementation` |
| `androidx.test:runner` | 1.6.2 | `androidTestImplementation` |
| `androidx.compose.ui:ui-test-manifest` | from BOM | `debugImplementation` |
| `androidx.compose.ui:ui-tooling` | from BOM | `debugImplementation` |

**Decision record:** `androidx.test:rules` and `espresso-core` were removed from
`androidTestImplementation`. They are no longer used: the device suite drives the app through
Compose's own test APIs, and Espresso's `pressBack()` is unusable on this device anyway
(see [section 15.1](#151-resolved--instrumented-test-defects)).

### 3.4 Assets

| Asset | Source |
| --- | --- |
| Inter Regular / Medium / SemiBold / Bold | `rsms/inter` v4.1 release, bundled as `res/font/inter_*.ttf` |
| Icons | Material Design Icons *outlined* 24 px SVGs hand-converted to vector drawables (`material-icons-extended` is **not** used) |

---

## 4. Architecture and directory layout

**IMPLEMENTED** — single Gradle module `:app`.

```text
app/src/main/
  proto/saved_cities.proto                  Proto DataStore schema
  java/com/worldclock/app/
    WorldClockApp.kt                        Application; owns the AppContainer
    MainActivity.kt                         Single activity; enableEdgeToEdge()
    di/AppContainer.kt                      Manual DI + Context.appContainer
    data/
      model/City.kt                         City(id, name, region, country, timeZone, lat, lon)
      model/CityDirectory.kt                The 22-city offline directory + search
      prefs/UserPreferences.kt              Domain model of persisted state
      prefs/AppTheme.kt                     The five themes (id 0-4) + proto mapping + migration
      prefs/UserPreferencesSerializer.kt    DataStore Serializer
      repository/ClockRepository.kt         add / remove / select / setUse24Hour / setTheme / setCityLabel / prune
      repository/PrefsError.kt              Error type surfaced to the UI
    domain/
      time/SecondTicker.kt                  Emits the current Instant once per second
      time/CityTime.kt                      Per-city computed time value object
      time/CityTimeFormatter.kt             IANA-aware formatting + offset labels
      time/TimeDifference.kt                Offset gap between two zones at one instant
      time/MeetingTime.kt                   MinuteOfDay value class + working-day rule
      map/GeoPoint.kt                       lat/lon pair
      map/EquirectangularProjection.kt      x/y fractions for lat/lon
      map/SolarPosition.kt                  Subsolar point, terminator, isDaylight
      map/WorldLandmasses.kt                26 hand-authored landmass outlines
    widget/
      WorldClockWidget.kt                   GlanceAppWidget; reads the same repository the screens read
      WorldClockWidgetReceiver.kt           AppWidgetProvider -> GlanceAppWidgetReceiver
      WidgetTicker.kt                       Minute-boundary alarm, its receiver, hasHomeScreenWidgets()
      WidgetMirror.kt                       Redraws the widget when the saved cities or format change
    ui/
      WorldClockRoot.kt                     Scaffold shell, NavHost, search + settings sheets, label dialog, snackbar
      WorldClockViewModel.kt                State holder
      WorldClockUiState.kt                  UI state model
      PlannerUiState.kt                     Meeting planner state model
      WidgetUiState.kt                      Home-screen widget state model
      mapping/WorldClockStateMapper.kt      buildWorldClockUiState — the only path from data to UI
      mapping/PlannerStateMapper.kt         buildPlannerUiState — one slot, every saved city
      mapping/WidgetStateMapper.kt          buildWidgetUiState — the same data, capped at six rows
      navigation/Routes.kt                  Route constants
      components/CityCard.kt                CityCard + CityCardModel (shared by list and map)
      components/CircleIconButton.kt        Icon-only button; drawn at 44 dp, 48 dp touch target
      components/DaySlider.kt               Hand-drawn 24-hour planner slider + DaySliderTags
      components/TimeDifferenceLabel.kt     "Same time" / "+2 hrs 30 mins ahead" wording
      components/WorldClockBottomBar.kt     Clocks / Search / Map
      components/WorldClockSegmentedControl.kt  12h / 24h segmented control (48 dp options)
      components/WorldMapCanvas.kt          Map renderer + markers + WorldMapTestTags
      screens/ClocksScreen.kt               City list; gear action opens settings; planner entry
      screens/CityDetailScreen.kt           Detail, 12h/24h toggle, label action, remove, view on map
      screens/CityLabelDialog.kt            Modal label editor overlay
      screens/CitySearchSheet.kt            Search / add
      screens/MeetingPlannerScreen.kt       Slider, legend and per-city rows
      screens/SettingsSheet.kt              Settings sheet + SettingsContent (the five-theme picker)
      screens/WorldMapScreen.kt             Map screen
      widget/WorldClockWidgetContent.kt     The widget's own drawing (Glance, not Compose UI)
      widget/DayNightColorProvider.kt       Device light/dark colour resolution for Glance
      samples/PreviewSamples.kt             Fixed instants for previews and tests
      theme/Color.kt                        WorldClockPalette, the five palettes, WorldClockColors
      theme/Type.kt                         Inter FontFamily, Material3 type roles, WorldClockTextStyles
      theme/Shape.kt                        WorldClockShapes (base 16 dp)
      theme/Theme.kt                        paletteFor(theme), palette -> ColorScheme, MaterialTheme
      theme/SystemBarAppearance.kt          Keeps system bar icons legible against the palette

app/src/main/res/
  xml/world_clock_widget_info.xml           Widget provider info (4x4 cells, 30-minute backstop)
  values/colors.xml                         Light window background
  values/themes.xml                         Light launch theme
  values-night/colors.xml                   Dark window background
  values-night/themes.xml                   Dark launch theme
```

**REQUIREMENT:** `ui/mapping/WorldClockStateMapper.kt` is the single place where city data
(including map coordinates) reaches the UI. Screens never read the repository directly.

---

## 5. Functional requirements

### 5.1 City directory

**IMPLEMENTED** — 22 cities, bundled offline in `CityDirectory.kt`, each with an IANA
timezone id and map coordinates.

| # | City | id | IANA timezone |
| --- | --- | --- | --- |
| 1 | Tokyo | `tokyo` | `Asia/Tokyo` |
| 2 | Sydney | `sydney` | `Australia/Sydney` |
| 3 | Los Angeles | `los-angeles` | `America/Los_Angeles` |
| 4 | New York | `new-york` | `America/New_York` |
| 5 | London | `london` | `Europe/London` |
| 6 | Yangon | `yangon` | `Asia/Yangon` |
| 7 | Singapore | `singapore` | `Asia/Singapore` |
| 8 | Bangkok | `bangkok` | `Asia/Bangkok` |
| 9 | Mumbai | `mumbai` | `Asia/Kolkata` |
| 10 | Dubai | `dubai` | `Asia/Dubai` |
| 11 | Paris | `paris` | `Europe/Paris` |
| 12 | Berlin | `berlin` | `Europe/Berlin` |
| 13 | Cairo | `cairo` | `Africa/Cairo` |
| 14 | Johannesburg | `johannesburg` | `Africa/Johannesburg` |
| 15 | Reykjavik | `reykjavik` | `Atlantic/Reykjavik` |
| 16 | Toronto | `toronto` | `America/Toronto` |
| 17 | Vancouver | `vancouver` | `America/Vancouver` |
| 18 | Seoul | `seoul` | `Asia/Seoul` |
| 19 | Hong Kong | `hong-kong` | `Asia/Hong_Kong` |
| 20 | Auckland | `auckland` | `Pacific/Auckland` |
| 21 | Honolulu | `honolulu` | `Pacific/Honolulu` |
| 22 | Buenos Aires | `buenos-aires` | `America/Argentina/Buenos_Aires` |

**REQUIREMENT:** search matches city name, region, country label, or IANA timezone id,
case-insensitively. A blank query returns the whole directory.

### 5.2 Defaults on first launch

| Setting | Value |
| --- | --- |
| Saved cities, in order | Tokyo, Sydney, Los Angeles, New York, London |
| Active city | Los Angeles |
| Initial time format | **24-hour** |
| Initial theme | **System** (follow the device) |

**Decision record:** the 24-hour default is read from the reference mockups. 12-hour was
never the default.

### 5.3 Clocks screen

**IMPLEMENTED**

- Eyebrow `THE WORLD, IN SYNC`, title `World Time`.
- Section label `YOUR CITIES` with a live count (`5 CITIES · 00:05`).
- One `CityCard` per saved city, in saved order, each showing name, region, offset label
  (`UTC+9`), local time, weekday + date, a sun or moon icon, and — phase 2 stage 2 — the
  **time difference against the device's own clock** (see
  [section 5.9](#59-time-difference-indicator)).
- A city with a custom label shows it in brackets after the name (`Tokyo (Head Office)`); see
  [section 5.8](#58-city-label-dialog).
- The active city's card is inverted (black on light, light on dark — see
  [section 7.1](#71-colour)).
- `ADD CITY` action opens the search sheet.
- **`PLAN A MEETING`** action opens the meeting planner
  ([section 5.10](#510-meeting-planner)).
- **Gear action** in the header opens the settings sheet.
- Caption `AUTO-ADJUSTS FOR DAYLIGHT SAVING`.
- Empty state: `No cities yet` + `Add a city to track its local time.` + `Find a city`.
- Loading state: `Loading your cities…`.

Each card is laid out as **three independent lines** — `offset | weekday`, `name | time`,
`region | difference` — rather than one two-column grid. That detail matters: with a shared
grid, a long label or a long difference line would shrink the column that holds the city
name. Per line, only the item that does not fit is ellipsized, and the city name is the last
thing to give way.

### 5.4 City detail screen

**IMPLEMENTED**

- `YOUR LOCAL TIME` label and the device's own timezone.
- **Stacked hour-over-minute clock** with weekday and date to the right — matches the
  reference mockup.
- **`COMPARED WITH YOUR LOCAL TIME`** section — phase 2 stage 2 — showing this city's
  difference against the device clock
  ([section 5.9](#59-time-difference-indicator)).
- The city's custom label, when set, on its own line under the region (`(Head Office)`), with
  an `ADD LABEL` / `EDIT LABEL` action beneath it
  ([section 5.8](#58-city-label-dialog)).
- 12h / 24h segmented control, labelled `Time format`.
- `Remove` (destructive) and `View on map`.
- Back arrow.

### 5.5 Search sheet

**IMPLEMENTED**

- Title `Add a city`, placeholder `City, country or timezone`.
- Live filtering over the 22-city directory.
- Already-saved cities are shown as added/disabled rather than hidden.
- No-results state: `No cities found` + `Try a different city, country or timezone name.`
- Adding a city makes it the active city and returns to the Clocks screen.

### 5.6 Behaviour rules

| Rule | Requirement |
| --- | --- |
| Selecting a city | It becomes active **and** its detail screen opens |
| Removing the active city | Another saved city is promoted to active |
| Removing the last city | Empty state, no crash, no dangling active id |
| Restart | Saved order, active city, format, theme and city labels are restored |
| Unknown persisted id | Dropped silently, order preserved, active id repaired |
| Unknown label id | Dropped silently (see [section 5.8](#58-city-label-dialog)) |
| Day/night icon | Sun for 06:00–17:59, moon for 18:00–05:59, **local** hour |
| Time difference reference | Always the **device's own** zone, never the active city |
| Meeting planner slot | A time of day in the **device's own** zone; one slot drives every row, and every row is read at that single instant |

### 5.7 Settings sheet

**IMPLEMENTED** — `screens/SettingsSheet.kt`.

- Opened by the gear action on the Clocks screen; dismissed by the scrim, the drag handle or
  system back.
- Eyebrow `HOW IT LOOKS`, title `Settings`, divider, section label `APPEARANCE`.
- Label `Theme`, then the five themes as full-width rows: a small preview swatch, the theme's
  name, and an inverted pill on the one in force. Each row is at least 48 dp tall and is a radio
  button in one `selectableGroup()`, so a screen reader announces e.g. `Warm Sepia, radio button,
  selected`. The swatch is decorative — the name is the label — and carries no semantics.
- Hint `Applies to every screen straight away, and is remembered. The home-screen widget follows
  your device's light or dark setting.` The second sentence is deliberate: the widget really does
  ignore the app's theme ([section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme)),
  and it is better to say so on the surface that does the choosing than to look broken.
- Changing the theme writes it to DataStore and repaints **without recreating the activity**, so
  the sheet stays open while the theme changes under it — including its own swatches.

**Decision record — why rows, not a segmented control (2026-10-18):** the three-mode control
could not carry the names. `Light Monochrome` is 16 characters, and the segmented control's
options are text plus 28 dp of padding inside a 360 dp sheet, so five of them would either
truncate or shrink below the 48 dp target the phase-2 pass had just established. A vertical list
also has room for the preview swatch, which is what turns "five names" into "five looks". The
segmented control itself is unchanged and still used for the 12h/24h choice on the detail screen.

**Decision record:** the theme control was deliberately **not** placed on the city detail
screen (a per-city surface is the wrong home for an app-wide preference) and **not** given a
fourth bottom-bar tab (that would depart from the reference design's three destinations). The
user chose the gear-action/sheet option on 2026-10-17.

### 5.8 City label dialog

**IMPLEMENTED** — `screens/CityLabelDialog.kt`.

A per-city personal label / note (the requirement's `Tokyo (Head Office)`), edited from the
city detail screen.

- Opened by `ADD LABEL` (no label yet) or `EDIT LABEL` (label set) on the detail screen.
- Title `City label`, support text `Shown after the city name, for example Tokyo (Head
  Office).`, a single-line field (placeholder `Head Office`), `Cancel` and `Save`.
- `Remove label` appears only when a label is already set; it clears it.
- Save trims; an empty or all-whitespace entry **clears** the label rather than storing blanks.
- Maximum length 40 characters (`CITY_LABEL_MAX_LENGTH`); the field refuses anything longer.
- Dismissed by `Cancel`, the system back gesture, or a tap outside the card.
- Dismissing without saving leaves the stored label untouched.

**Where the label is shown:** in brackets after the city name — the Clocks card (ellipsized if
the user's label is long, never the city name), the map strip pill (uppercased with the rest of
the pill), the map selected-city panel, and the detail screen, where it also gets its own line.

**Decision record — not a platform dialog.** The dialog is a modal overlay inside the app's own
composition, not an `androidx.compose.ui.window.Dialog`. Two reasons: a platform dialog brings
its own container shape, elevation and button styling, which is exactly the Material default look
this project must not have; and a dialog window cannot be rendered or driven by the JVM
screenshot tests — a text field inside a dialog window never reaches an idle state under
Robolectric, so the dialog would have been the only untested surface in the app. The overlay is
drawn from the same palette as every other surface and is covered by both screenshot and
behaviour tests.

### 5.9 Time difference indicator

**IMPLEMENTED** — `domain/time/TimeDifference.kt`, `ui/components/TimeDifferenceLabel.kt`.

Compares a city with the **device's own** zone at the same instant and shows the gap.

| Case | Text |
| --- | --- |
| No gap | `Same time` |
| Whole hours ahead | `+2 hrs ahead` |
| Whole hours behind | `-5 hrs behind` |
| Minutes involved | `+2 hrs 30 mins ahead`, `-30 mins behind` |

- The maths is `ZoneId.rules.getOffset(instant)` for both zones, subtracted. Nothing is
  hard-coded, so half-hour zones (`UTC+5:30`, `UTC+6:30`), quarter-hour zones (`UTC+5:45`) and
  both directions of daylight saving are all exact (acceptance criteria 13a).
- Shown on every city card (bottom right), in the map's selected-city panel, and on the detail
  screen under `COMPARED WITH YOUR LOCAL TIME`.
- Card text is spoken in full: `Compared with your local time: +2 hrs 30 mins ahead`, because
  `+2 hrs ahead` alone does not say *ahead of what*.

**Decision record — the reference is the device, not the active city.** Comparing with the
active city was tried first and rejected: tapping a city makes it active, so the detail screen
would have read `Same time` for the city you are looking at. The device's own clock is the only
reference that is stable across all three surfaces. The user chose it on 2026-10-18.

---

### 5.10 Meeting planner

**IMPLEMENTED** — `ui/screens/MeetingPlannerScreen.kt`, `ui/components/DaySlider.kt`,
`domain/time/MeetingTime.kt`, `ui/mapping/PlannerStateMapper.kt`.

A 24-hour time-zone converter for arranging a call across the saved cities: choose a time of
day where you are, and every saved city's reading for that instant appears at once.

- Reached from `PLAN A MEETING`, a bordered action directly under the `YOUR CITIES` header on
  the Clocks screen. It is styled like `ADD CITY` so the two read as a pair, and it sits at the
  top because at 360 dp a full city list pushes anything at the foot of the screen out of view.
- `YOUR LOCAL TIME` reads the chosen slot: the time at 48 sp, then the date and the device's
  offset at that slot (`Sat, 20 Jan · UTC+6:30`).
- The **day slider** is a hand-drawn 24-hour control: a track with a tick per hour, labelled
  `00 06 12 18 24`, a draggable marker, and the working day (09:00–18:00 in the reference zone)
  shaded as a pill inside the track. Tap anywhere to jump there, or drag to sweep.
- The slot moves in **15-minute steps** (`MinuteOfDay.STEP_MINUTES`), from 00:00 to 23:45. The
  last slot is deliberately 23:45 rather than midnight: 24:00 is the next day, and a slider that
  can reach it would silently change the date under the user.
- One row per saved city, in saved order, each showing the name (with the user's label), the
  offset from the reference measured **at the chosen slot**, the city's own weekday, and its
  local time. The weekday is there because a city across the date line is on a different day and
  a bare `18:30` would hide that.
- A city whose local time falls inside 09:00–18:00 is marked with a filled marker and a
  full-contrast time; a city outside it gets a hollow marker and a muted time. The rule is
  stated under the slider (`WITHIN 09:00–18:00` / `OUTSIDE`) rather than applied silently.
- Opens on the current time, so the planner agrees with the Clocks screen behind it, and stays
  on the clock until the slider is touched; from the first touch the slot is the user's. The
  slot survives a rotation (`rememberSaveable`) and is deliberately **not** persisted — it is a
  scratch calculation, not a setting.
- With no cities saved it says `No cities to plan for` and offers `Find a city`; the slider is
  still there and still works.

**Decision record — its own screen, not a fourth tab.** The reference design's bottom bar has
three destinations, and a fourth was rejected when the theme control was placed
([section 5.7](#57-settings-sheet)). The planner also needs the height: a day ruler plus a row
per city does not fit in a sheet.

**Decision record — the slot is a time of day in the device's own zone.** The planner answers
"what time will it be for everyone if we meet at 09:00 where I am", which is the same reference
the city cards compare against ([section 5.9](#59-time-difference-indicator)). Reading the slot
in the active city's zone instead would make the answer change when a different card is tapped.

**Decision record — the working day is 09:00–18:00, start inclusive, end exclusive.** A
judgement, not a fact, so it lives as one named rule (`isWithinWorkingDay`) and is shown to the
user as a legend. Nothing is hidden by it.

**Decision record — the slider is hand-drawn, not Material's.** Material's `Slider` brings its
own track geometry, thumb size and tick marks — the default Material look this project does not
use — and the shaded working-day band has to line up with the marker exactly. The control keeps
the accessibility contract Material's slider implements: `progressBarRangeInfo` with a
`setProgress` action and a `stateDescription` that reads the slot as a time, so TalkBack can
adjust it without touching the screen and never announces a bare percentage.

### 5.11 Home-screen widget

**IMPLEMENTED** — `widget/WorldClockWidget.kt`, `widget/WorldClockWidgetReceiver.kt`,
`widget/WidgetTicker.kt`, `widget/WidgetMirror.kt`, `ui/widget/WorldClockWidgetContent.kt`,
`ui/mapping/WidgetStateMapper.kt`, `res/xml/world_clock_widget_info.xml`.

The widget is a second view of the same data, never a second opinion: its rows are built by
`buildWidgetUiState`, which delegates to `buildWorldClockUiState` and then caps the list.
Formats, labels, day/night icons and differences are therefore identical to the screens.

| Aspect | Decision |
| --- | --- |
| Toolkit | Jetpack Compose Glance 1.1.1 (`GlanceAppWidget` + `GlanceAppWidgetReceiver`); the card is drawn by `WorldClockWidgetContent` |
| Contents | Header `WORLD TIME` with the device's own time (muted, right), a hairline, then one row per city: sun/moon icon, name (labels included), weekday **only when the city is on a different date** from the device, and the time in the app-wide format; then `+N more` when the list is longer than the row budget |
| Row budget | `WIDGET_MAX_ROWS = 6`, a fixed number rather than a measured size (`LocalSize` is unreliable below API 31); overflow is counted, never dropped |
| Default footprint | 4×4 cells — `minWidth`/`minHeight` 250 dp with `targetCellWidth`/`targetCellHeight` 4, resizable both ways |
| Theme | The **device's** light/dark setting, not the app's stored choice: a widget sits on a home screen, not inside the app. `DayNightColorProvider` resolves each token against the device configuration, because Glance 1.1 has no literal day/night `ColorProvider` |
| Tap | The whole card opens `MainActivity` |
| Refresh | An `AlarmManager.setAndAllowWhileIdle` alarm on every minute boundary, self-rearming while a widget exists, plus `updatePeriodMillis="1800000"` as a backstop |
| Data changes | `WidgetMirror` watches only `savedCityIds`, `cityLabels` and `use24Hour` and redraws on change; a failed redraw is logged and does not kill the mirror |

**Decision record — a minute alarm, not WorkManager.** WorkManager's minimum periodic interval
is 15 minutes, which is not a clock. `setAndAllowWhileIdle` is inexact by design: it fires on the
minute while the device is awake, and Android throttles it to roughly once every 9 minutes in
deep doze. That is the honest trade-off for a widget that shows minutes without holding a wake
lock; the 30-minute `updatePeriodMillis` is the backstop if an alarm is lost (for example
across a reboot). `setExactAndAllowWhileIdle` was rejected because it needs the
`SCHEDULE_EXACT_ALARM` permission for no visible gain.

**Decision record — the ticker runs only while a widget exists.** `scheduleWidgetTick` checks
`hasHomeScreenWidgets` first, and the alarm is cancelled when the last widget is removed, so a
user who never adds one is never woken by it. `WorldClockApp.startWidgetMirror()` is idempotent
and starts the mirror from the same check.

**Decision record — the widget follows the device theme.** Following the app's stored theme was
rejected: a home screen is shared with other apps, and a widget that ignores the device's
setting looks broken next to them. The consequence is documented in
[section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

**Re-opened by the five themes (2026-10-18).** The rejection above assumed the app had two
palettes whose light/dark split matched the device's, which made "device" and "app" agree most of
the time. With Warm Sepia, Cool Slate and High Contrast that coincidence is gone, so this
decision is now recorded as **open** with three options and no answer yet — see
[section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme). The behaviour is
unchanged until that is decided, and the Settings hint now says so on screen.

### 5.12 Phase 2 integration, accessibility and polish

**IMPLEMENTED** — the pass that connects the phase 2 features to each other and finishes their
accessibility, with no change to what any of them does.

| Item | Decision |
| --- | --- |
| Reachability | Every phase 2 feature is reachable from the Clocks screen — the theme switch behind the gear, the planner from `PLAN A MEETING`, labels from a city's detail screen, and the widget from the launcher. Nothing is hidden behind a long-press or a gesture |
| Touch targets | Every control added in phase 2 is at least 48 dp tall. `CircleIconButton` keeps its drawn circle (44 dp at the small call sites, 48 dp by default) and puts the click, the label and the semantics on a box around it that is never smaller than 48 dp (`MinTouchTarget`); the segmented control's options, the five theme rows and the bordered/text action rows (`PLAN A MEETING`, `ADD CITY`, `Find a city`, `EDIT LABEL`) are clamped with `heightIn(min = MinTouchTarget)` |
| Headings | Screen titles and section headers carry the `heading()` semantics — `World Time` / `YOUR CITIES`, `Meeting planner` / `YOUR LOCAL TIME` / `YOUR CITIES AT THIS TIME`, `Settings` / `APPEARANCE`, `City label` and the detail screen's two blocks — so TalkBack can jump between sections instead of swiping through them |
| The label dialog | Announced as a pane (`paneTitle`), because it is drawn inside the app's own composition rather than in a platform dialog window; its field carries `a11y_city_label_field` (`Label for %1$s`) so the edit box names the city it edits |
| The difference on a card | Already announced as `Compared with your local time: %1$s` in phase 2 stage 2; asserted for the first time in criterion [17c](#11-acceptance-criteria) |

**What this pass deliberately did not change:** no colour, type, spacing or wording changes; the
only visual differences are the taller segmented control and the 48 dp slots around the small
circular buttons, both visible in the re-recorded screenshots.

## 6. World map specification

### 6.1 Rendering

**IMPLEMENTED** — `WorldMapCanvas.kt`, drawn entirely with Compose `Canvas`.

Draw order: ocean → graticule → landmasses → night shading → active meridian → UTC ruler,
with city markers as real composables on top.

| Layer | Detail |
| --- | --- |
| Ocean | `WorldClockColors.Card` fill |
| Graticule | 1 px `WorldClockColors.Border` lines every **30°** |
| Landmasses | `WorldClockColors.MapLandmass` fills from 26 hand-authored outlines. It is `accent` in the four soft themes and white in High Contrast, where the ocean is pure black and the wash below cannot be seen on it |
| Night shading | `WorldClockColors.MapNight` — the ink colour at **18 %** in the three light themes, black at **45 %** in True Dark and **40 %** in High Contrast (a fixed alpha would be invisible on one map and mud on the other) |
| Active meridian | `WorldClockColors.MapAccent` (`#BF6B6B`), 1.5 px vertical line at the active city's longitude |
| UTC ruler | Hour ticks from −12 to +14, label every 2 h, measured with `rememberTextMeasurer`/`drawText`; labels that would collide with the border are skipped |

**REQUIREMENT:** no map tiles, no map provider, no API key, no image assets.

### 6.2 Projection

**IMPLEMENTED** — equirectangular.

```text
x = (longitude + 180) / 360
y = (90 - latitude) / 180
```

Chosen so that meridians and parallels are straight lines and the terminator is cheap to
evaluate per longitude.

### 6.3 Day/night terminator

**IMPLEMENTED** — analytic, per longitude, from the standard first-order NOAA solar
approximation (`SolarPosition.subsolarPoint`), then:

```text
φ = atan( -cos(Δλ) · cos(δ) / sin(δ) )
```

Two deliberate implementation notes, both recorded because they were bugs first:

1. **`atan`, not `atan2`.** `atan2` selects the antipodal branch and returns latitudes
   outside ±90°.
2. **Declination is clamped to a minimum of 0.5°.** At the equinoxes the Sun sits over the
   equator and the equation degenerates; the clamp keeps the curve continuous and
   unambiguous all year round.

Greenwich mean sidereal time is computed in **hours per day** and must be **multiplied by
15** to become degrees before use. (This was also a bug first.)

Traced at 180 steps (one per 2° of longitude), then closed through whichever pole is in
shadow.

### 6.4 Markers

**IMPLEMENTED**

- Markers are **composables, not canvas shapes**, so each gets a **44 dp** touch target, a
  ripple, and a screen-reader label.
- Saved city: hollow circle with a solid centre dot.
- Active city: `#BF6B6B` diamond rotated 45°, outlined in the card colour.
- Label: `"<City>, <time>, <offset>"`, plus `", selected city"` when active.
- Tapping a marker makes that city active.
- Test tags live in `internal object WorldMapTestTags` in `WorldMapCanvas.kt`.

### 6.5 Map screen layout

**IMPLEMENTED** — `WorldMapScreen.kt`, vertically centred with `Arrangement.Center`:

1. City strip (pills, auto-scrolls to the active city) with `+` at the top right
2. Selected-city clock panel (tappable → detail screen)
3. Map canvas at `aspectRatio(1.7f)`
4. Legend: `SELECTED` / `SAVED CITY` / `IN NIGHT`
5. Caption: `TAP A MARKER TO SELECT · SHADED AREA IS NIGHT`

Empty state replaces the strip with a titled header plus the `+` button, and shows
`No cities on the map` + `Add a city to see where it sits on the globe.` + `Find a city`.

---

## 7. Design system

### 7.1 Colour

**IMPLEMENTED** — `WorldClockPalette` (`theme/Color.kt`). The app ships exactly five themes and
these tokens are the *only* colours the app uses. Nothing falls back to a Material default, so
the Material 3 purple/blue accent cannot appear.

| Token | Light Monochrome | True Dark | Warm Sepia | Cool Slate | High Contrast | Use |
| --- | --- | --- | --- | --- | --- | --- |
| `background` | `#F3F3F1` | `#111111` | `#F7F4EF` | `#EEF2F5` | `#000000` | App main surface, sheet surface |
| `foreground` | `#111111` | `#E1E1DE` | `#2C2521` | `#1E293B` | `#FFFFFF` | Main text, primary actions |
| `card` | `#FAFAF8` | `#1A1A1A` | `#FCFAF6` | `#F8FAFC` | `#000000` | City cards, elevated surfaces, map water |
| `primary` | `#111111` | `#E1E1DE` | `#2C2521` | `#1E293B` | `#FFFFFF` | Selected card, active controls |
| `primaryForeground` | `#FFFFFF` | `#111111` | `#FFFFFF` | `#FFFFFF` | `#000000` | Text and icons on `primary` |
| `onPrimaryMuted` | `#9EFFFFFF` | `#9E111111` | `#9EFFFFFF` | `#9EFFFFFF` | `#9E000000` | Supporting text on the selected card |
| `secondary` | `#E8E8E6` | `#232323` | `#EFEAE2` | `#E4EAEF` | `#1A1A1A` | Secondary / raised surfaces |
| `mutedForeground` | `#777774` | `#777774` | `#7A7065` | `#64748B` | `#C4C4C4` | Supporting text, offsets, labels |
| `accent` | `#E3E3E0` | `#262626` | `#EDE7DD` | `#DDE5EB` | `#1F1F1F` | Soft selected surfaces, theme-picker container |
| `border` | `#D9D9D6` | `#2E2E2E` | `#E2DACE` | `#CFD9E1` | `#FFFFFF` | Outlines, separators, graticule |
| `mapAccent` | `#BF6B6B` | `#BF6B6B` | `#BF6B6B` | `#BF6B6B` | `#BF6B6B` | Map meridian, active pin, destructive action |
| `mapLandmass` | `#E3E3E0` | `#262626` | `#EDE7DD` | `#DDE5EB` | `#FFFFFF` | Continents on the schematic map |
| `mapNight` | `#111111` @ 18 % | `#000000` @ 45 % | `#2C2521` @ 18 % | `#1E293B` @ 18 % | `#000000` @ 40 % | Wash over the half of the globe in shadow |
| `isDark` | `false` | `true` | `false` | `false` | `true` | Drives `darkColorScheme()` and the system bar icons |

**Theme ids.** `AppTheme` (`data/prefs/AppTheme.kt`) numbers the five in the order above:
Light Monochrome `0`, True Dark `1`, Warm Sepia `2`, Cool Slate `3`, High Contrast `4`. The id
is persisted, so the order is part of the stored format — tests pin both the ids and the
mapping.

**The themes are complete palettes, not a light/dark switch.** There is no "follow the device"
option: the user has chosen an exact look, and repainting it because the phone switched at
sunset would throw that choice away. This replaces the phase-2 `System / Light / Dark` mode
(user decision, 2026-10-18) and is asserted — the same theme paints the same palette on a
device in dark mode as on one in light mode. The home-screen widget is the one surface that
still follows the *device* (§5.11).

**The agreed tokens are user-specified:** Light Monochrome's `#F3F3F1` / `#111111` and True
Dark's `#111111` / `#1A1A1A` / `#E1E1DE` / `#777774` are unchanged from phase 2, Warm Sepia's
`#F7F4EF` / `#2C2521` and Cool Slate's `#EEF2F5` / `#1E293B` and High Contrast's `#000000` /
`#FFFFFF` are the values the five-theme design names (2026-10-18). Every other value is derived
so that all five keep the *same relationships*:

- `primary` is the **inverse of the base surface** in all five. The light themes select with a
  black/ink card holding light text; the dark themes select with a light card holding dark text.
- `card` is one step lighter than `background` wherever the surface is light, and the two light
  themes that are not monochrome step `secondary`, `accent` and `border` *down* from the
  background by the same kind of increments.
- `mapAccent` is identical in all five — the one warm accent works against every background and
  is the app's only non-monochrome element.
- `mapLandmass` is `accent` in the four soft themes (so their maps are unchanged, byte for byte)
  and **white in High Contrast**. A black night wash cannot shade a pure-black ocean, so High
  Contrast draws its continents light and lets them carry the terminator; a test composites the
  wash over the landmasses and asserts it is still darker than they are.
- `mapNight` **carries its own alpha** and is not derived from `foreground`. Deriving it would
  wash a dark map *lighter*, which reads as daylight. In the light themes it is the ink colour
  at 18 %, so the light monochrome value stays byte-identical to phase 1.

**High Contrast is deliberately special.** Its `card` is the same pure black as its background,
so cards are separated by their `border` outlines rather than by a fill, and the legacy "cards
are lighter than the background" rule does not apply to it — the palette test instead requires
every dark theme to be separated by *either* a lifted card or a readable outline, and documents
High Contrast as the outline case.

**How the active palette reaches the UI.** `paletteFor(theme)` is the single place that maps a
theme to a palette (pure, so the five-way mapping is unit-testable without a device).
`WorldClockTheme(theme)` provides the result through `LocalWorldClockPalette`. `WorldClockColors`
exposes it with Material-style composable getters (`WorldClockColors.Card`), mirroring
`MaterialTheme.colorScheme`, so a screen written once paints correctly in every theme and no
call site needed to change. Because they read a composition local they are only readable from
a composable scope — non-composable drawing code (the map's `DrawScope` steps) receives a
`WorldClockPalette` parameter instead.

**Contrast (asserted by `WorldClockPaletteTest`, over all five themes):** main text ≥ 4.5:1 on
both background and card, text on the selected card ≥ 4.5:1, supporting text ≥ 3:1 on the
background, the card and the soft pills (it is used only for short supporting labels, so the
large-text / non-text threshold is the right bar), and the translucent supporting text on the
selected card is composited before it is measured.

**System bars.** `SystemBarAppearance` sets the status- and navigation-bar icon appearance
from the *active palette* rather than the device setting. `enableEdgeToEdge()` decides from the
device configuration, which was only right while the mode was `System`; True Dark or High
Contrast chosen on a device in light mode would otherwise leave dark icons on a near-black
background.

**REQUIREMENT:** no purple, no blue accent, no gradients.

### 7.2 Typography

**IMPLEMENTED** — Inter Regular / Medium / SemiBold / Bold mapped onto the Material 3 type
roles, plus a `WorldClockTextStyles` object for app-specific styles such as
`MapRulerLabel`.

| App style | Size / weight | Used by |
| --- | --- | --- |
| `Eyebrow` | 11 sp Medium, 1.6 tracking | Small-caps label above every screen title |
| `ScreenTitle` | 32 sp Bold, −0.6 tracking | `World Time`, `Meeting planner` |
| `SectionLabel` | 11 sp SemiBold, 1.4 tracking | `YOUR CITIES`, `PLAN A MEETING` |
| `DstCaption` | 10 sp Medium, 1.2 tracking | `AUTO-ADJUSTS FOR DAYLIGHT SAVING`, planner legend |
| `CityDifference` | 10 sp Medium | Time difference on a city card |
| `DetailDifference` | 18 sp SemiBold | Time difference on the detail screen |
| `DetailCityLabel` | 15 sp Medium | The user's label, in brackets |
| `MapRulerLabel` | 9 sp Medium, tabular | Longitude ruler on the map |
| `PlannerClock` | 48 sp Bold, tabular | The chosen slot at the top of the meeting planner |
| `PlannerRowName` | 17 sp SemiBold | City name in a planner row |
| `PlannerRowTime` | 20 sp Bold, tabular | A city's time in a planner row |
| `PlannerRowDifference` | 11 sp Medium | Offset under a planner row's name |
| `PlannerAxisLabel` | 9 sp Medium, tabular | Hour labels under the day slider |

Tabular figures matter wherever a clock is drawn or a column of times is aligned: proportional
digits would make `11:30` and `18:30` different widths and the times would jitter as the
minutes change.

### 7.3 Shape and spacing

| Token | Value |
| --- | --- |
| Base corner radius | 16 dp (`WorldClockShapes.medium`) |
| City card height | ≈ 93 dp |
| Horizontal page padding | ≈ 24 dp |
| Map aspect ratio | 1.7 |
| Minimum touch target | 48 dp (`MinTouchTarget`) |

---

## 8. Data model and persistence

### 8.1 Proto schema

**IMPLEMENTED** — `app/src/main/proto/saved_cities.proto`, compiled with `protobuf-lite`.

```proto
// Superseded: the light/dark choice of the three-mode build. Read-only, for
// migration; nothing writes it any more and the next theme change clears it.
enum SavedThemeMode {
  THEME_MODE_SYSTEM = 0;   // proto3 zero value; also what an older store reads as
  THEME_MODE_LIGHT  = 1;
  THEME_MODE_DARK   = 2;
}

// The five themes, numbered as AppTheme ids 0-4.
enum SavedAppTheme {
  SAVED_APP_THEME_LIGHT_MONOCHROME = 0;
  SAVED_APP_THEME_TRUE_DARK        = 1;
  SAVED_APP_THEME_WARM_SEPIA       = 2;
  SAVED_APP_THEME_COOL_SLATE       = 3;
  SAVED_APP_THEME_HIGH_CONTRAST    = 4;
}

message SavedCities {
  repeated string city_ids = 1;      // ordered; order is significant
  string active_city_id = 2;         // empty when nothing is selected
  bool use_24_hour = 3;              // true = 24-hour; first launch seeds true
  bool seeded = 4;                   // false until defaults are seeded
  SavedThemeMode theme_mode = 5;     // legacy; read for migration only
  map<string, string> city_labels = 6;  // label per city id; absent = no labels
  optional SavedAppTheme theme = 7;  // the chosen theme; absent = nothing stored
}
```

**Decision record — field 4:** named `seeded`, not `initialized`, to avoid any clash with
protobuf's generated `isInitialized()`. It exists so that an empty city list can mean *"the
user removed every city"* rather than *"new install"*.

**Decision record — field 5 (legacy):** the enum is `SavedThemeMode`, **not** `ThemeMode`, so the
generated type cannot collide with the domain enum of the same concept. `THEME_MODE_SYSTEM` is
`0`, so a store written before theme support existed (field absent) reads as `SYSTEM`. The five
themes replaced this field (field 7); it is kept so that those stores still read, and
`setTheme` clears it in the same write, so a store carries one answer rather than two that can
disagree. `toMigratedAppTheme()` maps `DARK → True Dark` and `LIGHT → Light Monochrome` — what
those users were actually looking at — and `SYSTEM → Light Monochrome`, because "follow the
device" cannot be resolved after the fact and guessing a theme the user never chose would be
worse than the default.

**Decision record — field 7:** `theme` is `optional` (**`optional`**, and this is the point)
rather than a plain enum field. Light Monochrome is the proto3 zero value, so without the
presence bit "nothing stored" and "Light Monochrome chosen" would be the same bytes — and every
user who had picked dark under the old three-mode switch would silently be handed the light
theme on upgrade. `UserPreferences.toUserPreferences()` therefore reads
`hasTheme() ? theme.toAppTheme() : themeMode.toMigratedAppTheme()`.
`seededWithDefaults()` uses `toBuilder()` and therefore never overwrites a stored theme choice.

**Migration is one-way and needs no rewrite.** A migrated store still carries the legacy field
until the user next changes the theme; the read path is the only thing that has to know about
it, and every subsequent write converges the store on field 7.

**Decision record — field 6:** `city_labels` is a map keyed by city id, not a repeated message,
because a city has at most one label and the id is the only stable key (a label cannot identify
its city). An absent map reads as "no labels", so no migration is needed. Unknown keys are
pruned on read, the same rule as `city_ids`; a label for a city that is merely **removed from
the list** but still exists in the directory is **kept**, so re-adding that city restores its
label instead of silently losing it.

### 8.2 Repository behaviour

**IMPLEMENTED** — `ClockRepository`

- `addCity`, `removeCity`, `selectCity`, `setUse24Hour`, `setTheme`, `setCityLabel`.
- `setTheme(theme)` writes field 7 and clears the legacy field 5.
- `setCityLabel(cityId, label)` trims the label; `null`, an empty string or whitespace clears
  it. It only ever touches the label map.
- Prunes ids that are no longer in the directory.
- Repairs a dangling active id by promoting a surviving city.
- Emits `PrefsError` on read/write failure; `WorldClockRoot` surfaces it in a snackbar
  (`Couldn't read your saved cities. Showing defaults.` / `Couldn't save your changes.`).

---

## 9. Accessibility requirements

**IMPLEMENTED**

| Requirement | Status |
| --- | --- |
| Icon-only controls have `contentDescription` | Yes — add, back, check, close, delete, expand, globe, moon, settings, search, sun |
| Emoji never used as a UI icon | Yes — vector drawables only |
| Selected state exposed | Yes — `Role.RadioButton` + `Selected` semantics on the 12h/24h control, the five theme rows, and map pills and markers |
| A group of choices announces itself as a group | The theme picker's rows sit in `selectableGroup()`, so a screen reader says "Warm Sepia, radio button, selected" and the five are read as one set of options |
| Disabled/already-added state exposed | Yes — search results announce `%1$s is already added` |
| Press feedback | Ripple on every interactive surface, including the gear action |
| Back behaviour | Detail screen back arrow **and** system back return to the previous destination; system back dismisses the search and settings sheets |
| Screen-reader descriptions | `a11y_world_map`, `a11y_utc_ruler`, `a11y_map_marker`, `a11y_map_marker_selected`, `a11y_select_city_on_map`, `a11y_settings`, `a11y_difference_from_your_time`, `a11y_edit_city_label`, `a11y_meeting_time_slider`, `a11y_planner_row` |
| Colour is never the only signal | Selected cards also change text colour and elevation; every theme in the picker is named in text and its row reports itself as a selected radio button, so the preview swatch is decoration rather than the signal; the planner's working day is a shaped marker plus a stated legend, not a tint alone |
| Meaning carried by a value alone is spoken in context | A card's difference is announced as `Compared with your local time: %1$s`, not as the bare `+2 hrs ahead` |
| A control whose value is not a plain percentage says so | The day slider's `stateDescription` reads the slot (`09:00`), because TalkBack's default for a progress bar — "38 percent" — would be useless for a meeting time |
| A row of values is one swipe, not four | Each planner row merges its children and announces `Meeting time in %1$s: %2$s, %3$s`, including the city's weekday and whether it is within working hours |
| A control is never smaller than a finger | Every interactive control is at least 48 dp tall. The 44 dp circular buttons draw at 44 dp but carry the click on a 48 dp box, and the segmented control's options are padded out to 48 dp, so the drawn size and the hit size are separate numbers |
| Screen titles and section headers are headings | `heading()` on `World Time` / `YOUR CITIES`, `Meeting planner` / `YOUR LOCAL TIME` / `YOUR CITIES AT THIS TIME`, `Settings` / `APPEARANCE`, `City label`, and the detail screen's `COMPARED WITH YOUR LOCAL TIME` / `YOUR LOCAL TIME`, so TalkBack's heading navigation works |
| A dialog drawn in the app's own composition announces itself | `CityLabelDialog` sets `paneTitle`, because it is an in-composition overlay rather than a platform dialog window; its field is named `Label for %1$s` instead of falling back to the placeholder |
| The widget's icons are labelled | The sun/moon in each widget row carries `a11y_daytime` / `a11y_nighttime`; the card itself is one click action that opens the app. `RemoteViews` cannot merge semantics, so a row is announced in reading order — which is why the weekday is shown only when it differs from the device's date |

---

## 10. (reserved)

Reserved so that section numbers in this document line up with the original specification's
section numbering. No content.

---

## 11. Acceptance criteria

**REQUIREMENT** — confirmed by the user on 2026-10-17 (see
[section 0.1](#01-section-11-provenance--resolved)).

**IMPLEMENTED** — every criterion below has an executable test in
`app/src/test/java/com/worldclock/app/ui/AcceptanceCriteriaTest.kt`.

| # | Criterion | Test |
| --- | --- | --- |
| 1 | First launch shows Tokyo, Sydney, Los Angeles, New York, London in that order with Los Angeles active | `criterion01_firstLaunchShowsTheDefaultCitiesInOrderWithLosAngelesActive` |
| 2 | Every city resolves to its correct IANA timezone and offset | `criterion02_everyCityMatchesItsIanaTimezone` |
| 3 | Daylight saving is applied automatically and correctly | `criterion03_daylightSavingIsAppliedAutomatically` |
| 4 | Sun icon for local 06:00–17:59, moon icon for 18:00–05:59 | `criterion04_dayAndNightFollowTheSixToEighteenRule` |
| 5 | Search covers the 22-city directory and refuses duplicates | `criterion05_searchCoversTheDirectoryAndRejectsDuplicates` |
| 6 | Selecting a city makes it active and opens its detail screen | `criterion06_selectingACityMakesItActiveAndOpensItsDetail` |
| 7 | Removing cities promotes a new active city; emptying shows the empty state | `criterion07_removingCitiesHandlesTheActiveCityAndTheEmptyState` |
| 8 | The 12h/24h switch changes the whole app and persists | `criterion08_theTimeFormatSwitchesAndPersists` |
| 9 | Saved state survives restart; unknown ids are dropped and the active id repaired | `criterion09_savedStateSurvivesRestartAndUnknownIdsAreDropped`<br>`criterion09b_unknownIdsAreDroppedAndTheActiveCityIsRepaired` |
| 10 | The map plots cities, shades night, and drives selection | `criterion10_theMapPlotsCitiesAndDrivesSelection` |
| 11 | Layout holds at 360 dp and every control is labelled; empty and error states are reachable | `criterion11_layoutHoldsAt360dpAndControlsAreLabelled`<br>`criterion11b_iconOnlyControlsCarryContentDescriptions`<br>`criterion11c_theEmptyAndErrorStatesAreReachable` |

**Phase 2 — theme (extended to five themes)**

| # | Criterion | Test |
| --- | --- | --- |
| 12a | The chosen theme is persisted and survives a restart, without disturbing the saved cities; a store written by the three-mode build is migrated (dark → True Dark, light → Light Monochrome, system → the default) rather than reset | `criterion12a_theChosenThemeIsPersistedAndSurvivesARestart`<br>`criterion12a_anOlderStoreIsMigratedToATheme` |
| 12b | Each of the five themes provides its own palette, a change of theme repaints without recreating anything, and the device's own light/dark setting no longer decides anything | `criterion12b_theChosenThemePaintsTheScreenWithTheMatchingPalette`<br>`criterion12b_theDeviceSettingNoLongerPicksThePalette` |
| 12c | The appearance control is reachable, offers all five themes by name, reports which is in force as a radio selection, hands the new choice back to the caller, and leaves every control labelled | `criterion12c_theSettingsSheetOffersEveryThemeAndReportsTheSelection` |

**Phase 2 — time difference and city labels**

| # | Criterion | Test |
| --- | --- | --- |
| 13a | The difference is the gap between the two zones' offsets at the same instant, so half-hour and quarter-hour zones are exact and daylight saving is picked up automatically | `criterion13a_theDifferenceIsTheOffsetGapAtThatInstant` |
| 13b | The indicator reads `Same time`, `+N hrs ahead` or `-N hrs behind`, keeping the minutes a half-hour zone needs | `criterion13b_theDifferenceReadsAheadBehindOrSameTime` |
| 13c | Every city card carries the difference, measured from the device's own time | `criterion13c_everyCityCardShowsTheDifferenceFromTheDeviceTime` |
| 13d | The detail screen shows the difference and names the reference it is measured against | `criterion13d_theDetailScreenNamesTheReferenceItComparesWith` |
| 14a | A city label is stored against the city id, survives a restart, can be changed and cleared, and disturbs nothing else | `criterion14a_aCityLabelIsPersistedAndCleared` |
| 14b | The label reaches the screens as the name followed by the label, leaving unlabelled cities alone | `criterion14b_theLabelIsShownAfterTheCityName` |
| 14c | The whole edit loop works through the real app shell: dialog opens, saves, shows, and removes the label | `criterion14c_theEditDialogSavesAndRemovesALabel` |

**Phase 2 — meeting planner**

| # | Criterion | Test |
| --- | --- | --- |
| 15a | The Clocks screen offers the meeting planner, and opening it puts a 24-hour slider on screen set to the current time | `criterion15a_theClocksScreenOffersAFullDayMeetingPlanner` |
| 15b | The slot is the single input to every row: sweeping the slider moves all saved cities at once, each by the same amount, and every row is that city's own clock at the slot's instant | `criterion15b_everyCityMovesWithTheSlot` |
| 15c | The slider is drivable with a finger and by a screen reader, and the plan follows the movement live rather than on release | `criterion15c_theSliderIsDrivableAndThePlanFollowsItLive` |
| 15d | The planner names the working day, judges it in each city's own zone, and marks the day each city is on | `criterion15d_thePlannerNamesTheWorkingDayInEachCitysOwnZone` |

**Grid contract (15b/15c, from `MeetingTimeTest`):** the first slot is 00:00, the last is 23:45,
every minute of the day snaps to a 15-minute slot within one step, and stepping the slider
always advances every city by exactly one step — including across midnight.

**Phase 2 — home-screen widget**

| # | Criterion | Test |
| --- | --- | --- |
| 16a | The app ships a home-screen widget that is declared the way Android requires (exported provider, provider info, `APPWIDGET_UPDATE`, a resizable 4×4 default footprint, the 30-minute backstop, a private ticker receiver) and reads the same store the screens read | `criterion16a_theWidgetIsDeclaredAndReadsTheSavedCitiesFromDataStore` |
| 16b | Every saved city appears with its own current time, in the app-wide format and with the user's labels; cities across the date line say which day they are on; past the row budget the rest are counted, never dropped; and a row always equals the card for the same city | `criterion16b_theWidgetListsTheSavedCitiesWithTheirTimesAndCountsTheRest` |
| 16c | The card is drawn — the device's own clock as the anchor, the cities below it, `+N more`, the sun/moon per city — and the whole card opens the app; drawn from the app's tokens resolved against the **device's** light/dark setting | `criterion16c_theCardDrawsTheCitiesAndOpensTheApp` |
| 16d | The widget is kept up to date: an alarm on every minute boundary while a widget exists, nothing when there is none, and an immediate redraw whenever the saved cities, labels or time format change | `criterion16d_theWidgetRefreshesWithTheClockAndWithTheSavedCities` |

**Phase 2 — integration, accessibility and polish**

| # | Criterion | Test |
| --- | --- | --- |
| 17a | The theme picker is wired into the running app: choosing a theme in the shell repaints the whole shell through `MainActivity`'s own wiring and is written back through the repository | `criterion17a_theAppearanceChosenInTheShellRepaintsTheApp` |
| 17b | Every control phase 2 added is at least 48 dp tall, and the icon-only controls — whose size the app sets — are 48 dp in both directions | `criterion17b_everyPhaseTwoControlMeetsTheTouchTargetGuideline` |
| 17c | The phase 2 surfaces are structured for assistive technology: titles and section headers are headings, the label dialog announces itself as a pane and names its field after the city, and a city card announces its difference | `criterion17c_thePhaseTwoSurfacesAreStructuredForAssistiveTechnology` |

Three further tests live in `WorldClockNavigationTest`, because they are about the flow rather
than about a surface: `theClocksScreenOpensTheMeetingPlannerAndBackReturnsToTheList`,
`thePlannerWithNothingSavedOffersTheCitySearch`, and `theSettingsActionOpensTheAppearanceSheet`.

---

## 12. Verification strategy

### 12.1 Four layers

| Layer | Tool | Command | What it proves |
| --- | --- | --- | --- |
| Unit / domain | JUnit 4 | `testDebugUnitTest` | Timezone maths, DST, solar position, projection, repository rules, directory |
| Screenshot / layout | Roborazzi + Robolectric (JVM, no emulator) | `testDebugUnitTest` | Real Compose rendering at a fixed 360 dp device size |
| End-to-end | Compose UI testing on a physical device | `connectedDebugAndroidTest` | Install, launch, real DataStore, real system clock and timezone, real rendering |
| Widget | Glance's own unit harness | `testDebugUnitTest` | The widget's tree, text, colour resolution and click action. It has **no** screenshot: Glance draws through `RemoteViews`, which Roborazzi cannot render |

**Decision record:** Roborazzi renders the real Compose tree on the JVM, so a screenshot
regression is caught without an emulator. `roborazzi.test.record=true` is set for all test
tasks, so `testDebugUnitTest` refreshes the snapshots in `app/build/screenshots/`.

### 12.2 Current results

**JVM suite: 264 tests, 0 failures, 24 suites.**

| Test file | Tests |
| --- | --- |
| `CityDirectoryTest` | 11 |
| `UserPreferencesTest` | 19 |
| `ClockRepositoryTest` | 17 |
| `EquirectangularProjectionTest` | 5 |
| `SolarPositionTest` | 12 |
| `WorldLandmassesTest` | 5 |
| `CityTimeFormatterTest` | 9 |
| `TimeDifferenceTest` | 6 |
| `MeetingTimeTest` | 10 |
| `SecondTickerTest` | 2 |
| `WorldClockPaletteTest` | 16 |
| `WorldClockThemeTest` | 5 |
| `AcceptanceCriteriaTest` | 37 |
| `AppIconTest` | 1 |
| `LayoutScreenshotTest` | 34 |
| `PlannerStateMapperTest` | 10 |
| `MeetingPlannerScreenTest` | 7 |
| `WorldClockNavigationTest` | 9 |
| `CitySearchContentTest` | 5 |
| `WorldMapScreenTest` | 10 |
| `WidgetStateMapperTest` | 12 |
| `WorldClockWidgetTest` | 11 |
| `WidgetMirrorTest` | 7 |
| `WidgetTickerTest` | 4 |
| **Total** | **264** |

**Screenshot artefacts: 35**, all 360 dp, in `app/build/screenshots/`:

`app_icon.png`, `app_shell_360dp.png`,
`clocks_screen_360dp.png`, `clocks_empty_360dp.png`, `clocks_screen_dark_360dp.png`,
`clocks_screen_labelled_360dp.png`, `clocks_screen_labelled_dark_360dp.png`,
`clocks_screen_12h_360dp.png`, `clocks_screen_warm_sepia_360dp.png`,
`clocks_screen_cool_slate_360dp.png`, `clocks_screen_high_contrast_360dp.png`,
`city_detail_360dp.png`, `city_detail_12h_360dp.png`, `city_detail_dark_360dp.png`,
`city_detail_labelled_360dp.png`, `city_label_dialog_360dp.png`,
`city_label_dialog_dark_360dp.png`,
`planner_screen_360dp.png`, `planner_screen_dark_360dp.png`, `planner_screen_12h_360dp.png`,
`planner_empty_360dp.png`,
`map_screen_360dp.png`, `map_screen_london_360dp.png`, `map_screen_summer_360dp.png`,
`map_screen_empty_360dp.png`, `map_screen_dark_360dp.png`, `map_screen_labelled_360dp.png`,
`map_screen_high_contrast_360dp.png`,
`search_sheet_360dp.png`, `search_sheet_empty_360dp.png`,
`settings_sheet_360dp.png`, `settings_sheet_dark_360dp.png`,
`settings_sheet_warm_sepia_360dp.png`, `settings_sheet_cool_slate_360dp.png`,
`settings_sheet_high_contrast_360dp.png`.

**Every theme has a list render and an appearance-sheet render:**
Light Monochrome is `clocks_screen_360dp.png` / `settings_sheet_360dp.png`, True Dark is
`clocks_screen_dark_360dp.png` / `settings_sheet_dark_360dp.png`, and Warm Sepia, Cool Slate
and High Contrast are the `*_warm_sepia_*`, `*_cool_slate_*` and `*_high_contrast_*` pairs.
`map_screen_high_contrast_360dp.png` is the one extra: High Contrast is the theme whose map
had to be redesigned (white continents, see [section 7.1](#71-colour)), so it is the theme
whose map is worth looking at directly.

The light-mode artefacts are byte-for-byte what phase 1 produced: the light tokens and the
light `mapNight` wash are unchanged, so nothing was re-styled by adding the dark palette.
Phase 2 stage 2 changed the city card's internal layout (three lines instead of a two-column
grid), which is visible in every clocks render — deliberately, and described in
[section 5.3](#53-clocks-screen). Phase 2 stage 3 added the `PLAN A MEETING` action to the
clocks renders and the three planner renders. The integration pass added the four renders that
pair the phase 2 features with the dark theme and the 12-hour format, and re-recorded every
other one: the segmented control grew to a 48 dp target and the small circular buttons now sit
in 48 dp slots ([section 5.12](#512-phase-2-integration-accessibility-and-polish)). The
five-theme pass added the seven per-theme renders above and re-recorded the two map renders,
whose `IN NIGHT` legend swatch gained an outline — on the two darkest themes the swatch had
been a black dot on a black background.

**The widget has no screenshot.** Glance draws through `RemoteViews`, which Roborazzi cannot
render, so the widget is asserted through Glance's own unit harness instead — the tree, the
texts, the colour resolution and the click action — and its *shape* is unverified visually
([section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen)).

**Device suite: 2 tests, 0 failures** (`connectedDebugAndroidTest`).

**Device run:** see [section 14.4](#144-device-verification) and
[section 14.5](#145-stage-4--dark-theme).

### 12.3 Reference values used by the tests

Screenshot and unit tests pin these instants:

| Name | Instant |
| --- | --- |
| `referenceInstant` (winter) | `2024-01-20T16:40:00Z` |
| `summerInstant` | `2024-07-20T04:40:00Z` |
| Device zone used in previews/tests | `Asia/Yangon` |

Expected offsets (winter): Tokyo `01:40 UTC+9 Sun 21 Jan`, Sydney `03:40 UTC+11`,
Los Angeles `08:40 UTC-8 Sat 20 Jan`, New York `11:40 UTC-5`, London `16:40 UTC+0`.
Expected offsets (summer): Los Angeles `UTC-7`, New York `UTC-4`, London `UTC+1`,
Sydney `UTC+10`. Mumbai is `UTC+5:30`.

**Widget reference values.** The widget tests use the same winter instant and the same device
zone, so the first six rows are the first six saved cities, only Tokyo and Sydney are on another
date, and the day/night icons follow each city's own local hour. Two harness details are load-
bearing: `runGlanceAppWidgetUnitTest` must be given `setContext(context)` or Glance's own
`LocalContext` throws, and Glance's `hasText` / `hasContentDescription` are substring matchers,
so the tests use `hasTextEqualTo` / `hasContentDescriptionEqualTo` where the wording itself is
the thing being checked.

**JVM text metrics are not real.** Robolectric's stub font measures text at close to zero width,
so a test can prove a control is at least 48 dp *tall* but not that it is 48 dp *wide* when its
width comes from its label. Criterion 17b asserts widths only for the icon-only controls, whose
size the app sets; the PNGs are the evidence for the rest.

**A bottom sheet is a window.** Touch injection does not cross into `ModalBottomSheet` under the
JVM renderer: `performClick()` on one of its options does nothing, while
`performSemanticsAction(SemanticsActions.OnClick)` reaches the callback. `criterion17a` uses the
action and says why.

---

## 13. Build and run

**IMPLEMENTED**

```powershell
# Build the debug APK
.\gradlew.bat assembleDebug

# Run the JVM suite (also refreshes screenshots in app/build/screenshots/)
.\gradlew.bat testDebugUnitTest

# Full clean verification
.\gradlew.bat clean assembleDebug testDebugUnitTest

# Install on the connected device
.\gradlew.bat installDebug

# Run the on-device end-to-end suite
.\gradlew.bat connectedDebugAndroidTest
```

Known benign build noise, safe to ignore:

- `Unable to strip libandroidx.graphics.path.so, libdatastore_shared_counter.so`
- On PowerShell, Gradle's `e:` diagnostics surface as `NativeCommandError`; filter with
  `Select-String -Pattern "^e: |FAILED|BUILD"`.

**Known baseline:** a full `clean assembleDebug testDebugUnitTest --no-build-cache` takes roughly
3 m 10 s and ends at **264 tests, 0 failures, 24 suites, 35 screenshots**. The APK is about 13.0 MB
at `app/build/outputs/apk/debug/app-debug.apk` (13.0 MB after phase 2 stage 4, up from 11.37 MB:
Glance and its appwidget runtime add about 1.6 MB; the five themes added no assets).

---

## 14. Implementation log

### 14.1 Stage 0 — scaffold

Gradle wrapper, `local.properties`, `.gitignore`, manifest with
`windowSoftInputMode="adjustResize"`, `WorldClockApp`, manual `AppContainer`,
`saved_cities.proto` code generation.

### 14.2 Stage 1 — visual foundation

Full monochrome `lightColorScheme`; Inter `FontFamily` plus Material 3 type roles and
`WorldClockTextStyles`; `WorldClockShapes`; `CityCard`; `CircleIconButton`;
`WorldClockBottomBar`; adaptive monochrome app icon; `WorldClockRoot` shell;
`MainActivity` with `enableEdgeToEdge()`; `City` + `CityDirectory`.

### 14.3 Stage 2 — working clocks

`SecondTicker`; `CityTimeFormatter` / `CityTime`; `WorldClockViewModel` +
`WorldClockUiState` + `buildWorldClockUiState`; `CitySearchSheet` / `CitySearchContent`;
`WorldClockSegmentedControl`; `CityDetailScreen`; `ClocksScreen`; `ClockRepository`
(with `PrefsError` → snackbar); `UserPreferences` / `seededWithDefaults()`; `Routes` /
`NavHost`; `PreviewSamples`.

### 14.4 Stage 3 — world map, polish and verification

Added `domain/map/*` and `ui/components/WorldMapCanvas.kt` +
`ui/screens/WorldMapScreen.kt`; deleted `MapPlaceholder`; `Routes.MAP` now renders the real
map; `CityCardModel` gained `latitude` / `longitude`, populated by `buildWorldClockUiState`.

Two real bugs were found and fixed while verifying the map:

1. Sidereal time was used in hours instead of degrees — fixed by multiplying by `15.0`.
2. `atan2` was used in `terminatorLatitude` — replaced with `atan`.

**Device verification (Xiaomi `24129RT7CC`, codename `rodin`, Android 16 / API 36,
1220×2712 px at 520 dpi = 375 dp wide).**

| Check | Result |
| --- | --- |
| `installDebug` | Installed successfully |
| Launch | Starts, gains focus, no crash in `logcat` (`-b crash` empty) |
| Clocks screen correctness | **Correct.** Yangon `00:05 Oct 17` (UTC+6:30) → Tokyo `02:35 UTC+9`, Sydney `04:35 UTC+11` (AEDT, correct DST), Los Angeles `10:35 Fri UTC-7` (PDT, correct DST), New York `13:35 Fri UTC-4` (EDT), London `18:35 Fri UTC+1` (BST). Sun/moon icons match the 06:00–17:59 rule. |
| Layout width | 375 dp — wider than the 360 dp target, so no clipping risk |
| Map tab rendering | **Verified.** Semantics tree shows the canvas, the active meridian, all five markers at correct projected positions, and the selected-city panel. |
| Marker tap → active city | **Verified.** Tapping the London marker flips `Selected` from `los-angeles` to `london`. |

**Limitation:** `adb shell input tap` and `input keyevent` are blocked on this device
(`java.lang.SecurityException: Injecting input events requires the caller … to have the
INJECT_EVENTS permission`) because Xiaomi HyperOS gates simulated input behind a
user-enabled developer setting. Scripted taps via `adb` are therefore not possible; the
Compose UI tests drive the app through the instrumentation instead, which is permitted.

### 14.5 Stage 4 — dark theme (phase 2)

Stage 4 of the project, stage 1 of phase 2.

**Added:** the `WorldClockPalette` data class with a light and a dark instance and the
`LocalWorldClockPalette` composition local; `WorldClockColors` reworked into Material-style
composable getters so no call site had to change; `ThemeMode` plus its proto mapping; the
`SavedThemeMode` enum and `theme_mode = 5` in the schema; `ClockRepository.setThemeMode`;
`themeMode` threaded through `WorldClockUiState` and `buildWorldClockUiState`;
`WorldClockTheme(themeMode)` with `resolveDarkTheme`; `ic_settings`; `SettingsSheet` /
`SettingsContent`; the gear action on the Clocks screen; `values-night` launch theme and
window background; `SystemBarAppearance`.

**Changed:** `WorldMapCanvas`'s `DrawScope` steps now take a `WorldClockPalette` parameter
(draw code is not composable), and the map's night wash moved from a private `NIGHT_ALPHA`
constant onto the `mapNight` token. `MainActivity` applies the theme so the palette is
correct on the first composition of every screen.

**Device verification — dark theme (Xiaomi `24129RT7CC`, Android 16 / API 36, 375 dp wide).**

The device was put into night mode with `adb shell cmd uimode night yes`. Because the stored
mode was `System` (no `theme_mode` field on the wire — proto3 omits the zero value, which is
the `System` value), the app resolved to the dark palette with no further interaction:

| Check | Result |
| --- | --- |
| Dark palette applied | **Correct.** Near-black background, `#1A1A1A` rows, light text |
| Inverted selected state in dark | **Correct.** Saved-city rows show a *light* circle with a *dark* check — the mirror of the light theme |
| Status bar icons | **Correct.** Light icons on the dark app surface |
| Search sheet in dark | **Correct.** Dark sheet, dark field with light outline, muted offset text |
| Settings sheet on device | **Correct.** Opened from the gear; `System` / `Light` / `Dark` all rendered; selection state visible; choosing a mode repainted the app immediately |
| Data integrity | The user's own saved cities, order and active city were untouched throughout |

**Note on method.** The system night mode was borrowed for this check and restored to
`auto` afterwards. The app's own stored preferences were backed up first
(`app/build/device/prefs_backup/`) and never modified — the theme was driven through the
settings sheet and the system setting, exactly as a user would.

**Not verified on device:** the explicit `Dark`-on-a-light-device case that
`SystemBarAppearance` exists for. Isolating it needs the stored mode set to `Dark` while the
device is in light mode; the reasoning is recorded in
[section 7.1](#71-colour) and the fix is exercised by every screenshot test.

### 14.6 Stage 5 — time difference and city labels (phase 2)

Stage 2 of phase 2, the project's fifth stage.

**Added:** `domain/time/TimeDifference.kt` (an inline value class over a signed minute count,
with `between(cityZone, referenceZone, instant)` built on `ZoneRules.getOffset`),
`ui/components/TimeDifferenceLabel.kt`, `ui/screens/CityLabelDialog.kt`, `ic_edit`, and the
fetched-string resources for both features.

**Changed:**

- `CityCard` — a label and a difference per card, and the card body rebuilt as three
  independent lines (`offset | weekday`, `name | time`, `region | difference`). A shared
  two-column grid let the difference line's width shrink the column holding the city name,
  which truncated `Tokyo (Head Office)`. Per line, only the item that does not fit gives way.
- `CityDetailScreen` — the `COMPARED WITH YOUR LOCAL TIME` section, the label line and the
  `ADD LABEL` / `EDIT LABEL` action.
- `WorldMapScreen` — pills and the selected-city panel use the labelled name; the panel shows
  the difference.
- `WorldClockViewModel` / `buildWorldClockUiState` — `cityLabels` threaded through as the
  single source of both labels and differences.
- `ClockRepository` — `setCityLabel`; `UserPreferences` — `cityLabels` plus pruning;
  `saved_cities.proto` — `city_labels = 6`.
- `WorldClockRoot` — the shell became an explicit `Box` stack, because the label dialog is
  drawn in the app's own composition rather than in a platform dialog window
  ([section 5.8](#58-city-label-dialog)).
- `Type.kt` — `CityDifference`, `DetailCityLabel`, `DetailDifference`, `MapCityDifference`.
- `string.xml` — the difference wording, the label dialog, and two accessibility strings.

**A finding worth keeping.** A text field inside a Compose **dialog window** never reaches an
idle state under Robolectric: `setContent` on a bare `AlertDialog` containing an
`OutlinedTextField` fails with `Compose did not get idle after 50383 attempts in 60 SECONDS`,
while the identical `OutlinedTextField` in the main window is driven normally by
`performTextInput`. This was isolated with a throwaway probe test (a bare `Dialog`, a bare
`AlertDialog`, an `AlertDialog` with a field, and a field in the main window) and it is the
reason the dialog is a modal overlay. The app's bottom sheets avoid the same trap differently —
their **content** composables (`CitySearchContent`, `SettingsContent`) are what the tests
render, never the `ModalBottomSheet` itself.

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **165 tests, 0
failures**, with 21 screenshots; the three new renders are `clocks_screen_labelled_360dp.png`,
`map_screen_labelled_360dp.png`, `city_detail_labelled_360dp.png` and
`city_label_dialog_360dp.png`. The renders confirmed the layout fixes: `Tokyo (Head Office)`
and `New York (HQ)` print in full at 360 dp, the card still measures 93 dp, and `Save` no
longer wraps onto two lines.

### 14.7 Stage 6 — meeting planner (phase 2)

Stage 3 of phase 2, the project's sixth stage.

**Added:** `domain/time/MeetingTime.kt` (a `MinuteOfDay` value class over minutes since
midnight, validation in the constructor, `snappedToGrid`, `onDate`, `ofInstant`, and the
`isWithinWorkingDay` rule), `ui/PlannerUiState.kt`, `ui/mapping/PlannerStateMapper.kt`,
`ui/components/DaySlider.kt` (the hand-drawn 24-hour control plus `DaySliderTags`), and
`ui/screens/MeetingPlannerScreen.kt`.

**Changed:**

- `Routes` — `PLANNER`; `WorldClockRoot` — the route, with the slot held in `rememberSaveable`
  and derived from `state.now` until the slider is touched.
- `ClocksScreen` — `onPlanMeetingClick` and the `PLAN A MEETING` action under the section
  header.
- `WorldClockViewModel` — `deviceZone`, so the planner's route and mapper use the same zone the
  differences are measured against, with no loading corner case.
- `PreviewSamples` — `plannerState(labels)` and `emptyPlannerState()`, so planner renders are
  built by the production mapper.
- `Type.kt` — the five planner styles; `strings.xml` — the planner's wording, the two legend
  items, and three accessibility strings.

**Numbers that had to be right.** The grid is 15 minutes, so a day has 96 slots from 00:00 to
23:45 — the last step stops short of midnight because 24:00 is the next day, and `Compose`
counts the steps *between* the ends, hence `GRID_STEPS = 94`. The slider's touch mapping and its
drawing use the same 24-hour scale while the value range stops at 23:45, which is why the marker
at the far right sits a hair short of the tick for `24`.

**Two daylight-saving corners are handled by `java.time` and pinned by tests.** A slot inside
the spring-forward gap (New York, 02:30 on 10 March 2024) is moved forward by the length of the
gap; a slot the clocks pass through twice (01:30 on 3 November 2024) takes the earlier offset.

**A near-miss caught by the render.** The working-day band was first drawn as a fill of the
whole track height. With the marker resting on the band's start it read as a slider's own
active range — a filled selection from the thumb to 18:00. It is now a rounded pill inset inside
the track, with the hour ticks drawn over it in `border` and the band in `accent`, so the ruler
stays continuous and the band cannot be mistaken for the control's own state.

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **199 tests, 0 failures**,
20 suites, with 24 screenshots; the three new renders are `planner_screen_360dp.png`,
`planner_screen_dark_360dp.png` and `planner_empty_360dp.png`, and `clocks_screen_360dp.png`
shows the new action row. `criterion15b` sweeps all 96 slots against times computed independently
in the test, and asserts that every step moves every city by exactly one step.

**Not verified on device:** the planner has not been driven on hardware. The slider's touch
mapping, the TalkBack announcement of its `stateDescription`, and the feel of the 15-minute
snapping are all covered on the JVM but not on a real touchscreen (see
[section 15.9](#159-open--the-meeting-planner-has-not-been-touched-on-hardware)).

### 14.8 Stage 7 — home-screen widget (phase 2)

Stage 4 of phase 2, the project's seventh stage.

**Added:** `ui/WidgetUiState.kt`, `ui/mapping/WidgetStateMapper.kt` (`WIDGET_MAX_ROWS = 6`),
`ui/widget/WorldClockWidgetContent.kt`, `ui/widget/DayNightColorProvider.kt`,
`widget/WorldClockWidget.kt`, `widget/WorldClockWidgetReceiver.kt`, `widget/WidgetTicker.kt`,
`widget/WidgetMirror.kt`, and `res/xml/world_clock_widget_info.xml`.

**Changed:** `AndroidManifest.xml` — the widget receiver (`exported`, the `APPWIDGET_UPDATE`
filter, the `android.appwidget.provider` meta-data) and the private ticker receiver;
`WorldClockApp.kt` — an application scope with a `CoroutineExceptionHandler` and an idempotent
`startWidgetMirror()`, called only when a widget exists; `strings.xml` — the widget's wording and
the `widget_more_cities` plural; `app/build.gradle.kts` — Glance and its test harness.

**Numbers that had to be right.** Six rows is the budget, and it is a constant rather than a
measurement: `LocalSize` is unreliable below API 31, so the card draws six and counts the rest
with `+N more` from the plural resource. The default footprint is 4×4 cells — 250 dp minimum in
both directions, which is exactly where six rows and the header stop being cramped. The tick
alarm is set on the *next* minute boundary (`nextMinuteBoundary`), not one minute from now, so
the widget's seconds stay aligned with the device clock.

**Three Glance API constraints were found by reading the resolved jars, not the docs.**
`GlanceComposable` is a `@ComposableTargetMarker` rather than a `@Composable` annotation, so a
content composable needs both; `ColorFilter` is `androidx.glance.ColorFilter`, not the Compose
one; and Glance 1.1 has no literal day/night `ColorProvider` — only `ColorProvider(@ColorRes)`
and `ColorProvider(Color)` — and no `letterSpacing` or `overflow` in its `TextStyle`. The first
of those shapes the file signatures, the last three are why `DayNightColorProvider` exists and
why a long city name is clipped by its own weight rather than ellipsised.

**Decision record — the widget follows the device theme, not the app's stored choice.** A home
screen is shared with other apps; a card that ignores the device's light/dark setting looks
broken next to them. The consequence is that an explicit app choice and the widget can disagree,
which is recorded in [section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

**Decision record — the row budget, the alarm and the theme are the widget's three judgement
calls,** and each is stated in [section 5.11](#511-home-screen-widget) with the alternative that
was rejected: a measured row budget (unreliable below API 31), WorkManager (15-minute minimum),
`setExactAndAllowWhileIdle` (needs `SCHEDULE_EXACT_ALARM` for no visible gain).

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **237 tests, 0 failures,
24 suites**. The four new suites are `WidgetStateMapperTest` (12), `WorldClockWidgetTest` (11,
including the day/night colour resolution and the click action asserted against `MainActivity`),
`WidgetMirrorTest` (7) and `WidgetTickerTest` (4), plus criteria 16a–16d. The widget adds **no
screenshot**: Glance draws through `RemoteViews`, which Roborazzi cannot render, so the card is
asserted through `runGlanceAppWidgetUnitTest` instead.

**Not verified on device:** the widget has never been on a real home screen — see
[section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

### 14.9 Stage 8 — integration, accessibility and polish (phase 2)

Stage 5 of phase 2, the project's eighth stage. No screen's meaning changed; this pass connected
what the previous three stages had already built and finished their accessibility.

**Added:** `MinTouchTarget` (`ui/components/CircleIconButton.kt`), three acceptance criteria
(17a–17c), three navigation tests, four screenshot renders and one string
(`a11y_city_label_field`).

**Changed:**

- `CircleIconButton` — the click, the label and the semantics moved from the drawn circle to a box
  around it that is never below 48 dp, so a 44 dp control still has a comfortable target.
- `WorldClockSegmentedControl` — each option is a 48 dp target wrapped around the unchanged pill.
- `ClocksScreen`, `MeetingPlannerScreen`, `CityDetailScreen` — the bordered and text action rows
  (`PLAN A MEETING`, `ADD CITY`, `Find a city`, `EDIT LABEL`) are clamped to 48 dp with
  `heightIn(min = MinTouchTarget)`.
- The same screens plus `SettingsSheet` and `CityLabelDialog` — `heading()` on every screen title
  and section header; `paneTitle` and a `contentDescription` naming the city on the label dialog.

**What the pass found, and what it means for later tests.**

- **A `ModalBottomSheet` is a separate window.** `performClick()` on one of its options does
  nothing under the JVM renderer — the injected touch never crosses into the sheet's window —
  while `performSemanticsAction(SemanticsActions.OnClick)` reaches the callback. The shell-level
  appearance test uses the action and says why. `criterion12c`, which renders `SettingsContent`
  directly, is unaffected.
- **Robolectric's stub font measures text at close to zero width.** A node's *height* is real; a
  text-driven *width* is not. Criterion 17b therefore asserts 48 dp in both directions only for
  the icon-only controls, whose size the app sets, and asserts height for everything else. The
  first draft of that test failed on `System` with "Actual width is 31.5 dp" — the `System` pill
  is text plus 28 dp of padding, and the stub font measures `System` at 3.5 dp.

**Verification.** A clean build — `clean assembleDebug testDebugUnitTest --no-build-cache` —
finishes at **247 tests, 0 failures, 24 suites**, with 28 screenshots, in about 2 m 30 s. The
four new renders pair the phase 2 features with the dark theme and the 12-hour format
(`clocks_screen_labelled_dark_360dp.png`, `clocks_screen_12h_360dp.png`,
`planner_screen_12h_360dp.png`, `city_label_dialog_dark_360dp.png`); the other 24 were
re-recorded because the segmented control and the small circular buttons changed size.

**Not verified on device:** none of this pass was seen on hardware. The enlarged targets were
chosen to meet the 48 dp guideline, not from a user report, and the headings and the dialog's
`paneTitle` have not been heard through TalkBack — see
[section 15.11](#1511-open--the-accessibility-pass-has-not-been-heard-on-hardware).

### 14.10 Stage 9 — the five-theme pass (phase 2)

The user's next request: replace the light/dark/system switch with **five switchable themes** —
Light Monochrome, True Dark, Warm Sepia, Cool Slate and High Contrast — each switchable in one
tap from Settings, each persisted so it survives a restart, and each covered by a screenshot.

**Decision record (user, 2026-10-18).** The choice of *how* to reconcile the old three-mode
setting with five themes was put to the user, who chose "replace with the five palettes":
Settings offers exactly the five themes, one tap each. The consequence is that device-following
is gone — a theme is an exact look, not a light/dark switch. The migration is recorded in
[section 8.1](#81-proto-schema).

**Added:** `data/prefs/AppTheme.kt` (the enum, its ids, the proto mapping and the migration);
three palettes and the `mapLandmass` token in `theme/Color.kt`; `paletteFor(theme)` in
`theme/Theme.kt`; the five-row picker in `SettingsSheet.kt`; five `theme_*` strings; seven
screenshots.

**Changed:**

- `ThemeMode.kt` **deleted**; `AppTheme` replaces it everywhere (`UserPreferences.theme`,
  `ClockRepository.setTheme`, `WorldClockViewModel.setTheme`, `WorldClockUiState.theme`,
  `WorldClockRoot`, `MainActivity`).
- `saved_cities.proto` — new `SavedAppTheme` enum and `optional theme = 7`; the old
  `theme_mode = 5` is kept read-only for migration.
- `resolveDarkTheme(mode, systemInDarkTheme)` **deleted**. It had no business existing once a
  theme names an exact palette: `isSystemInDarkTheme()` is no longer read anywhere in the app.
- `SettingsSheet` — the three-option segmented control became five named rows. A segmented
  control cannot carry `Light Monochrome` at 360 dp, and a theme needs a preview, so each row is
  a full-width ≥ 48 dp radio row holding a swatch (decorative, `contentDescription` absent), the
  theme's name, and an inverted pill when it is the one in force. The rows sit in
  `selectableGroup()` so the five are announced as one set of options.
- `WorldMapCanvas` — landmasses now use `mapLandmass` rather than `accent`, and
  `WorldMapScreen`'s `IN NIGHT` legend swatch gained an outline.

**The two design problems the pass had to solve.**

1. **A black shadow over a black ocean is invisible.** High Contrast's background *and* card are
   pure black, so a black night wash leaves the ocean unchanged and the terminator disappears.
   Two ways out were considered: give High Contrast a light wash (rejected — a light wash reads
   as daylight, and "night is always painted darker" is an invariant the other four themes rely
   on), or separate the two roles `accent` was carrying. `accent` is also the fill of the app's
   soft pills, where white text has to stay readable, so it must stay dark; the landmasses needed
   to go light. `mapLandmass` was split out of `accent` for exactly this, is `accent` in the four
   soft themes (so their maps are unchanged), and is white in High Contrast. The wash is drawn
   over the continents, so that is where the terminator now lives. `map_screen_high_contrast_360dp.png`
   is the evidence.
2. **`System` could not be migrated honestly.** A user who had chosen `System` was seeing
   whichever of two palettes their device was set to, and that fact is not in the store. Rather
   than guess, `SYSTEM → Light Monochrome` (the default), and the reasoning is written into the
   migration function and the proto decision record.

**What the pass found, and what it means for later tests.**

- **A "night is darker" test does not prove the terminator is visible.** The old check — the wash
  has a low luminance and a higher alpha on the dark theme — passes for a wash that is painted
  over nothing. The palette test now composites the wash *over the landmasses* and asserts the
  result is darker, which is the property the map actually depends on.
- **`optional` is what makes the migration decidable.** Light Monochrome is the proto3 zero
  value, so a plain enum field would have made "nothing stored" and "Light Monochrome chosen"
  the same bytes, and every dark-mode user would have been handed the light theme on upgrade.
  A test switches the legacy field to dark *and* the new field to Light Monochrome to pin the
  presence bit's precedence.
- **The theme picker's rows are `selectable`, and a `selectable` node merges its children.** The
  row's bounds are therefore what `assertHeightIsAtLeast(48.dp)` measures, and its name is what
  `onNodeWithText` finds — the same shape the segmented control's options already had, so
  criterion 17b's touch-target sweep extended to the five rows without a new mechanism.

**Verification.** A clean build — `clean assembleDebug testDebugUnitTest --no-build-cache` —
finishes at **264 tests, 0 failures, 24 suites**, with **35 screenshots**, in about 3 m 10 s;
APK 13.0 MB. `WorldClockPaletteTest` went from 10 tests to 16 because every contrast, inversion,
accent and map check now runs over all five themes rather than the original two, and
`WorldClockThemeTest` pins the five-way mapping, the ids and the default.

**Not verified on device:** the three new palettes have never been seen on the Redmi Turbo 4 —
only in Roborazzi renders — and the widget still resolves its own colours from the device, so
choosing Warm Sepia or High Contrast leaves the home-screen card neutral grey. That is the
pre-existing behaviour ([section 5.11](#511-home-screen-widget)), not a regression, but it is now
visible enough to be worth a decision — see
[section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme).

**Not verified by a human:** the five themes have not been judged by eye for taste, only asserted
for contrast and distinctness. Warm Sepia's, Cool Slate's and High Contrast's derived supporting
greys are the assistant's choices, like the dark theme's before them — see
[section 15.8](#158-open--the-palette-values-are-partly-derived).

---

## 15. Known issues and open questions

### 15.1 RESOLVED — instrumented test defects

`WorldClockEndToEndTest` had **four test-authoring defects** (not app defects). The file has
been **deleted** and replaced by `WorldClockDeviceSmokeTest.kt`, a deliberately minimal
two-test on-device suite. Instrumented and Espresso tests were **deprioritised by the user**:
they are flaky across OEM animation and timing settings, and the unit + Roborazzi layers
already carry the coverage.

The findings are kept because they are still true of the app, and any future device test will
hit them:

| # | Root cause (still true) | Guidance for future device tests |
| --- | --- | --- |
| 1 | The 12h/24h control lives on the **city detail screen**, not the Clocks screen. | Navigate into a city's detail first. |
| 2 | A city card shows the **region** (`Myanmar`) and the **offset** (`UTC+6:30`), never the IANA id. | Assert on `Yangon` and the offset. |
| 3 | Espresso's `pressBack()` refuses to run when the device animation scale is non-zero. | Do not use `pressBack()`. Use the on-screen Back button or `activity.onBackPressedDispatcher`. |
| 4 | Every city mutation is a disk write plus a flow round-trip, so the UI updates a beat later; and during a navigation transition both destinations are briefly composed. | Wait with `waitUntil` against **semantics state**, not display text. |

**Evidence for #4.** A temporary diagnostic (`waitUntil` on the London marker's `Selected`
semantics, 8 s budget) reported:

```text
selected after navigating to map: los-angeles
london selected after tap: true
los-angeles still selected: false
timed out waiting for the tap: false
```

So the marker tap **does** work; it is simply not synchronous. The diagnostic file was
removed after use.

**Consequence beyond the tests.** This is a genuine architectural characteristic worth
remembering: on a real device every mutation is a *disk* write followed by a flow round-trip,
so the UI updates a beat later. The JVM suite never exposed this because
`FakeSavedCitiesDataStore` is synchronous. This now applies to the theme as well — the
settings sheet repaints one beat after the tap, which is why `criterion12c` asserts on the
callback rather than on the resulting palette. Any future code that assumes a mutation is
visible immediately will be wrong; any future UI that needs instant feedback should apply it
optimistically.

### 15.2 RESOLVED — dependency injection

The app uses a hand-written `AppContainer` plus a `Context.appContainer` extension. Hilt was
offered as an alternative and the **user confirmed the manual approach** (2026-10-17):
it keeps the app lightweight, avoids APK growth, compiles faster, and all existing tests
already pass. Switching later remains containable because
`ui/mapping/WorldClockStateMapper.kt` is the single data → UI path.

### 15.3 CLOSED — emulator tooling

Emulator end-to-end verification was originally blocked: the Android SDK has no
`cmdline-tools` (no `sdkmanager` / `avdmanager`) and no AVDs exist. This is now **moot** —
a physical device is connected and has been used for end-to-end verification. Reinstalling
`cmdline-tools` is no longer required unless a second, scriptable device is wanted for
`adb`-driven UI automation.

### 15.4 Note — not a defect

An early device screenshot showed the app opening on the Tokyo detail screen instead of the
Clocks list. This was a stale task being resumed by `am start`, not a navigation bug. A
`force-stop` followed by a fresh start shows the Clocks list, as required.

### 15.5 OPEN — no version control

`D:\MY APP\WorldClock` is **not a git repository**. All work exists only as working-tree
files. There is no history, no diff, and no way to recover an overwritten file.

This is now the highest-risk item in the project: phases 1 and 2 both restyled shared files
(`Color.kt`, `Theme.kt`, `ClocksScreen.kt`, `WorldMapCanvas.kt`), and there is no way to diff
or revert any of it.

### 15.6 RESOLVED — section 11 wording

Confirmed by the user; see [section 0.1](#01-section-11-provenance--resolved).

### 15.7 OPEN — first-frame flash when the theme disagrees with the device

The theme is stored in DataStore, which is read asynchronously. Until the first emission arrives
the app paints `AppTheme.Default` — Light Monochrome — and the *launch theme*
(`res/values/` and `res/values-night/`) still follows the device. So whenever the chosen theme
and the device's own setting disagree there is one wrong-coloured frame at cold start: True Dark
or High Contrast on a light device starts light, Light Monochrome on a dark device starts dark.

Both directions now exist, where the old three-mode scheme had a flash only for an explicit
`Light` or `Dark` on a disagreeing device — `System` matched the launch theme exactly. Replacing
the modes with five exact palettes removed that no-flash case, so this is slightly more visible
than it was.

- Impact: low. The read completes in tens of milliseconds and only affects cold starts, and the
  status/navigation-bar icons are already correct because `SystemBarAppearance` follows the
  palette rather than the device.
- Full fix would mean blocking the first frame on the DataStore read, or mirroring the choice
  into a synchronous store such as `SharedPreferences` purely for the launch theme — which would
  also mean shipping five launch themes instead of two. Not worth the complexity yet.

### 15.8 OPEN — the palette values are partly derived

Five of Light Monochrome's and True Dark's values were specified by the user; the rest, and
everything in Warm Sepia, Cool Slate and High Contrast except the base pairs, were **derived**
to preserve the relationships the design relies on
(see [section 7.1](#71-colour)). They have been reviewed as relational values — contrast,
inversion, distinctness, a visible terminator — and not as design values, and nobody has looked
at them by eye except in the Roborazzi renders.

The most opinionated derivations:

- `primary = foreground` in every theme, so the selected card is the *inverse* of the base
  surface: the dark themes select a light card with dark text, mirroring the light themes'
  black/ink card. If that reads as too heavy, the alternative is a subtly raised surface
  (around `#2A2A2A` in True Dark) with light text, which loses the inversion but is quieter.
- Warm Sepia's and Cool Slate's `mutedForeground` are the assistant's choices (`#7A7065`,
  `#64748B`), picked to clear 4:1 on their own background. They are supporting-label greys, not
  body text, but the taste is unjudged.
- High Contrast is the most derived of all: only pure black and pure white were specified, and
  the four supporting greys (secondary, muted, accent, and the black night wash's alpha) exist to
  stop two things becoming indistinguishable. Reasonable people could choose different greys.

### 15.9 OPEN — the meeting planner has not been touched on hardware

The day slider is the first control in the app that is **dragged** rather than tapped, and the
first that reports a value to TalkBack. Its behaviour is covered on the JVM — touch mapping,
snapping, the `setProgress` action, the read-out — but what a JVM cannot tell us is still open:

- How the 15-minute snapping feels under a real finger. Continuous dragging with a snapped
  value can feel sticky if the step is too coarse, and 15 minutes is a judgement.
- Whether TalkBack announces `stateDescription` as written. It is set on the same node as
  `progressBarRangeInfo`, which is the contract Material's own slider uses, but the spoken
  result has not been heard.
- Whether a 10 dp thumb is comfortable to grab. It meets the touch-target rule because the whole
  48 dp strip is the target, not just the circle — which is also why a miss still lands.

The same input-injection limitation that blocks tapping the label dialog on this device applies
here: this ROM refuses `adb shell input tap` and `adb shell input keyevent`, so a scripted
on-device pass would have to wait for a human finger.

### 15.10 OPEN — the widget has not been placed on a real home screen

The widget is asserted thoroughly on the JVM — the row budget and the `+N more` count, the
per-city time and day, the sun/moon per city, the colour each token resolves to under a light and
a dark device configuration, the click action against `MainActivity`, the alarm on the next
minute boundary, and the mirror's redraw on a saved-cities change. What a JVM cannot tell us is
still open:

- **Whether the card looks right in a real launcher.** Glance draws through `RemoteViews`, which
  Roborazzi cannot render, so there is no widget screenshot at all. Six rows at 250 dp is
  arithmetic, not a picture.
- **Whether the launcher gives the card the size it asks for.** A 4×4 default footprint with
  `targetCellWidth`/`targetCellHeight` is a request; different launchers resolve cells
  differently, and the card has never been measured on a grid.
- **Whether the minute alarm survives a real doze cycle.** `setAndAllowWhileIdle` is inexact by
  design and Android throttles it to roughly once per 9 minutes in deep doze. The 30-minute
  `updatePeriodMillis` is the backstop, but the interval has not been observed on hardware.
- **Which font the launcher uses.** `RemoteViews` cannot carry a bundled font, so the widget is
  drawn in the platform's family, not Inter, and its figures are not tabular — the minute column
  will shift width as the digits change where the app's rows do not.
- **Whether the app's theme choice and the widget's device theme disagree in practice.** They do:
  the widget follows the device, the app follows the stored choice, so Warm Sepia or High
  Contrast leaves the card neutral grey. See
  [section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme).

The device also blocks the obvious manual check — this ROM refuses `adb shell input tap` and
`adb shell input keyevent` — so placing the widget on the home screen is a human action.

**Machine-side preconditions are verified, 2026-10-18.** The provider is registered with the
system (`dumpsys appwidget`: `WorldClockWidgetReceiver`, `updatePeriodMillis=1800000`,
`resizeMode=3`, `widgetCategory=1`), and the widget does **not** need to be placed for that to be
true. The check also found a real problem: the build installed on the device was from
2026-10-17 23:10 — **before the widget existed** — so there were no widget receivers and nothing
in the picker. The current `app-debug.apk` was installed over it (`adb install -r`, saved cities
kept) before the human pass, so the first attempt at Step 1 fails for a reason that has nothing
to do with the app.

The remaining work, step by step, is in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 15.11 OPEN — the accessibility pass has not been heard on hardware

The integration pass set `heading()` on every title and section header, `paneTitle` on the label
dialog, and `contentDescription` naming the city on the dialog's field. All of it is asserted as
semantics on the JVM, and none of it has been heard through TalkBack:

- Whether `paneTitle` on an **in-composition** overlay is announced as panes usually are. It is
  the standard Compose idiom for "a pane appeared" and it is the only signal the dialog has, but
  TalkBack's treatment of a pane title inside the same window is not something a JVM test can
  report.
- Whether a field whose `contentDescription` is `Label for Tokyo` is read instead of, or as well
  as, its placeholder. The description is on the same node as the editable text, and the spoken
  order has not been heard.
- Whether heading navigation is a help or a nuisance at this size — eleven headings across five
  surfaces is a judgement, not a measurement.

The enlarged touch targets are also unverified by hand: they were raised to meet the 48 dp
guideline, not from a report that anything was hard to hit.

**The five-theme picker was added after this pass**, and it is the one new surface whose spoken
behaviour has not been thought through on hardware either: five radio rows in a
`selectableGroup()`, each named by its theme, with a decorative swatch that is deliberately
silent. The *rendering* is covered — `criterion12c` asserts each row's label and selection, and
`assertEveryControlIsLabelled()` asserts no row is unlabelled — but whether "Light Monochrome,
radio button, selected" is what is actually heard, and whether five rows is the right shape for
this control, is unjudged.

**The screen reader's input is also blocked.** Turning TalkBack on is the human's step, but the
*output* half cannot be inspected either: `adb shell uiautomator dump` fails on this ROM with
`ERROR: null root node returned by UiTestAutomationBridge`, the same class of limitation as the
refused `input tap`. So the semantics cannot be read off the live tree the way the widget's
provider could be read off `dumpsys appwidget` — Step 2 is by ear or not at all. The exact
announcements to listen for, control by control, are listed in
[HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 15.12 OPEN — the widget does not follow the chosen theme

The home-screen widget resolves its own colours from the **device's** light/dark setting
([section 5.11](#511-home-screen-widget)), and the mirror deliberately does not redraw on a theme
change — a home screen is not the app's screen. That was invisible while the app had exactly two
palettes whose light/dark split matched the device's: "the widget follows the device" and "the
widget follows the app" produced the same card most of the time.

Five themes end that coincidence. Choosing Warm Sepia or Cool Slate or High Contrast now leaves a
neutral grey card on the home screen next to a warm, cool or black-and-white app, and the card can
disagree with the app in *both* directions at once (a dark device with Light Monochrome chosen).

Three options, none chosen yet — the question was put to the user alongside the five-theme
replacement and this half was left unanswered, so the existing behaviour was kept:

1. **Keep it as it is.** The widget is a glanceable card, not a themed surface; a launcher may
   even look more consistent if the card matches the launcher's own light/dark mode. Cost:
   nothing to build, one documented inconsistency, and the Settings hint says so on screen.
2. **Give the widget five palettes too.** The colours already resolve through a token interface,
   so this means resolving from the *stored* theme rather than the device configuration, and
   redrawing on a theme change (which would also need the mirror to stop ignoring field 7). Cost:
   a redraw on every theme change, and a card that can clash with a launcher in the opposite mode
   — pure black on a white home screen.
3. **Give the widget only the theme's *darkness*.** Resolve the light/dark decision from the
   chosen theme's `isDark` but keep the neutral two-colour card. Cost: small, and it removes the
   "warm app, grey card" oddity without putting five palettes on a home screen; but it still
   mismatches the device when the two disagree.

This needs a decision before the widget's colours are considered finished; it is recorded here
rather than acted on because the user's answer covered the *app's* themes and not the widget's.

---

## 16. Amendment history

| Date | Change |
| --- | --- |
| 2026-10-17 | Document created. Captures the full Stage 0–3 implementation, the device verification, the four instrumented-test defects, and the open questions. |
| 2026-10-17 | Folded in three user decisions: section 11 wording confirmed; manual `AppContainer` confirmed (Hilt rejected); instrumented/Espresso tests deprioritised. Replaced the failing `WorldClockEndToEndTest` with the two-test `WorldClockDeviceSmokeTest`, trimmed the unused `androidTest` dependencies, and marked §15.1–§15.6 accordingly. |
| 2026-10-17 | Phase 2, stage 1 — dark theme. Added the dark palette and the token table, `ThemeMode` and its schema field, the settings sheet and gear action, `resolveDarkTheme` wiring, `values-night` launch theme, `SystemBarAppearance`, acceptance criteria 12a–12c, 5 new dark/settings screenshots, and §15.7/§15.8. Suite grew from 107 to 138 tests. |
| 2026-10-18 | Phase 2, stage 2 — time difference and city labels. Added §5.8, §5.9, §8.1 `city_labels = 6`, §8.2 `setCityLabel`, criteria 13a–13d and 14a–14c, §14.6, and the a11y row for context-spoken values. `CityCard` rebuilt as three independent lines after the shared grid truncated `Tokyo (Head Office)`; the label dialog is a modal overlay because a text field in a dialog window cannot be tested on the JVM. Suite grew from 138 to 165 tests, screenshots from 17 to 21. |
| 2026-10-18 | Phase 2, stage 3 — meeting planner. Added §5.10, §5.3's `PLAN A MEETING` action, §5.6's planner row, the §7.2 typography table, four a11y rows, criteria 15a–15d with the grid contract, §14.7, §15.9, and three planner screenshots. The working day (09:00–18:00, the device's own zone, 15-minute slots ending at 23:45) is stated on screen rather than applied silently. The slider is hand-drawn to keep Material's default track out of the app and to keep the shaded band aligned with the marker. Suite grew from 165 to 199 tests, screenshots from 21 to 24. |
| 2026-10-18 | Phase 2, stage 4 — home-screen widget. Added §5.11, §3.2/§3.3 (Glance and its test harness), the widget rows of §4, criteria 16a–16d, §14.8, §15.10, and the `ui/WidgetUiState.kt` · `ui/mapping/WidgetStateMapper.kt` · `ui/widget/*` · `widget/*` files. Documented the three judgement calls with their rejected alternatives: a fixed six-row budget instead of a measured one, `AlarmManager.setAndAllowWhileIdle` on the minute boundary instead of WorkManager, and the device's light/dark setting instead of the app's stored choice. The widget adds no screenshot — Glance draws through `RemoteViews` — and is asserted through `runGlanceAppWidgetUnitTest` instead. Suite grew from 199 to 237 tests in 24 suites; the APK grew from 11.37 MB to about 13.0 MB. |
| 2026-10-18 | Phase 2, stage 5 — integration, accessibility and polish. Added §5.12, `MinTouchTarget`, three acceptance criteria (17a–17c), three navigation tests in `WorldClockNavigationTest`, §14.9, §15.11 and four 12-hour/dark renders. Every control added in phase 2 is now at least 48 dp tall (the 44 dp circular buttons carry their interaction on a 48 dp box, the segmented control's options are padded out to 48 dp); screen titles and section headers carry `heading()`; the label dialog sets `paneTitle` and names its field after the city. Recorded two test-harness findings: injected touches do not cross into a `ModalBottomSheet`, and Robolectric's stub font measures text at close to zero width, so width assertions are only meaningful for controls the app sizes itself. A clean `clean assembleDebug testDebugUnitTest --no-build-cache` ends at **247 tests, 0 failures, 24 suites, 28 screenshots**. |
| 2026-10-18 | Human-verification prep. Added `HUMAN_VERIFICATION.md` — the worksheet for the two things the JVM cannot do (placing the widget, hearing TalkBack) with a control-by-control list of expected announcements, the exact `adb` probes, and a findings table. Machine-side: the provider is now verified registered on the device (`dumpsys appwidget`), and the build that was installed turned out to be **pre-widget** (2026-10-17 23:10), so the current APK was installed over it. `uiautomator dump` is blocked by this ROM as well, so the live semantics tree cannot be read — recorded in §15.11. |
| 2026-10-18 | Phase 2, stage 9 — the five themes. Replaced the `System / Light / Dark` mode with five palettes (Light Monochrome, True Dark, Warm Sepia, Cool Slate, High Contrast) per the user's decision, so device-following is gone: `ThemeMode`/`resolveDarkTheme` deleted for `AppTheme`/`paletteFor(theme)`, the proto gained `SavedAppTheme` + `optional theme = 7` with read-time migration of the legacy field, and the Settings segmented control became five ≥ 48 dp radio rows with decorative swatches. Added the `mapLandmass` token (`accent` in the four soft themes, white in High Contrast) because a black night wash cannot shade High Contrast's black ocean — the terminator lives on the light continents instead. Rewrote §7.1 as a five-column token table, and added §15.12 (the widget still follows the device, three options, no decision yet). A clean `clean assembleDebug testDebugUnitTest --no-build-cache` ends at **264 tests, 0 failures, 24 suites, 35 screenshots**; `WorldClockPaletteTest` now runs every contrast, inversion and map check over all five themes. |
