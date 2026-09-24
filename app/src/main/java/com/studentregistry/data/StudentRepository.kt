package com.studentregistry.data

import com.studentregistry.model.AgeFilter
import kotlinx.coroutines.flow.Flow

class StudentRepository(private val studentDao: StudentDao) {
    fun observeStudents(
        schoolId: String,
        query: String,
        ageFilter: AgeFilter
    ): Flow<List<StudentEntity>> {
        val pattern = "%${query.trim()}%"
        return studentDao.observeStudents(
            schoolId = schoolId,
            searchPattern = pattern,
            ageFilter = ageFilter.name
        )
    }

    suspend fun getStudent(studentId: String): StudentEntity? = studentDao.getById(studentId)

    fun observeStudentsForSchool(schoolId: String): Flow<List<StudentEntity>> = studentDao.observeBySchool(schoolId)

    suspend fun saveStudent(student: StudentEntity) {
        studentDao.upsert(student)
    }

    suspend fun deleteStudent(student: StudentEntity) {
        studentDao.delete(student)
    }

    suspend fun getSchoolStudents(schoolId: String): List<StudentEntity> {
        return studentDao.getAllForSchool(schoolId)
    }

    suspend fun markSchoolSynced(schoolId: String) {
        studentDao.markSynced(schoolId)
    }

    suspend fun getTodaysBirthdays(monthDay: String): List<StudentEntity> {
        return studentDao.getStudentsWithBirthday(monthDay)
    }
}
