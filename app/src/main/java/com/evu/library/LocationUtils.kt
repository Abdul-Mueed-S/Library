package com.evu.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Kept for the "Set Location (to here)" flow where a cached fix is fine
    // (user is standing there right now, opening the app fresh).
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var best: Location? = null
        for (provider in providers) {
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            if (best == null || location.accuracy < best!!.accuracy) {
                best = location
            }
        }
        return best
    }

    // Requests an ACTIVE fresh fix rather than trusting cached last-known values.
    // Fixes a real bug: with multiple location providers (GPS/Network), especially
    // with mock-location apps, different providers can hold different STALE cached
    // fixes — picking "best accuracy" among stale cached values can flip-flop between
    // two old locations instead of reflecting where the device actually is right now.
    fun requestFreshLocation(context: Context, timeoutMs: Long = 6000, onResult: (Location?) -> Unit) {
        if (!hasLocationPermission(context)) {
            onResult(null)
            return
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            onResult(getLastKnownLocation(context))
            return
        }

        var resolved = false
        val handler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (resolved) return
                resolved = true
                locationManager.removeUpdates(this)
                handler.removeCallbacksAndMessages(null)
                onResult(location)
            }
            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            onResult(null)
            return
        }

        // Timeout fallback in case no fresh fix arrives in time (e.g. indoors, no signal)
        handler.postDelayed({
            if (!resolved) {
                resolved = true
                locationManager.removeUpdates(listener)
                onResult(getLastKnownLocation(context))
            }
        }, timeoutMs)
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    const val SUGGESTION_RADIUS_METERS = 150.0
}