package com.rubcut.gis2smartspacer

import android.content.Context
import android.content.SharedPreferences

/**
 * Global settings storage.
 *
 * Since 2.5 every Complication instance keeps its own settings — use
 * [forComplication] to get them. The repository itself owns what is shared
 * between all instances:
 *  - [defaultApiKey] — the synced default 2GIS API key. Newly added
 *    Complications are pre-filled with it, and saving a new key in any
 *    Complication overwrites it;
 *  - the legacy shared settings written by versions up to 2.4, used once to
 *    migrate already added Complications without losing their configuration.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        seedDefaultKeyFromLegacy()
    }

    var defaultApiKey: String
        get() = prefs.getString(Constants.KEY_DEFAULT_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(Constants.KEY_DEFAULT_API_KEY, value).apply()

    fun forComplication(smartspacerId: String): ComplicationSettings =
        ComplicationSettings(this, prefs, smartspacerId)

    /** Removes every stored value of a removed Complication instance. */
    fun clearComplication(smartspacerId: String) {
        val prefix = Constants.INSTANCE_PREFIX + smartspacerId
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }

    /**
     * Called after a Complication saved its own settings: the legacy shared
     * blob is no longer needed as a template for instances that have not been
     * migrated yet, so newly added Complications start fresh.
     */
    fun onComplicationSaved() {
        prefs.edit()
            .remove(Constants.KEY_API_KEY)
            .remove(Constants.KEY_DEST_ADDRESS)
            .remove(Constants.KEY_DEST_LAT)
            .remove(Constants.KEY_DEST_LON)
            .remove(Constants.KEY_MANUAL_COORDINATES)
            .remove(Constants.KEY_CAR_MINUTES)
            .remove(Constants.KEY_WALK_MINUTES)
            .remove(Constants.KEY_TRANSIT_MINUTES)
            .remove(Constants.KEY_LAST_UPDATE_TS)
            .apply()
    }

    // If the app was configured before 2.5, promote its key to the synced
    // default so migrated and newly created Complications start from it.
    private fun seedDefaultKeyFromLegacy() {
        val legacyKey = prefs.getString(Constants.KEY_API_KEY, "").orEmpty()
        if (legacyKey.isNotBlank() && defaultApiKey.isBlank()) {
            defaultApiKey = legacyKey
        }
    }
}

/**
 * Settings of a single Smartspacer Complication instance ([smartspacerId]).
 *
 * The first access snapshots the legacy shared settings (written by versions
 * up to 2.4), so Complications added before the update keep their key,
 * destination and cached ETA. Instances created later start empty; the
 * settings screen pre-fills their key field with
 * [SettingsRepository.defaultApiKey].
 */
