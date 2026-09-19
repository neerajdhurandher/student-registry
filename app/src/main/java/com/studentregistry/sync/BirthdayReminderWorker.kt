package com.studentregistry.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.StudentRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BirthdayReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.settings.first()
        if (!settings.notificationsEnabled) {
            return Result.success()
        }

        val database = AppDatabase.getInstance(applicationContext)
        val repository = StudentRepository(database.studentDao())
        val monthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"))
        val students = repository.getTodaysBirthdays(monthDay)
        students.forEachIndexed { index, student ->
            NotificationHelper.showBirthdayNotification(
                context = applicationContext,
                notificationId = 1000 + index,
                studentName = student.name
            )
        }
        return Result.success()
    }
}
