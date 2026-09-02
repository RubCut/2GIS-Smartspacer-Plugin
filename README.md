**English** · [Русский](README_RU.md)

<div align="center">

<img src="docs/icon.svg" width="112" alt="2GIS ETA Smartspacer icon">

# 2GIS ETA for Smartspacer

**Travel time to your chosen destination — right inside Smartspacer**

[![Build APK](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml/badge.svg)](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)

</div>

The plugin adds three Complication providers to [Smartspacer](https://github.com/KieronQuinn/Smartspacer)
that show ETA via the 2GIS API:

- 🚗 by car;
- 🚶 on foot;
- 🚌 on public transport.

All three modes share the same destination. It can be looked up by address through
the Geocoder API, or set manually as latitude and longitude.

## Features

- Modern Material 3 settings screen;
- Dynamic colors on Android 12+;
- Light and dark themes;
- English and Russian UI;
- Per-Complication settings: each Complication keeps its own destination and
  API key;
- Synced default API key: a newly added Complication is pre-filled with the
  last key you saved, and a new key entered in any Complication becomes the
  default for future ones;
- Destination by address or coordinates;
- "Set up" hint right in the Complication before the first run;
- Complication stays visible on a temporary error and shows `No ETA`;
- Quick jump to the 2GIS map and Platform Manager;
- Immediate ETA calculation after saving;
- Automatic refresh every 15 minutes;
- Parallel calculation of driving, walking, and transit routes;
- No continuous location tracking.

## Requirements

- Android 10 or newer;
- Smartspacer installed;
- A 2GIS API key with access to:
  - [Routing API](https://docs.2gis.com/api/navigation/routing/overview);
  - Public Transport API;
  - [Geocoder API](https://docs.2gis.com/api/search/geocoder/overview).

> The 2GIS API may be paid and may have request rate limits. Check the current
> pricing and quotas in Platform Manager before use.

## Getting an API key

1. Open [2GIS Platform Manager](https://platform.2gis.ru/).
2. Create a project and a key.
3. Enable Routing API, Public Transport API, and Geocoder API.
4. Paste the key into the plugin settings.

Links to Platform Manager and the docs are also available directly from the
settings screen.

## Installation

### Prebuilt debug build

1. Open the latest successful run in
   [Actions](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml).
2. Download the `gis2smartspacer-debug-apk` artifact.
3. Unpack the archive and install the APK on your phone.

### Building from sources

Requires JDK 17, Android SDK 35, and Gradle 8.9.

```bash
git clone https://github.com/RubCut/2GIS-Smartspacer-Plugin.git
cd 2GIS-Smartspacer-Plugin
gradle assembleDebug
```

The APK will be in:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

1. In Smartspacer, add one or more Complications of this plugin:
   **Driving**, **Walking**, or **Transit**.
2. Open **More settings** on the added Complication.
3. Paste your 2GIS API key. The key is synced between Complications: a newly
   added Complication is pre-filled with the last key you saved, while a new
   key entered in any Complication overwrites that default. Complications that
   were already configured keep their own key.
4. Choose how to specify the destination:
   - **by address** — coordinates will be resolved by the Geocoder API;
   - **coordinates** — find the place in 2GIS and enter latitude and longitude.
5. Grant location access.
6. Tap **"Save and refresh ETA"**.

For background updates Android must grant the app location access set to
**"Allow all the time"**.

The plugin intentionally has no launcher icon: settings open from Smartspacer
through **More settings**.

## How refresh works

```text
Smartspacer
    │ periodic request
    ▼
EtaComplicationUpdateReceiver
    │ last known location
    ▼
2GIS Routing API
    │ ETA for requested modes
    ▼
SharedPreferences → Complication
```

`getSmartspaceActions()` does not perform network calls. The Complication
providers only read the latest cached result, so Smartspacer gets its response
fast. Network calls happen in the background receiver and run in parallel to
shorten update time.

## Location and privacy

- the plugin does not start continuous location tracking;
- saving requests a single fresh fix with a timeout;
- background updates reuse the last known Android location;
- the API key, address, coordinates, and ETA cache are stored locally in
  `SharedPreferences` — per Complication instance, plus a synced default key;
- location and destination are sent only to the 2GIS API for route calculation.

## Project structure

```text
app/src/main/java/com/rubcut/gis2smartspacer/
├── SettingsActivity.kt                 # settings screen
├── SettingsRepository.kt               # local settings and cache
├── LocationHelper.kt                   # location retrieval
├── TwoGisClient.kt                     # Geocoder and Routing API
├── EtaUpdater.kt                       # parallel ETA refresh
├── EtaComplicationUpdateReceiver.kt    # Smartspacer update requests
└── complications/
    ├── BaseEtaComplication.kt
    ├── CarEtaComplication.kt
    ├── WalkEtaComplication.kt
    └── TransitEtaComplication.kt
```

## Limitations

- ETA accuracy depends on how fresh your location is and on 2GIS data;
- before configuration the Complication shows `Set up`, and when no route is
  available — `No ETA`; tapping opens the settings screen;
- the Basic Complication text is limited to 12 characters, so a short format is
  used: `23 min`, `1 h 5 m`;
- three installed Complications may issue up to three API calls every 15 minutes
  — keep that in mind when choosing a plan.

## Disclaimer

This project is not an official product of 2GIS or Smartspacer. Names and
trademarks belong to their respective owners. By using the 2GIS API you agree
to the current terms of service and the limits of your plan.
