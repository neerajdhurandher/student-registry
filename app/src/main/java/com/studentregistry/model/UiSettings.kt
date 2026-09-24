package com.studentregistry.model

data class UiSettings(
    val darkTheme: Boolean = false,
    val fontScale: Float = 1f,
    val notificationsEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = true,
    val activeTeacherId: String = "",
    val activeSchoolId: String = ""
)
