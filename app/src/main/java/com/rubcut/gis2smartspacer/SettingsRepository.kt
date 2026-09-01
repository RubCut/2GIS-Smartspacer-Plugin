package com.rubcut.gis2smartspacer

import android.content.Context
import android.content.SharedPreferences

/**
 * Простая обёртка над SharedPreferences.
 * Все чтения тут синхронные и быстрые, поэтому их можно безопасно
 * дёргать прямо из getSmartspaceActions() у Complication-провайдеров.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_API_KEY, value).apply()

    var destAddress: String
        get() = prefs.getString(Constants.KEY_DEST_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_DEST_ADDRESS, value).apply()

    var destLat: Double
        get() = prefs.getFloat(Constants.KEY_DEST_LAT, 0f).toDouble()
        set(value) = prefs.edit().putFloat(Constants.KEY_DEST_LAT, value.toFloat()).apply()

    var destLon: Double
        get() = prefs.getFloat(Constants.KEY_DEST_LON, 0f).toDouble()
        set(value) = prefs.edit().putFloat(Constants.KEY_DEST_LON, value.toFloat()).apply()

    val hasDestination: Boolean
        get() = destLat != 0.0 && destLon != 0.0

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && hasDestination

    fun setEtaMinutes(mode: TravelMode, minutes: Int?) {
        val key = when (mode) {
            TravelMode.DRIVING -> Constants.KEY_CAR_MINUTES
            TravelMode.WALKING -> Constants.KEY_WALK_MINUTES
            TravelMode.TRANSIT -> Constants.KEY_TRANSIT_MINUTES
        }
        val editor = prefs.edit()
        if (minutes == null) {
            editor.remove(key)
        } else {
            editor.putInt(key, minutes)
        }
        editor.putLong(Constants.KEY_LAST_UPDATE_TS, System.currentTimeMillis())
        editor.apply()
    }

    fun getEtaMinutes(mode: TravelMode): Int? {
        val key = when (mode) {
            TravelMode.DRIVING -> Constants.KEY_CAR_MINUTES
            TravelMode.WALKING -> Constants.KEY_WALK_MINUTES
            TravelMode.TRANSIT -> Constants.KEY_TRANSIT_MINUTES
        }
        return if (prefs.contains(key)) prefs.getInt(key, -1).takeIf { it >= 0 } else null
    }

    val lastUpdateTimestamp: Long
        get() = prefs.getLong(Constants.KEY_LAST_UPDATE_TS, 0L)
}
