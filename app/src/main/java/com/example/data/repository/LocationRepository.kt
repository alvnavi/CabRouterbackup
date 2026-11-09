package com.example.data.repository

import com.example.data.database.LocationDao
import com.example.data.model.SavedLocation
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val dao: LocationDao) {

    fun getLocations(query: String): Flow<List<SavedLocation>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            dao.getAllRecentLocations()
        } else {
            dao.searchLocations(trimmed)
        }
    }

    suspend fun getLocationById(id: Long): SavedLocation? {
        return dao.getLocationById(id)
    }

    suspend fun saveLocation(location: SavedLocation): Long {
        return dao.insertLocation(location)
    }

    suspend fun updateLocation(location: SavedLocation) {
        dao.updateLocation(location)
    }

    suspend fun deleteLocation(location: SavedLocation) {
        dao.deleteLocation(location)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun recordTrip(id: Long) {
        dao.recordTrip(id, System.currentTimeMillis())
    }

    fun getTotalCount(): Flow<Int> = dao.getLocationCount()

    suspend fun getAllLocationsSnapshot(): List<SavedLocation> {
        return dao.getAllLocationsSnapshot()
    }

    suspend fun restoreLocations(locations: List<SavedLocation>, clearExisting: Boolean) {
        if (clearExisting) {
            dao.deleteAll()
        }
        // Insert with new or reset IDs to avoid collisions
        val normalized = locations.map { it.copy(id = 0) }
        dao.insertAll(normalized)
    }
}
