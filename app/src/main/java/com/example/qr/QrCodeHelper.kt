package com.example.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.SavedLocation
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject

data class SharedLocationData(
    val name: String,
    val fare: Double,
    val notes: String,
    val latitude: Double,
    val longitude: Double,
    val addressSnippet: String = ""
)

object QrCodeHelper {

    fun generateQrBitmap(content: String, sizePx: Int = 600): ImageBitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun encodeLocationToPayload(location: SavedLocation): String {
        val json = JSONObject().apply {
            put("v", 1)
            put("type", "cabrouter_item")
            put("name", location.name)
            put("fare", location.fare)
            put("notes", location.notes)
            put("lat", location.latitude)
            put("lng", location.longitude)
            put("addr", location.addressSnippet)
        }
        return json.toString()
    }

    fun parseLocationPayload(payload: String): SharedLocationData? {
        return try {
            val json = JSONObject(payload.trim())
            val name = json.optString("name", "").ifBlank { "Destino compartido" }
            val fare = json.optDouble("fare", 0.0)
            val notes = json.optString("notes", "")
            val lat = json.optDouble("lat", 0.0)
            val lng = json.optDouble("lng", 0.0)
            val addr = json.optString("addr", "")
            SharedLocationData(name, fare, notes, lat, lng, addr)
        } catch (_: Exception) {
            null
        }
    }

    fun exportBackupJson(locations: List<SavedLocation>): String {
        val root = JSONObject()
        root.put("app", "CabRouter")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        val array = JSONArray()
        for (loc in locations) {
            val obj = JSONObject().apply {
                put("name", loc.name)
                put("fare", loc.fare)
                put("notes", loc.notes)
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
                put("accuracyMeters", loc.accuracyMeters.toDouble())
                put("addressSnippet", loc.addressSnippet)
                put("createdAt", loc.createdAt)
                put("lastUsedAt", loc.lastUsedAt)
                put("tripCount", loc.tripCount)
            }
            array.put(obj)
        }
        root.put("locations", array)
        return root.toString(2)
    }

    fun parseBackupJson(rawJson: String): List<SavedLocation>? {
        return try {
            val root = JSONObject(rawJson.trim())
            val array = root.optJSONArray("locations") ?: return null
            val result = mutableListOf<SavedLocation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    SavedLocation(
                        id = 0,
                        name = obj.optString("name", "Destino"),
                        fare = obj.optDouble("fare", 0.0),
                        notes = obj.optString("notes", ""),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        accuracyMeters = obj.optDouble("accuracyMeters", 0.0).toFloat(),
                        addressSnippet = obj.optString("addressSnippet", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastUsedAt = obj.optLong("lastUsedAt", System.currentTimeMillis()),
                        tripCount = obj.optInt("tripCount", 0)
                    )
                )
            }
            result
        } catch (_: Exception) {
            null
        }
    }
}
