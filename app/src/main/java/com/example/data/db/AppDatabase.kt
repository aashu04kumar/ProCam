package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.CameraPreset
import com.example.data.CapturedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CapturedMedia::class, CameraPreset::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lite_pro_cam_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial pro presets
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialPresets(database.presetDao())
                    }
                }
            }

            suspend fun populateInitialPresets(dao: PresetDao) {
                dao.insertPreset(
                    CameraPreset(
                        name = "Golden Hour Glow",
                        iso = 100,
                        shutterSpeed = "1/250s",
                        evCompensation = 0.3f,
                        whiteBalanceKelvin = 6200,
                        focusDistance = 0.8f,
                        lens = "1×"
                    )
                )
                dao.insertPreset(
                    CameraPreset(
                        name = "Astrophotography Sky",
                        iso = 3200,
                        shutterSpeed = "15s",
                        evCompensation = 0.0f,
                        whiteBalanceKelvin = 4200,
                        focusDistance = 1.0f,
                        lens = "1×"
                    )
                )
                dao.insertPreset(
                    CameraPreset(
                        name = "Crisp Street B&W",
                        iso = 400,
                        shutterSpeed = "1/500s",
                        evCompensation = -0.3f,
                        whiteBalanceKelvin = 5500,
                        focusDistance = 0.5f,
                        lens = "2×"
                    )
                )
                dao.insertPreset(
                    CameraPreset(
                        name = "Silky Waterfall",
                        iso = 50,
                        shutterSpeed = "2s",
                        evCompensation = -0.7f,
                        whiteBalanceKelvin = 5600,
                        focusDistance = 0.6f,
                        lens = "1×"
                    )
                )
            }
        }
    }
}
