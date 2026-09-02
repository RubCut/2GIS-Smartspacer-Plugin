package com.rubcut.gis2smartspacer

object Constants {

    const val PREFS_NAME = "eta_settings"

    // Настройки, вводимые пользователем
    const val KEY_API_KEY = "api_key_2gis"
    const val KEY_DEST_ADDRESS = "dest_address"
    const val KEY_DEST_LAT = "dest_lat"
    const val KEY_DEST_LON = "dest_lon"
    const val KEY_MANUAL_COORDINATES = "manual_coordinates"

    // Кэш последних результатов (читается синхронно в getSmartspaceActions)
    const val KEY_CAR_MINUTES = "car_minutes"
    const val KEY_WALK_MINUTES = "walk_minutes"
    const val KEY_TRANSIT_MINUTES = "transit_minutes"
    const val KEY_LAST_UPDATE_TS = "last_update_ts"

    // Authorities Complication-провайдеров, должны совпадать с applicationId
    const val AUTHORITY_CAR = "com.rubcut.gis2smartspacer.complication.car"
    const val AUTHORITY_WALK = "com.rubcut.gis2smartspacer.complication.walk"
    const val AUTHORITY_TRANSIT = "com.rubcut.gis2smartspacer.complication.transit"

    // Раз в сколько минут Smartspacer будет присылать broadcast на обновление
    const val REFRESH_PERIOD_MINUTES = 15
}

enum class TravelMode {
    DRIVING,
    WALKING,
    TRANSIT
}
