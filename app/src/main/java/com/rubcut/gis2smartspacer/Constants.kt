package com.rubcut.gis2smartspacer

object Constants {

    const val PREFS_NAME = "eta_settings"

    const val KEY_API_KEY = "api_key_2gis"
    const val KEY_DEST_ADDRESS = "dest_address"
    const val KEY_DEST_LAT = "dest_lat"
    const val KEY_DEST_LON = "dest_lon"
    const val KEY_MANUAL_COORDINATES = "manual_coordinates"

    const val KEY_CAR_MINUTES = "car_minutes"
    const val KEY_WALK_MINUTES = "walk_minutes"
    const val KEY_TRANSIT_MINUTES = "transit_minutes"
    const val KEY_LAST_UPDATE_TS = "last_update_ts"

    const val AUTHORITY_CAR = "com.rubcut.gis2smartspacer.complication.car"
    const val AUTHORITY_WALK = "com.rubcut.gis2smartspacer.complication.walk"
    const val AUTHORITY_TRANSIT = "com.rubcut.gis2smartspacer.complication.transit"

    const val REFRESH_PERIOD_MINUTES = 15
}

enum class TravelMode {
    DRIVING,
    WALKING,
    TRANSIT
}
