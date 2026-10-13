package com.studentregistry.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.SchoolRepository
import kotlinx.coroutines.flow.first

class StartupSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()

        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.settings.first()
        if (!settings.cloudSyncEnabled) return Result.success()

        val database = AppDatabase.getInstance(applicationContext)
        val schoolRepository = SchoolRepository(
            applicationContext,
            database.schoolDao(),
            database.studentDao()
        )
        schoolRepository.syncAllSchoolsFromFirestore()
        return Result.success()
    }
}
