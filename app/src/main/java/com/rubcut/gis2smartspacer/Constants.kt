package com.rubcut.gis2smartspacer

object Constants {

    const val PREFS_NAME = "eta_settings"

    // Legacy settings shared by all Complications in versions up to 2.4.
    // They are snapshotted into per-instance storage on first access and are
    // only kept as a migration fallback afterwards.
    const val KEY_API_KEY = "api_key_2gis"
    const val KEY_DEST_ADDRESS = "dest_address"
    const val KEY_DEST_LAT = "dest_lat"
    const val KEY_DEST_LON = "dest_lon"
    const val KEY_MANUAL_COORDINATES = "manual_coordinates"

    // Synced default API key: newly added Complications are pre-filled with it,
    // and a new key saved in any Complication overwrites it.
    const val KEY_DEFAULT_API_KEY = "default_api_key"

    // Per-Complication-instance storage lives in the same prefs file under
    // "inst_<smartspacerId>_<field>" keys, see ComplicationSettings.
    const val INSTANCE_PREFIX = "inst_"

    // Used when the settings screen is opened without the Smartspacer extras
    // (e.g. from a cached tap action created by an older plugin version).
    const val FALLBACK_COMPLICATION_ID = "local_instance"

    const val KEY_CAR_MINUTES = "car_minutes"
    const val KEY_WALK_MINUTES = "walk_minutes"
    const val KEY_TRANSIT_MINUTES = "transit_minutes"
    const val KEY_LAST_UPDATE_TS = "last_update_ts"

    const val AUTHORITY_CAR = "com.rubcut.gis2smartspacer.complication.car"
    const val AUTHORITY_WALK = "com.rubcut.gis2smartspacer.complication.walk"
    const val AUTHORITY_TRANSIT = "com.rubcut.gis2smartspacer.complication.transit"

    const val REFRESH_PERIOD_MINUTES = 15

    fun authorityForMode(mode: TravelMode): String = when (mode) {
        TravelMode.DRIVING -> AUTHORITY_CAR
        TravelMode.WALKING -> AUTHORITY_WALK
        TravelMode.TRANSIT -> AUTHORITY_TRANSIT
    }

    fun modeForAuthority(authority: String): TravelMode? = when (authority) {
        AUTHORITY_CAR -> TravelMode.DRIVING
        AUTHORITY_WALK -> TravelMode.WALKING
        AUTHORITY_TRANSIT -> TravelMode.TRANSIT
        else -> null
    }
}

enum class TravelMode {
    DRIVING,
    WALKING,
    TRANSIT
}
