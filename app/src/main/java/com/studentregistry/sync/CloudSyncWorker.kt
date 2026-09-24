package com.studentregistry.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.StudentRepository

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val schoolId = inputData.getString(SCHOOL_ID_KEY).orEmpty()
        val teacherEmail = inputData.getString(TEACHER_EMAIL_KEY).orEmpty()
        if (schoolId.isBlank() || teacherEmail.isBlank()) {
            return Result.success()
        }

        val database = AppDatabase.getInstance(applicationContext)
        val repository = StudentRepository(database.studentDao())
        val syncService = CloudSyncService(applicationContext)
        val school = database.schoolDao().getById(schoolId) ?: return Result.success()
        val students = repository.getSchoolStudents(schoolId)
        val synced = syncService.syncSchool(teacherEmail, school, students)
        if (synced) {
            repository.markSchoolSynced(schoolId)
        }
        return Result.success()
    }

    companion object {
        const val SCHOOL_ID_KEY: String = "school_id"
        const val TEACHER_EMAIL_KEY: String = "teacher_email"
    }
}
