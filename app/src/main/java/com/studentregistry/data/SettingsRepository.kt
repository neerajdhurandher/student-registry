package com.studentregistry.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studentregistry.model.UiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val darkTheme: Preferences.Key<Boolean> = booleanPreferencesKey("dark_theme")
        val fontScale: Preferences.Key<Float> = floatPreferencesKey("font_scale")
        val notifications: Preferences.Key<Boolean> = booleanPreferencesKey("notifications")
        val cloudSync: Preferences.Key<Boolean> = booleanPreferencesKey("cloud_sync")
        val activeTeacher: Preferences.Key<String> = stringPreferencesKey("active_teacher")
        val activeSchool: Preferences.Key<String> = stringPreferencesKey("active_school")
    }

    val settings: Flow<UiSettings> = context.settingsDataStore.data.map { prefs ->
        UiSettings(
            darkTheme = prefs[Keys.darkTheme] ?: false,
            fontScale = prefs[Keys.fontScale] ?: 1f,
            notificationsEnabled = prefs[Keys.notifications] ?: true,
            cloudSyncEnabled = prefs[Keys.cloudSync] ?: true,
            activeTeacherId = prefs[Keys.activeTeacher] ?: "",
            activeSchoolId = prefs[Keys.activeSchool] ?: ""
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.darkTheme] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.fontScale] = scale }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.cloudSync] = enabled }
    }

    suspend fun setActiveTeacherId(teacherId: String) {
        context.settingsDataStore.edit { it[Keys.activeTeacher] = teacherId }
    }

    suspend fun setActiveSchoolId(schoolId: String) {
        context.settingsDataStore.edit { it[Keys.activeSchool] = schoolId }
    }
}
