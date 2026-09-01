package com.rubcut.gis2smartspacer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Без зависимости от Play Services: берём последнюю известную точку
 * из LocationManager (GPS/Network/Fused-провайдеры, которые есть на устройстве).
 * Этого достаточно для "откуда я обычно нахожусь", без активного слежения.
 */
object LocationHelper {

    fun getLastKnownLocation(context: Context): GeoPoint? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        var best: android.location.Location? = null
        for (provider in locationManager.getProviders(true)) {
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            if (best == null || location.time > best!!.time) {
                best = location
            }
        }
        return best?.let { GeoPoint(it.latitude, it.longitude) }
    }
}
