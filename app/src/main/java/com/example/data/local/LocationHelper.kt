package com.example.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class ResolvedCoordinates(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
)

class LocationHelper(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinatesAndCity(): ResolvedCoordinates? {
        if (!hasCoarseLocationPermission()) return null

        val location = getCurrentOrLastLocation() ?: return null
        val city = resolveCityName(location.latitude, location.longitude)
        return ResolvedCoordinates(
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = city,
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentOrLastLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token,
            ).addOnSuccessListener { current ->
                if (current != null) {
                    if (cont.isActive) cont.resume(current)
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (cont.isActive) cont.resume(last)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }.addOnFailureListener {
                fusedClient.lastLocation
                    .addOnSuccessListener { last ->
                        if (cont.isActive) cont.resume(last)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }

    private suspend fun resolveCityName(latitude: Double, longitude: Double): String {
        val fallback = context.getString(R.string.your_location_fallback)
        if (!Geocoder.isPresent()) return fallback

        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale("es", "MX"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    val city = extractCity(addresses.firstOrNull()) ?: fallback
                                    if (cont.isActive) cont.resume(city)
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(fallback)
                                }
                            },
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    extractCity(addresses?.firstOrNull()) ?: fallback
                }
            }.getOrDefault(fallback)
        }
    }

    private fun extractCity(address: Address?): String? {
        if (address == null) return null
        return listOf(
            address.locality,
            address.subAdminArea,
            address.adminArea,
        ).firstOrNull { !it.isNullOrBlank() }
    }
}
