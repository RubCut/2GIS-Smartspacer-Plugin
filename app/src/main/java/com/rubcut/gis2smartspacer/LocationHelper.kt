package com.rubcut.gis2smartspacer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationHelper {

    fun hasForegroundPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): GeoPoint? {
        if (!hasForegroundPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return manager.getProviders(true)
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
            .maxByOrNull(Location::getTime)
            ?.toGeoPoint()
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GeoPoint? {
        if (!hasForegroundPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { manager.isProviderEnabled(it) }
            ?: return getLastKnownLocation(context)

        val current = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val cancellation = CancellationSignal()
                    manager.getCurrentLocation(
                        provider,
                        cancellation,
                        ContextCompat.getMainExecutor(context)
                    ) { location -> if (continuation.isActive) continuation.resume(location) }
                    continuation.invokeOnCancellation { cancellation.cancel() }
                } else {
                    @Suppress("DEPRECATION")
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            manager.removeUpdates(this)
                            if (continuation.isActive) continuation.resume(location)
                        }
                        override fun onProviderDisabled(provider: String) = Unit
                        override fun onProviderEnabled(provider: String) = Unit
                        @Deprecated("Deprecated in Android")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, null)
                    continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                }
            }
        }
        return current?.toGeoPoint() ?: getLastKnownLocation(context)
    }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
}
