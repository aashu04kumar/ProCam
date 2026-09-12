package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.CameraPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM camera_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<CameraPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CameraPreset): Long

    @Query("DELETE FROM camera_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
