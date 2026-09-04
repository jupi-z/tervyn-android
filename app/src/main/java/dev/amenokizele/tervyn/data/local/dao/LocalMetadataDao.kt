package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.amenokizele.tervyn.data.local.entity.LocalMetadataEntity

@Dao
interface LocalMetadataDao {
    @Query("SELECT * FROM local_metadata WHERE `key` = :key")
    suspend fun get(key: String): LocalMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: LocalMetadataEntity)
}
