package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.entity.Store
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val suggestedStoreName: String = "",
    val nearestSavedStore: Store? = null,
    val distanceToNearestStoreMeters: Float? = null
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()

            try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resultAddress = ""
                var featureName = ""
                val addresses = suspendCancellableCoroutine<List<Address>> { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addrs ->
                        cont.resume(addrs)
                    }
                }
                if (addresses.isNotEmpty()) {
                    val addr = addresses[0]
                    resultAddress = formatAddress(addr)
                    featureName = addr.featureName ?: ""
                }
                Pair(resultAddress, featureName)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    Pair(formatAddress(addr), addr.featureName ?: "")
                } else {
                    Pair(String.format(Locale.US, "%.4f, %.4f", latitude, longitude), "")
                }
            }
        } catch (e: Exception) {
            Pair(String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude), "")
        }
    }

    private fun formatAddress(address: Address): String {
        val sb = StringBuilder()
        val thoroughfare = address.thoroughfare
        val subThoroughfare = address.subThoroughfare
        val locality = address.locality ?: address.subAdminArea ?: ""
        val adminArea = address.adminArea ?: ""

        if (!thoroughfare.isNullOrBlank()) {
            if (!subThoroughfare.isNullOrBlank()) {
                sb.append(subThoroughfare).append(" ")
            }
            sb.append(thoroughfare)
        } else if (!address.featureName.isNullOrBlank()) {
            sb.append(address.featureName)
        }

        if (locality.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(locality)
        }
        if (adminArea.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(adminArea)
        }

        return if (sb.isNotEmpty()) sb.toString() else address.getAddressLine(0) ?: ""
    }

    fun distanceBetweenMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun findNearestStore(
        latitude: Double,
        longitude: Double,
        savedStores: List<Store>,
        thresholdMeters: Float = 500f
    ): Pair<Store?, Float?> {
        var closestStore: Store? = null
        var minDistance = Float.MAX_VALUE

        for (store in savedStores) {
            if (store.latitude != 0.0 && store.longitude != 0.0) {
                val dist = distanceBetweenMeters(latitude, longitude, store.latitude, store.longitude)
                if (dist < minDistance) {
                    minDistance = dist
                    closestStore = store
                }
            }
        }

        return if (closestStore != null && minDistance <= thresholdMeters) {
            Pair(closestStore, minDistance)
        } else {
            Pair(closestStore, if (closestStore != null) minDistance else null)
        }
    }
}
