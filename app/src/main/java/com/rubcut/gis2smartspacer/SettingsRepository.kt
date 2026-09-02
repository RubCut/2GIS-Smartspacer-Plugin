package com.rubcut.gis2smartspacer

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_API_KEY, value).apply()

    var destAddress: String
        get() = prefs.getString(Constants.KEY_DEST_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_DEST_ADDRESS, value).apply()

    val destLat: Double
        get() = readCoordinate(Constants.KEY_DEST_LAT)

    val destLon: Double
        get() = readCoordinate(Constants.KEY_DEST_LON)

    val hasDestination: Boolean
        get() = prefs.contains(Constants.KEY_DEST_LAT) && prefs.contains(Constants.KEY_DEST_LON)

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && hasDestination

    val usesManualCoordinates: Boolean
        get() = prefs.getBoolean(Constants.KEY_MANUAL_COORDINATES, false)

    fun saveDestination(
        apiKey: String,
        address: String,
        point: GeoPoint,
        manualCoordinates: Boolean = false
    ) {
        prefs.edit()
            .putString(Constants.KEY_API_KEY, apiKey)
            .putString(Constants.KEY_DEST_ADDRESS, address)
            .putLong(Constants.KEY_DEST_LAT, point.lat.toBits())
            .putLong(Constants.KEY_DEST_LON, point.lon.toBits())
            .putBoolean(Constants.KEY_MANUAL_COORDINATES, manualCoordinates)
            .remove(Constants.KEY_CAR_MINUTES)
            .remove(Constants.KEY_WALK_MINUTES)
            .remove(Constants.KEY_TRANSIT_MINUTES)
            .remove(Constants.KEY_LAST_UPDATE_TS)
            .apply()
    }

    fun setEtaResults(results: Map<TravelMode, Int?>) {
        val editor = prefs.edit()
        var hasSuccess = false
        results.forEach { (mode, minutes) ->
            val key = etaKey(mode)
            if (minutes == null) {
                editor.remove(key)
            } else {
                editor.putInt(key, minutes)
                hasSuccess = true
            }
        }
        if (hasSuccess) editor.putLong(Constants.KEY_LAST_UPDATE_TS, System.currentTimeMillis())
        editor.apply()
    }

    fun getEtaMinutes(mode: TravelMode): Int? {
        val key = etaKey(mode)
        return if (prefs.contains(key)) prefs.getInt(key, -1).takeIf { it >= 0 } else null
    }

    val lastUpdateTimestamp: Long
        get() = prefs.getLong(Constants.KEY_LAST_UPDATE_TS, 0L)

    private fun etaKey(mode: TravelMode) = when (mode) {
        TravelMode.DRIVING -> Constants.KEY_CAR_MINUTES
        TravelMode.WALKING -> Constants.KEY_WALK_MINUTES
        TravelMode.TRANSIT -> Constants.KEY_TRANSIT_MINUTES
    }

    // Preserve coordinates stored as Float by version 1.
    private fun readCoordinate(key: String): Double {
        if (!prefs.contains(key)) return 0.0
        return try {
            Double.fromBits(prefs.getLong(key, 0L))
        } catch (_: ClassCastException) {
            prefs.getFloat(key, 0f).toDouble()
        }
    }
}
