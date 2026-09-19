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
        val teacherId = inputData.getString(TEACHER_ID_KEY).orEmpty()
        if (teacherId.isBlank()) {
            return Result.success()
        }

        val database = AppDatabase.getInstance(applicationContext)
        val repository = StudentRepository(database.studentDao())
        val syncService = CloudSyncService(applicationContext)
        val students = repository.getTeacherStudents(teacherId)
        val synced = syncService.syncStudents(teacherId, students)
        if (synced) {
            repository.markTeacherSynced(teacherId)
        }
        return Result.success()
    }

    companion object {
        const val TEACHER_ID_KEY: String = "teacher_id"
    }
}
