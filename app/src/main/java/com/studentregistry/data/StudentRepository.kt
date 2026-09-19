package com.studentregistry.data

import com.studentregistry.model.AgeFilter
import kotlinx.coroutines.flow.Flow

class StudentRepository(private val studentDao: StudentDao) {
    fun observeStudents(
        teacherId: String,
        query: String,
        ageFilter: AgeFilter
    ): Flow<List<StudentEntity>> {
        val pattern = "%${query.trim()}%"
        return studentDao.observeStudents(
            teacherId = teacherId,
            searchPattern = pattern,
            ageFilter = ageFilter.name
        )
    }

    suspend fun getStudent(studentId: String): StudentEntity? = studentDao.getById(studentId)

    suspend fun saveStudent(student: StudentEntity) {
        studentDao.upsert(student)
    }

    suspend fun deleteStudent(student: StudentEntity) {
        studentDao.delete(student)
    }

    suspend fun getTeacherStudents(teacherId: String): List<StudentEntity> {
        return studentDao.getAllForTeacher(teacherId)
    }

    suspend fun markTeacherSynced(teacherId: String) {
        studentDao.markSynced(teacherId)
    }

    suspend fun getTodaysBirthdays(monthDay: String): List<StudentEntity> {
        return studentDao.getStudentsWithBirthday(monthDay)
    }
}