class ComplicationSettings internal constructor(
    private val repository: SettingsRepository,
    private val prefs: SharedPreferences,
    private val smartspacerId: String
) {

    init {
        initializeIfNeeded()
    }

    var apiKey: String
        get() = prefs.getString(field(FIELD_API_KEY), "").orEmpty()
        set(value) = prefs.edit().putString(field(FIELD_API_KEY), value).apply()

    var destAddress: String
        get() = prefs.getString(field(FIELD_DEST_ADDRESS), "").orEmpty()
        set(value) = prefs.edit().putString(field(FIELD_DEST_ADDRESS), value).apply()

    val destLat: Double
        get() = readCoordinate(FIELD_DEST_LAT)

    val destLon: Double
        get() = readCoordinate(FIELD_DEST_LON)

    val hasDestination: Boolean
        get() = prefs.contains(field(FIELD_DEST_LAT)) && prefs.contains(field(FIELD_DEST_LON))

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && hasDestination

    val usesManualCoordinates: Boolean
        get() = prefs.getBoolean(field(FIELD_MANUAL_COORDINATES), false)

    fun saveDestination(
        apiKey: String,
        address: String,
        point: GeoPoint,
        manualCoordinates: Boolean = false
    ) {
        prefs.edit()
            .putString(field(FIELD_API_KEY), apiKey)
            .putString(field(FIELD_DEST_ADDRESS), address)
            .putLong(field(FIELD_DEST_LAT), point.lat.toBits())
            .putLong(field(FIELD_DEST_LON), point.lon.toBits())
            .putBoolean(field(FIELD_MANUAL_COORDINATES), manualCoordinates)
            .remove(field(FIELD_CAR_MINUTES))
            .remove(field(FIELD_WALK_MINUTES))
            .remove(field(FIELD_TRANSIT_MINUTES))
            .remove(field(FIELD_LAST_UPDATE_TS))
            .apply()
        repository.onComplicationSaved()
    }

    fun setEtaResults(results: Map<TravelMode, Int?>) {
        val editor = prefs.edit()
        var hasSuccess = false
        results.forEach { (mode, minutes) ->
            val key = field(etaField(mode))
            if (minutes == null) {
                editor.remove(key)
            } else {
                editor.putInt(key, minutes)
                hasSuccess = true
            }
        }
        if (hasSuccess) editor.putLong(field(FIELD_LAST_UPDATE_TS), System.currentTimeMillis())
        editor.apply()
    }

    fun getEtaMinutes(mode: TravelMode): Int? {
        val key = field(etaField(mode))
        return if (prefs.contains(key)) prefs.getInt(key, -1).takeIf { it >= 0 } else null
    }

    val lastUpdateTimestamp: Long
        get() = prefs.getLong(field(FIELD_LAST_UPDATE_TS), 0L)

    /**
     * How often Smartspacer is asked to refresh this instance, minutes.
     * Kept inside [Constants.MIN_UPDATE_INTERVAL_MINUTES]..
     * [Constants.MAX_UPDATE_INTERVAL_MINUTES].
     */
    var updateIntervalMinutes: Int
        get() = Constants.clampUpdateInterval(
            prefs.getInt(field(FIELD_UPDATE_INTERVAL), Constants.DEFAULT_REFRESH_PERIOD_MINUTES)
        )
        set(value) {
            prefs.edit()
                .putInt(field(FIELD_UPDATE_INTERVAL), Constants.clampUpdateInterval(value))
                .apply()
        }

    /** What the Complication does when tapped. */
    var tapAction: TapActionMode
        get() = prefs.getString(field(FIELD_TAP_ACTION), null)
            ?.let { stored -> TapActionMode.entries.firstOrNull { it.name == stored } }
            ?: TapActionMode.SETTINGS
        set(value) {
            prefs.edit().putString(field(FIELD_TAP_ACTION), value.name).apply()
        }

    /**
     * Persists the behavior block (interval + tap action) of this instance.
     * Unlike the destination, it is saved immediately, without geocoding.
     */
    fun saveBehavior(updateIntervalMinutes: Int, tapAction: TapActionMode) {
        this.updateIntervalMinutes = updateIntervalMinutes
        this.tapAction = tapAction
    }

    private fun field(name: String): String =
        Constants.INSTANCE_PREFIX + smartspacerId + "_" + name

    private fun etaField(mode: TravelMode): String = when (mode) {
        TravelMode.DRIVING -> FIELD_CAR_MINUTES
        TravelMode.WALKING -> FIELD_WALK_MINUTES
        TravelMode.TRANSIT -> FIELD_TRANSIT_MINUTES
    }

    // Preserve coordinates stored as Float by version 1.
    private fun readCoordinate(name: String): Double {
        val key = field(name)
        if (!prefs.contains(key)) return 0.0
        return try {
            Double.fromBits(prefs.getLong(key, 0L))
        } catch (_: ClassCastException) {
            prefs.getFloat(key, 0f).toDouble()
        }
    }

    private fun initializeIfNeeded() {
        if (prefs.getBoolean(field(FIELD_INITIALIZED), false)) return
        val editor = prefs.edit()
        if (prefs.contains(Constants.KEY_API_KEY)) {
            // One-time snapshot of the shared settings written by versions <= 2.4.
            editor.putString(
                field(FIELD_API_KEY),
                prefs.getString(Constants.KEY_API_KEY, "").orEmpty()
            )
            if (prefs.contains(Constants.KEY_DEST_ADDRESS)) {
                editor.putString(
                    field(FIELD_DEST_ADDRESS),
                    prefs.getString(Constants.KEY_DEST_ADDRESS, "").orEmpty()
                )
            }
            copyLegacyCoordinate(editor, Constants.KEY_DEST_LAT, FIELD_DEST_LAT)
            copyLegacyCoordinate(editor, Constants.KEY_DEST_LON, FIELD_DEST_LON)
            editor.putBoolean(
                field(FIELD_MANUAL_COORDINATES),
                prefs.getBoolean(Constants.KEY_MANUAL_COORDINATES, false)
            )
            copyLegacyEta(editor, Constants.KEY_CAR_MINUTES, FIELD_CAR_MINUTES)
            copyLegacyEta(editor, Constants.KEY_WALK_MINUTES, FIELD_WALK_MINUTES)
            copyLegacyEta(editor, Constants.KEY_TRANSIT_MINUTES, FIELD_TRANSIT_MINUTES)
            if (prefs.contains(Constants.KEY_LAST_UPDATE_TS)) {
                editor.putLong(
                    field(FIELD_LAST_UPDATE_TS),
                    prefs.getLong(Constants.KEY_LAST_UPDATE_TS, 0L)
                )
            }
        }
        editor.putBoolean(field(FIELD_INITIALIZED), true)
        editor.apply()
    }

    private fun copyLegacyCoordinate(
        editor: SharedPreferences.Editor,
        legacyKey: String,
        fieldName: String
    ) {
        if (!prefs.contains(legacyKey)) return
        val value = try {
            Double.fromBits(prefs.getLong(legacyKey, 0L))
        } catch (_: ClassCastException) {
            prefs.getFloat(legacyKey, 0f).toDouble()
        }
        editor.putLong(field(fieldName), value.toBits())
    }

    private fun copyLegacyEta(
        editor: SharedPreferences.Editor,
        legacyKey: String,
        fieldName: String
    ) {
        if (!prefs.contains(legacyKey)) return
        editor.putInt(field(fieldName), prefs.getInt(legacyKey, -1))
    }

    private companion object {
        const val FIELD_API_KEY = "api_key"
        const val FIELD_DEST_ADDRESS = "dest_address"
        const val FIELD_DEST_LAT = "dest_lat"
        const val FIELD_DEST_LON = "dest_lon"
        const val FIELD_MANUAL_COORDINATES = "manual_coordinates"
        const val FIELD_CAR_MINUTES = "car_minutes"
        const val FIELD_WALK_MINUTES = "walk_minutes"
        const val FIELD_TRANSIT_MINUTES = "transit_minutes"
        const val FIELD_LAST_UPDATE_TS = "last_update_ts"
        const val FIELD_UPDATE_INTERVAL = "update_interval_minutes"
        const val FIELD_TAP_ACTION = "tap_action"
        const val FIELD_INITIALIZED = "initialized"
    }
}
