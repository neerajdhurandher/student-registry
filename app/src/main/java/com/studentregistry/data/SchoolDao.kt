package com.studentregistry.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools ORDER BY name COLLATE NOCASE ASC")
    fun observeSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SchoolEntity?>

    @Query("DELETE FROM schools WHERE id = :id")
    suspend fun deleteById(id: String)

    @Upsert
    suspend fun upsert(school: SchoolEntity)
}
