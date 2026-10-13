package com.studentregistry

import android.app.Application
import com.studentregistry.sync.NotificationHelper
import com.studentregistry.sync.WorkerScheduler

class StudentRegistryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createBirthdayChannel(this)
        WorkerScheduler.scheduleBirthdayWorker(this)
        WorkerScheduler.scheduleStartupSync(this)
    }
}
