# AGENTS.md — 2GIS Smartspacer Plugin

## Project architecture

- **Smartspacer plugin** for Android exposing three ETA **Complications** that show 2GIS travel times on the Smartspacer home/lock screen surface.
- Core modules:
  - `TwoGisClient` — REST client for 2GIS Routing API.
  - `EtaUpdater` — orchestrates fetches in parallel; called from the `BroadcastReceiver`.
  - `EtaComplicationUpdateReceiver` — `BroadcastReceiver` entry point invoked by Smartspacer.
  - `BaseEtaComplication` — base class shared by all three Complications (car / walk / bus).
  - `LocationHelper` — provides device location or falls back to manual coordinates.
  - `SettingsRepository` — DataStore-backed user settings (manual coords, selected transport, etc.).
  - `Constants` — API keys, endpoint paths, update intervals.

## 2GIS API logic

- Endpoint: 2GIS Routing API. Transport type is selected per Complication (car, walk, bus).
- Both endpoints nest `duration` differently, so the parser must traverse either response shape.
- Parallel requests for each transport are kept independent to stay within the `BroadcastReceiver` time limit.
- API key is read from `Constants` and passed via query parameter.

## Three Complications

1. **Car ETA** — `BaseEtaComplication` configured for `car`.
2. **Walk ETA** — same base, transport `walk`.
3. **Bus ETA** — same base, transport `bus`.

Each one renders the ETA on the Smartspacer surface and triggers a tap action that opens the 2GIS app.

## Manual coordinates mode

- Toggled in Settings. When enabled, `LocationHelper` returns user-entered coordinates instead of GPS.
- Coordinates stored as `Float` since version 1; preserve that on read.
- Coordinates are validated as a valid lat/lng range before use.

## Placeholder states

- **Set up** — shown when the user has not configured the API key or location yet.
- **No ETA** — shown when the API returns no route (e.g. unreachable destination).

## Build requirements

- Android Gradle Plugin compatible with Kotlin 1.9+.
- `compileSdk` / `targetSdk` per `app/build.gradle.kts`.
- Min SDK as declared in the manifest.
- 2GIS API key (set in `Constants.kt` or as a `BuildConfig` field, depending on configuration).

## Unfinished / pending actions

- (TBD — to be filled in by the next session as work progresses.)

## Do not delete

- **Do not delete the GitHub Actions workflows** in `.github/` without an explicit instruction from the maintainer.
