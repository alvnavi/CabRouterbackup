package com.example.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

sealed class NavigationLaunchResult {
    data object LaunchedWaze : NavigationLaunchResult()
    data object WazeNotInstalled : NavigationLaunchResult()
    data class Error(val message: String) : NavigationLaunchResult()
}

object WazeLauncher {

    /**
     * Formats label according to user requirement: "terminal 1- $50"
     */
    fun formatDestinationLabel(name: String, fare: Double): String {
        val cleanName = name.trim().ifBlank { "Destino" }
        val fareText = if (fare > 0) {
            if (fare % 1.0 == 0.0) "$${fare.toInt()}" else String.format(Locale.US, "$%.2f", fare)
        } else ""

        return if (fareText.isNotEmpty()) {
            "$cleanName - $fareText"
        } else {
            cleanName
        }
    }

    /**
     * Attempts to open the target destination in Waze for turn-by-turn navigation,
     * including destination title label (e.g. "Terminal 1 - $50") so Waze does not
     * display "Camino sin nombre".
     */
    fun navigateWithWaze(
        context: Context,
        latitude: Double,
        longitude: Double,
        name: String? = null,
        fare: Double = 0.0
    ): NavigationLaunchResult {
        return try {
            val label = formatDestinationLabel(name ?: "Destino", fare)
            val encodedLabel = Uri.encode(label)

            // Pass both ll coordinates and destination label q to Waze
            val wazeUri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes&q=$encodedLabel")
            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                setPackage("com.waze")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("title", label)
                putExtra("address", label)
                putExtra(Intent.EXTRA_TITLE, label)
            }

            // Check if Waze can handle the intent
            val resolveInfo = context.packageManager.resolveActivity(wazeIntent, 0)
            if (resolveInfo != null) {
                context.startActivity(wazeIntent)
                NavigationLaunchResult.LaunchedWaze
            } else {
                // Try fallback to waze web link with destination label
                val universalUri = Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes&q=$encodedLabel")
                val universalIntent = Intent(Intent.ACTION_VIEW, universalUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolveUniversal = context.packageManager.resolveActivity(universalIntent, 0)
                if (resolveUniversal != null && resolveUniversal.activityInfo.packageName == "com.waze") {
                    context.startActivity(universalIntent)
                    NavigationLaunchResult.LaunchedWaze
                } else {
                    NavigationLaunchResult.WazeNotInstalled
                }
            }
        } catch (e: ActivityNotFoundException) {
            NavigationLaunchResult.WazeNotInstalled
        } catch (e: Exception) {
            NavigationLaunchResult.Error(e.localizedMessage ?: "Error al iniciar navegación")
        }
    }

    /**
     * Fallback to system navigation (Google Maps or installed GPS app) with custom label
     */
    fun openFallbackNavigation(
        context: Context,
        latitude: Double,
        longitude: Double,
        name: String? = null,
        fare: Double = 0.0
    ) {
        val label = formatDestinationLabel(name ?: "Destino", fare)
        val encodedLabel = Uri.encode(label)
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        val navIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_TITLE, label)
        }
        try {
            context.startActivity(navIntent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) { }
        }
    }

    fun openPlayStoreForWaze(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.waze")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) { }
        }
    }
}
