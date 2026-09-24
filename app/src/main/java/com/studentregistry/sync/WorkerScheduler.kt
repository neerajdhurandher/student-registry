package com.studentregistry.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object WorkerScheduler {
    private const val BIRTHDAY_WORK_NAME: String = "birthday-reminders"

    fun scheduleBirthdayWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<BirthdayReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BIRTHDAY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleCloudSync(context: Context, schoolId: String, teacherEmail: String) {
        val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    CloudSyncWorker.SCHOOL_ID_KEY to schoolId,
                    CloudSyncWorker.TEACHER_EMAIL_KEY to teacherEmail
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "cloud-sync-$schoolId",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun triggerCloudSyncNow(context: Context, schoolId: String, teacherEmail: String) {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    CloudSyncWorker.SCHOOL_ID_KEY to schoolId,
                    CloudSyncWorker.TEACHER_EMAIL_KEY to teacherEmail
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "cloud-sync-now-$schoolId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
