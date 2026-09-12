package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.CapturedMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM captured_media WHERE isPrivate = 0 ORDER BY timestamp DESC")
    fun getAllPublicMedia(): Flow<List<CapturedMedia>>

    @Query("SELECT * FROM captured_media WHERE isPrivate = 1 ORDER BY timestamp DESC")
    fun getPrivateMedia(): Flow<List<CapturedMedia>>

    @Query("SELECT * FROM captured_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): CapturedMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: CapturedMedia): Long

    @Update
    suspend fun updateMedia(media: CapturedMedia)

    @Query("UPDATE captured_media SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivateState(id: Long, isPrivate: Boolean)

    @Query("UPDATE captured_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavoriteState(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM captured_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)
}
