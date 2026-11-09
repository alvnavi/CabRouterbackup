package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getAllRecentLocations(): Flow<List<SavedLocation>>

    @Query("""
        SELECT * FROM saved_locations 
        WHERE name LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%' 
           OR CAST(fare AS TEXT) LIKE '%' || :query || '%'
        ORDER BY lastUsedAt DESC, createdAt DESC
    """)
    fun searchLocations(query: String): Flow<List<SavedLocation>>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Long): SavedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation): Long

    @Update
    suspend fun updateLocation(location: SavedLocation)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE saved_locations SET lastUsedAt = :timestamp, tripCount = tripCount + 1 WHERE id = :id")
    suspend fun recordTrip(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM saved_locations")
    fun getLocationCount(): Flow<Int>

    @Query("SELECT * FROM saved_locations ORDER BY id ASC")
    suspend fun getAllLocationsSnapshot(): List<SavedLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<SavedLocation>)

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAll()
}
