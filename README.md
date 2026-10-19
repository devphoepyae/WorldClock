# 🌍 WorldClock

A minimalist, lightweight, and offline-first World Clock Android application built with native Kotlin and Jetpack Compose. Designed for simplicity, performance, and a clean monochrome aesthetic.

🌐 **[Landing Page](https://devphoepyae.github.io/WorldClock/)** · **[Netlify](https://worldclock-android.netlify.app/)** — Features overview and direct APK download.

---

## ✨ Features

- **Offline-First Architecture:** Operates seamlessly without internet dependence using local storage and programmatic time calculations.
- **Real-Time Solar Terminator:** Canvas-rendered schematic world map with live day/night shading.
- **Live Second-Aligned Ticker:** Accurate time updates synced precisely with system time.
- **Flexible Formats:** Instant 12h/24h format toggling.
- **Smart Management:** Built-in search and duplicate guards for city management.
- **Minimalist Design System:** Clean monochrome UI focused on readability and distraction-free experience.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (BOM 2024.09.00)
- **Data Persistence:** Proto DataStore
- **Architecture:** Clean Architecture with Unidirectional Data Flow
- **Time Engine:** `java.time` IANA timezone calculations
- **Min SDK:** 26 (Android 8.0+)
- **Target SDK:** 34 (Android 14)

---

## 📦 Download & Installation

You can download the latest release APK directly from the [Releases](../../releases) page or the [landing page](https://devphoepyae.github.io/WorldClock/) and install it on your Android device.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
