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
        WHERE teacherId = :teacherId
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
        teacherId: String,
        searchPattern: String,
        ageFilter: String
    ): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudentEntity?

    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("SELECT * FROM students WHERE teacherId = :teacherId")
    suspend fun getAllForTeacher(teacherId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE substr(birthday, 6, 5) = :monthDay")
    suspend fun getStudentsWithBirthday(monthDay: String): List<StudentEntity>

    @Query("UPDATE students SET isSynced = 1 WHERE teacherId = :teacherId")
    suspend fun markSynced(teacherId: String)
}
