package com.studentregistry.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query(
        """
        SELECT * FROM students
        WHERE schoolId = :schoolId
          AND (name LIKE :searchPattern OR contact LIKE :searchPattern OR email LIKE :searchPattern)
          AND (
            :ageFilter = 'ALL' OR
            (:ageFilter = 'MINOR' AND age < 18) OR
            (:ageFilter = 'ADULT' AND age >= 18)
          )
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeStudents(
        schoolId: String,
        searchPattern: String,
        ageFilter: String
    ): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE schoolId = :schoolId ORDER BY name COLLATE NOCASE ASC")
    fun observeBySchool(schoolId: String): Flow<List<StudentEntity>>

    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("SELECT * FROM students WHERE schoolId = :schoolId")
    suspend fun getAllForSchool(schoolId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE substr(birthday, 6, 5) = :monthDay")
    suspend fun getStudentsWithBirthday(monthDay: String): List<StudentEntity>

    @Query("UPDATE students SET isSynced = 1 WHERE schoolId = :schoolId")
    suspend fun markSynced(schoolId: String)

    @Query("DELETE FROM students WHERE schoolId = :schoolId")
    suspend fun deleteForSchool(schoolId: String)
}
