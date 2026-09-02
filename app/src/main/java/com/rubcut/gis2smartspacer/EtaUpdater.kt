package com.rubcut.gis2smartspacer

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil

object EtaUpdater {

    suspend fun refresh(
        context: Context,
        settings: ComplicationSettings,
        modes: Set<TravelMode> = TravelMode.values().toSet()
    ): Boolean {
        if (!settings.isConfigured || modes.isEmpty()) return false
        val origin = LocationHelper.getLastKnownLocation(context) ?: return false
        return refreshFrom(origin, settings, modes)
    }

    suspend fun refreshFrom(
        origin: GeoPoint,
        settings: ComplicationSettings,
        modes: Set<TravelMode> = TravelMode.values().toSet()
    ): Boolean = coroutineScope {
        val destination = GeoPoint(settings.destLat, settings.destLon)
        val client = TwoGisClient(settings.apiKey)

        // Keep independent requests parallel to stay within the BroadcastReceiver time limit.
        val results = modes.map { mode ->
            async {
                val minutes = client.routeDuration(mode, origin, destination)
                    ?.takeIf { it >= 0 }
                    ?.let { ceil(it / 60.0).toInt() }
                mode to minutes
            }
        }.awaitAll().toMap()

        settings.setEtaResults(results)
        results.values.any { it != null }
    }
}
