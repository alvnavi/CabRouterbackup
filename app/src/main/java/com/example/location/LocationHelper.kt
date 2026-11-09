package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val addressSnippet: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GpsFix? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        var bestLocation: Location? = null

        // 1. Try Play Services FusedLocationProviderClient first
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            bestLocation = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            // If immediate high accuracy is null, check lastLocation
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                cont.resume(lastLoc)
                            }.addOnFailureListener {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }

                cont.invokeOnCancellation {
                    cts.cancel()
                }
            }
        } catch (_: Exception) {
            // Ignored, fallback to native LocationManager below
        }

        // 2. Fallback to native LocationManager (works 100% offline without Play Services)
        if (bestLocation == null) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val gpsLoc = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } else null

                    val netLoc = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } else null

                    bestLocation = when {
                        gpsLoc != null && netLoc != null -> {
                            if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
                        }
                        gpsLoc != null -> gpsLoc
                        else -> netLoc
                    }
                }
            } catch (_: Exception) { }
        }

        val location = bestLocation ?: return@withContext null
        val snippet = resolveAddressSnippet(context, location.latitude, location.longitude)

        GpsFix(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
            addressSnippet = snippet
        )
    }

    private fun resolveAddressSnippet(context: Context, lat: Double, lng: Double): String {
        val coordStr = formatCoordinates(lat, lng)
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.thoroughfare ?: addr.featureName
                    val subLocality = addr.subLocality ?: addr.locality
                    if (!feature.isNullOrBlank() && !subLocality.isNullOrBlank()) {
                        "$feature, $subLocality"
                    } else if (!feature.isNullOrBlank()) {
                        feature
                    } else if (!subLocality.isNullOrBlank()) {
                        subLocality
                    } else {
                        coordStr
                    }
                } else {
                    coordStr
                }
            } else {
                coordStr
            }
        } catch (_: Exception) {
            // Geocoder failure is normal when offline; gracefully return clean coordinates
            coordStr
        }
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "O"
        return String.format(Locale.US, "%.5f°%s, %.5f°%s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
    }
}
