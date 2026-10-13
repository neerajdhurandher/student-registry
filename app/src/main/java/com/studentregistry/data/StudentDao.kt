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
          AND (
            name LIKE :searchPattern
            OR contact LIKE :searchPattern
            OR email LIKE :searchPattern
            OR admissionNumber LIKE :searchPattern
            OR apparId LIKE :searchPattern
          )
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeStudents(
        schoolId: String,
        searchPattern: String
    ): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE schoolId = :schoolId ORDER BY name COLLATE NOCASE ASC")
    fun observeBySchool(schoolId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAll(): List<StudentEntity>

    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Upsert
    suspend fun upsertAll(students: List<StudentEntity>)

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

    @Query("DELETE FROM students")
    suspend fun deleteAll()

    @Query("DELETE FROM students WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)
}
