package com.example.alp.utils // Your package

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import android.content.ActivityNotFoundException
import android.util.Log

@Composable
fun rememberLocationPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): androidx.activity.result.ActivityResultLauncher<Array<String>> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            onPermissionGranted()
        } else {
            // Check if permission was permanently denied
            val activity = context as? Activity
            if (activity != null &&
                (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                 !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION))
            ) {
                onPermissionPermanentlyDenied()
            } else {
                onPermissionDenied()
            }
        }
    }
}

fun hasLocationPermission(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
           locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun requestLocationEnable(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important if context is not Activity
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("LocationUtils", "Failed to open location settings", e)
        // Handle error, e.g., show a Toast
    }
}


fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important if context is not Activity
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("LocationUtils", "Failed to open app settings", e)
        // Handle error
    }
}


fun getCurrentLocation(
    context: Context,
    onSuccess: (lat: Double, lon: Double) -> Unit,
    onError: (Exception) -> Unit
) {
    if (!hasLocationPermission(context)) {
        onError(SecurityException("Location permission not granted."))
        return
    }
    if (!isLocationEnabled(context)) {
        onError(Exception("Location services are disabled."))
        return
    }

    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    // Fallback to last known location if current is null (less accurate)
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            onSuccess(lastLocation.latitude, lastLocation.longitude)
                        } else {
                            onError(Exception("Unable to retrieve location."))
                        }
                    }.addOnFailureListener(onError)
                }
            }
            .addOnFailureListener(onError)
    } catch (e: SecurityException) {
        onError(e) // Should be caught by hasLocationPermission, but good practice
    }
}