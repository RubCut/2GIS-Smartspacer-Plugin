# AGENTS.md — 2GIS Smartspacer Plugin

## Project architecture

- **Smartspacer plugin** for Android exposing three ETA **Complications** that show 2GIS travel times on the Smartspacer home/lock screen surface.
- Core modules:
  - `TwoGisClient` — REST client for 2GIS Routing API.
  - `EtaUpdater` — orchestrates fetches in parallel; called from the `BroadcastReceiver`.
  - `EtaComplicationUpdateReceiver` — `BroadcastReceiver` entry point invoked by Smartspacer.
  - `BaseEtaComplication` — base class shared by all three Complications (car / walk / bus).
  - `LocationHelper` — provides device location or falls back to manual coordinates.
  - `SettingsRepository` — SharedPreferences-backed settings: the synced default
    API key (`defaultApiKey`) plus per-Complication-instance storage
    (`ComplicationSettings`, keyed by the Smartspacer `smartspacerId`).
  - `Constants` — pref keys, endpoint authorities, update intervals.

## 2GIS API logic

- Endpoint: 2GIS Routing API. Transport type is selected per Complication (car, walk, bus).
- Both endpoints nest `duration` differently, so the parser must traverse either response shape.
- Parallel requests for each transport are kept independent to stay within the `BroadcastReceiver` time limit.
- Each Complication instance has its own API key (`ComplicationSettings.apiKey`),
  passed via query parameter.

## API key synchronization

- Every Complication instance (identified by Smartspacer's `smartspacerId`)
  stores its own key, destination, and ETA cache under `inst_<id>_*` prefs keys.
- `SettingsRepository.defaultApiKey` is the synced default: the settings screen
  pre-fills the key field with it for instances that have no key of their own,
  so a newly added Complication is ready to go.
- Saving a key that differs from the instance's previous key overwrites
  `defaultApiKey` — that key becomes the default for future Complications.
  Existing instances keep their own stored key.
- Migration from versions <= 2.4 (shared settings): on first access an instance
  snapshots the legacy shared blob (`api_key_2gis`, destination, ETA cache), so
  already added Complications keep working. The legacy blob is deleted after
  the first save, so instances created later start fresh (only the key is
  pre-filled from the default).
- `SettingsActivity` receives `SmartspacerConstants.EXTRA_SMARTSPACER_ID` and
  `EXTRA_AUTHORITY` from Smartspacer (both on add and on "More settings") and
  edits exactly that instance; without extras it falls back to
  `Constants.FALLBACK_COMPLICATION_ID`.
- `onProviderRemoved(smartspacerId)` deletes the instance's stored settings
  (`SettingsRepository.clearComplication`).

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
- 2GIS API key is entered per Complication in the settings screen; the synced
  default lives in SharedPreferences (no key is hardcoded).

## Unfinished / pending actions

- (TBD — to be filled in by the next session as work progresses.)

## Do not delete

- **Do not delete the GitHub Actions workflows** in `.github/` without an explicit instruction from the maintainer.
