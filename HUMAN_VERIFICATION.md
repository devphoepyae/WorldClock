# Human verification worksheet — home-screen widget + TalkBack

Two things the JVM suite cannot do: **place a widget on a real home screen** and **hear
TalkBack**. Everything else about this build is already green (see
[SPECIFICATION.md](SPECIFICATION.md) §12.2: 264 tests, 0 failures, 24 suites, 35 screenshots).

What is open, and why, is recorded in
[§15.10](SPECIFICATION.md#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen),
[§15.11](SPECIFICATION.md#1511-open--the-accessibility-pass-has-not-been-heard-on-hardware) and
[§15.12](SPECIFICATION.md#1512-open--the-widget-does-not-follow-the-chosen-theme).

| | |
| --- | --- |
| Device | Redmi Turbo 4 (`24129RT7CC`, HyperOS, Android 16) |
| Device zone | `Asia/Yangon` — **UTC+6:30** |
| Build under test | `app/build/outputs/apk/debug/app-debug.apk` (13.0 MB, `versionName 1.0`) |
| Installed | 2026-10-18 22:40, replacing the pre-widget build (see §0). This is the **five-theme** build |
| Version note | This is a debug build. It is the same code the tests ran against. |

> **Before starting:** close the floating windows on the home screen. A video picture-in-picture
> bubble and the Wi-Fi/Bluetooth panel were covering the right-hand side of the app during the
> machine check, which is exactly where the times are.

---

## 0. Machine checks — already done, 2026-10-18

| Check | Command | Result |
| --- | --- | --- |
| Device reachable | `adb devices -l` | ✅ `IVQSS8HYWCCIM7EQ` |
| App installed | `adb install -r app-debug.apk` | ✅ `Success` — **replaced a build that had no widget at all** (the installed APK was from 2026-10-17 23:10, before the widget existed) |
| Widget provider registered with the system | `adb shell dumpsys appwidget` | ✅ `com.worldclock.app/com.worldclock.app.widget.WorldClockWidgetReceiver`, `updatePeriodMillis=1800000`, `resizeMode=3`, `widgetCategory=1` |
| App launches | `adb shell am start -n com.worldclock.app/.MainActivity` | ✅ Clocks screen rendering live times (see `worldclock-device\wc_live_clocks.png` in the temp folder) |
| Saved data intact | screenshot | ✅ 4 cities: **Tokyo, Los Angeles, London, Auckland** (no data was wiped) |
| TalkBack available | `adb shell pm list packages` | ✅ `com.google.android.marvin.talkback` installed, **currently OFF** |
| Accessibility tree dump | `adb shell uiautomator dump` | ❌ `ERROR: null root node returned by UiTestAutomationBridge` — this ROM blocks it, the same family of limitation as `input tap`/`input keyevent`. **Step 2 can only be done by ear.** |
| Five-theme build installed and running | `adb install -r app-debug.apk` → `am start` → screenshot | ✅ `Success`; `lastUpdateTime=2026-10-18 22:40:08`; the Clocks screen renders live times (see `worldclock-device\wc_five_themes_clocks.png`) |
| Widget provider still registered after the upgrade | `adb shell dumpsys appwidget` | ✅ `WorldClockWidgetReceiver` still listed |
| Stored theme migrated without disturbing anything | screenshot | ✅ the app came up on **Light Monochrome** (the default) with the saved cities intact and their labels, so nothing was rewritten or lost by the schema change |

**Live sanity of the real device data** (from the current screenshot at device time 22:40 Sun 18 Oct):
the list now holds **8 cities** — the visible ones are `Tokyo (Village)` (UTC+9), Los Angeles
(UTC−7), London (UTC+1), Auckland (UTC+13, showing `Mon`) and the active city is **Yangon**
(UTC+6:30, `Same time`), because the device's own zone is `Asia/Yangon`. The offsets are the
*summer* ones, which confirms DST is being computed from `ZoneRules` and not hard-coded, and the
`(Village)` label confirms the city-label feature is carrying real user data through the schema
change.

---

## Step 1 — the widget on the real home screen

Long-press an empty area of the home screen → **Widgets** → find **WorldClock** → drag it out.

### 1a. It appears at all

- [ ] **WorldClock** is listed in the widget picker (no `dumpsys` can prove this; only the launcher can)
- [ ] It can be placed, and the preview shown while dragging looks like a card, not a blank box
- [ ] Default size is about **4×4 cells**
- [ ] It can be **resized** both wider and taller

### 1b. The content matches the app

Expected shape (the app now holds **8 cities**, more than the 6-row budget, so the last two rows
become the count). The exact rows depend on the saved order, and the values below are a snapshot
at device time 22:40 Sun — the header, the ruler and the order are what is fixed:

```
WORLD TIME                              10:40 PM
──────────────────────────────────────────────
☾  Tokyo                                   1:10 AM
☾  Auckland                        Mon      5:10 AM
☀  London                                  5:10 PM
☀  Los Angeles                             9:10 AM
…  (the rest, in the app's order)               
                                           +2 more
```

- [ ] The header shows `WORLD TIME` and the **device's own** time (muted, right-aligned)
- [ ] One row per saved city, in the app's order, up to the 6-row budget — then `+2 more` for the
      two that do not fit, since the device has 8 cities. **Never silently dropped**
- [ ] `+N more` appears and the count is right (it should read `+2 more` for 8 cities)
- [ ] Each row's time **equals the card for the same city in the app** (compare side by side; this is
      the real invariant, so the values above are only a snapshot)
- [ ] **A weekday appears only where the city's date differs from the device's date** — at 22:40
      device time that is Auckland (`Mon`) and Tokyo (already `Sun`), and not London or Los Angeles.
      The rule is the date, not the hour, so the set changes through the evening
- [ ] **Sun/moon matches each city's own hour**: Tokyo (01:10) and Auckland (05:10) → moon; London
      (17:10) and Los Angeles (09:10) → sun
- [ ] A city with a **custom label** shows it — the user's own `Tokyo (Village)` is already one, so
      this is visible without adding anything
- [ ] Switch the app to **24h** and the widget follows (times become `01:10` style)

### 1c. Tapping

- [ ] Tapping anywhere on the card opens the app on the **Clocks** screen

### 1d. Refresh

- [ ] **Minute ticks by itself.** Leave the phone on the home screen with the app closed and watch for 2–3 minutes: the minutes must change at the same moment the status-bar clock does
- [ ] **Immediate redraw on a data change.** With the app open, add a city / remove a city / rename a label / switch 12h–24h: the widget updates within a second or two (this is `WidgetMirror`, not the alarm)
- [ ] **Survives a reboot.** Reboot, wait, and the widget is still there and correct (this is the 30-minute `updatePeriodMillis` backstop)
- [ ] **Deep doze is allowed to lag.** Lock the phone, leave it 20–30 minutes untouched, then look: a lag of up to ~9 minutes is the documented `setAndAllowWhileIdle` trade-off (§5.11), not a defect. Record how long it actually lagged

### 1e. Theme behaviour (deliberate, worth confirming)

The widget follows the **device's** light/dark setting, **not** the app's stored theme.

- [ ] Flip the **device** dark theme → the widget follows
- [ ] Flip the **app** theme (gear → Theme → any of the five) → the widget **does not** change

That divergence is the documented decision, not a bug — but it is now **re-opened** as a real
question, because the app has five palettes and only two of them match the device's light/dark
split. Choosing **Warm Sepia**, **Cool Slate** or **High Contrast** leaves a neutral grey card
next to a warm / cool / black-and-white app. Three options are written up in
`SPECIFICATION.md` §15.12; nothing has been decided.

- [ ] Look at a **Warm Sepia** or **High Contrast** app next to its widget and judge: is the grey
      card acceptable, does it look like a bug, or would you rather the card matched the theme?
      (This is the answer §15.12 needs.)

---

## Step 2 — TalkBack

Settings → Accessibility → **TalkBack** → On. Then open WorldClock and keep your eyes off the
screen as much as possible.

### 2a. The Clocks screen

| Control | Expected announcement |
| --- | --- |
| Title | **"World Time"** — must be reachable with **heading** navigation |
| Section label | **"YOUR CITIES"** — also a heading |
| Gear | "Settings, button" |
| Plus | "Add city, button" |
| `PLAN A MEETING` row | "Plan a meeting across time zones, button" — the *purpose*, not just the visible words |
| City card (Tokyo) | One swipe, not seven: the city, its region, its time, its day/night state, and the difference **as the words** "Compared with your local time: … ahead" — a bare "+2 hrs 30 mins ahead" means the prefix is being lost |
| Bottom tabs | Clocks / Search / Map, with the active one announced as selected |

- [ ] Headings reachable (TalkBack: change the reading control to *Headings*, then swipe up/down)
- [ ] The card is a **single** merged item
- [ ] The difference is announced with its "Compared with your local time" prefix
- [ ] Day/night is announced as "Daytime" / "Nighttime"

### 2b. Search, detail, settings

| Control | Expected announcement |
| --- | --- |
| Search field | the placeholder, and typing is spoken |
| Close / clear | "Close", "Clear search" |
| A result | "Add Tokyo, button" — or "Tokyo is already added" for one already saved |
| Detail back | "Back" |
| 12h / 24h control | two radio buttons; the active one announced as **selected** |
| `EDIT LABEL` | "Edit label for Tokyo, button" |
| Detail headings | "COMPARED WITH YOUR LOCAL TIME", "YOUR LOCAL TIME" |
| Settings sheet | "Settings" heading, then "APPEARANCE" heading, then "Theme", then **five** radio rows — Light Monochrome / True Dark / Warm Sepia / Cool Slate / High Contrast — each announced as a radio button with the one in force announced as **selected** |
| A theme row | the theme's name, "radio button", and — for the one in force — "selected". The little colour swatch beside the name must be **silent**; if TalkBack stops on it, say so |

- [ ] A result already saved is announced as already added, not as an add action

### 2c. Map and planner

| Control | Expected announcement |
| --- | --- |
| Map canvas | "Schematic world map. UTC offset ruler." |
| A marker | "Tokyo, 11:48 PM, Nighttime" — and "…, selected city" for the active one |
| Planner slider | "Meeting time", swipe up/down to change it, and the value spoken **as a time**, never as a percentage |
| Planner row | "Meeting time in Tokyo: 11:48 PM, within working hours" (or "…outside working hours") |

- [ ] The slider can be adjusted with a swipe and never says "50 percent"

### 2d. The three things this pass added — listen for these specifically

These are new, asserted only as semantics on the JVM, and never heard:

- [ ] **Opening the label dialog announces the pane**, i.e. "City label", before you reach the field (this is `paneTitle`)
- [ ] **The label field is named for the city** — "Label for Tokyo" — not just "edit box" (this is `a11y_city_label_field`)
- [ ] **The headings help rather than nag.** Thirteen headings across five surfaces is a judgement: if heading navigation is noisy, say so and it comes down

### 2e. Touch targets (with TalkBack off)

- [ ] The 44 dp circles (gear, plus, back, map zoom) feel comfortably hittable — they now carry their click on a **48 dp** box
- [ ] The **five theme rows** are easy to hit and the right shape for the job — they are ~48 dp tall full-width rows, replacing the three ~36 dp segments
- [ ] Switching through all five themes by eye: do the swatches match what you get when you tap them, is any theme hard to read, and does **High Contrast**'s map still make the night half obvious? (High Contrast draws white continents, because a black night wash cannot shade a black ocean)
- [ ] Nothing feels *oversized* as a result — if the theme list now makes the sheet chunky, that is a visual judgement to make now, while it is cheap to change

---

## Step 3 — Findings

Fill this in as you go. Anything that is a defect gets fixed; anything that is a judgement gets a
decision; anything unverifiable gets left OPEN in §15.10/§15.11.

| # | Step | What I saw | Type (defect / judgement / expected) | Severity |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

**Already suspected — confirm or clear these first:**

| # | Suspect | What to look for |
| --- | --- | --- |
| S1 | The widget is drawn in the platform font, not Inter, and its figures are **not tabular**, because `RemoteViews` cannot carry a bundled font | Watch for the minute column changing width as digits change (e.g. `11:48` → `11:49`), where the app's own rows sit still |
| S2 | The 4×4 footprint is a *request*; launchers resolve cells differently | Whether the card looks cramped or has a large empty area at its default size |
| S3 | The minute alarm is inexact and throttled in deep doze | How far behind the widget gets after a long idle (up to ~9 minutes is expected) |
| S4 | The widget follows the device theme while the app can be pinned to any of five | Now the widest gap in the app: app = Warm Sepia or High Contrast, device = Light or Dark, gives a neutral grey card beside a warm or black-and-white app. Judge it — see §15.12 |
| S5 | `paneTitle` on an **in-composition** overlay may not be announced the way a real window's pane title is | Listening for "City label" when the dialog opens — silence here is a real finding, not a test artefact |
| S6 | Enlarged touch targets are unverified by hand and were raised to meet a guideline, not from a complaint | Whether anything now feels too big or too spaced out |
| S7 | The five themes have been judged by a test for contrast and distinctness, never by an eye for taste | Whether Warm Sepia, Cool Slate or High Contrast reads as a designed theme or as a recoloured one — the derived greys are the assistant's choices (§15.8) |
| S8 | The theme is read from DataStore asynchronously, so a cold start can paint one frame of Light Monochrome before the stored theme arrives | Force-stop and reopen with True Dark or High Contrast chosen on a light device (and the reverse): is there a visible flash? (§15.7) |
| S9 | Five theme rows push the settings sheet taller than the three segments did | Whether the sheet still fits comfortably, and whether it covers too much of the screen behind it |

---

## Re-running the machine checks

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Is the widget provider still registered, and what does the launcher think its size is?
& $adb shell dumpsys appwidget | Select-String -Pattern "worldclock" -Context 1,3

# Is a widget currently placed on a home screen?
& $adb shell dumpsys appwidget | Select-String -Pattern "Widgets:" -Context 0,5

# Reinstall this build (keeps saved cities)
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"

# Launch, then screenshot
& $adb shell am start -n com.worldclock.app/.MainActivity
& $adb shell screencap -p /sdcard/wc.png
& $adb pull /sdcard/wc.png "$env:TEMP\opencode\worldclock-device\wc_live.png"
```

### TalkBack without touching the settings UI (optional, and not done for you)

`adb shell` can flip it, but TalkBack changes how the whole phone is used, so it is left to you:

```powershell
# ON
& $adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
& $adb shell settings put secure accessibility_enabled 1

# BACK OFF  (restores the service that was enabled before: com.milink.service/...MLAcessibilityService)
& $adb shell settings put secure enabled_accessibility_services com.milink.service/com.miui.circulate.world.MLAccessibilityService
& $adb shell settings put secure accessibility_enabled 1
```

---

## What happens to this file

Findings go back into [SPECIFICATION.md](SPECIFICATION.md):

- Something broken → fixed, and a criterion or a row in §9 added so it cannot come back silently.
- Something disagreeable → a decision record in §5.11 or §5.12, with what was rejected.
- Something still unverifiable → stays OPEN in §15.10/§15.11, with the reason.
