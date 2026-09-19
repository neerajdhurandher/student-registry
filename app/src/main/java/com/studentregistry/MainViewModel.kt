package com.studentregistry

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.StudentEntity
import com.studentregistry.data.StudentRepository
import com.studentregistry.export.ExportManager
import com.studentregistry.model.AgeFilter
import com.studentregistry.model.TeacherUser
import com.studentregistry.model.UiSettings
import com.studentregistry.sync.WorkerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class StudentFormInput(
    val name: String,
    val age: String,
    val contact: String,
    val parentContact: String,
    val email: String,
    val birthday: String,
    val notes: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val appContext: Context,
    private val studentRepository: StudentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val currentUser = MutableStateFlow<TeacherUser?>(null)
    private val searchQuery = MutableStateFlow("")
    private val ageFilter = MutableStateFlow(AgeFilter.ALL)
    private val _messages = MutableSharedFlow<String>()

    val user: StateFlow<TeacherUser?> = currentUser
    val selectedAgeFilter: StateFlow<AgeFilter> = ageFilter
    val currentSearchQuery: StateFlow<String> = searchQuery
    val messages = _messages.asSharedFlow()

    val settings: StateFlow<UiSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiSettings()
    )

    val students: StateFlow<List<StudentEntity>> = combine(
        currentUser,
        searchQuery,
        ageFilter
    ) { signedInUser, query, filter ->
        Triple(signedInUser, query, filter)
    }.flatMapLatest { (signedInUser, query, filter) ->
        if (signedInUser == null) {
            flowOf(emptyList())
        } else {
            studentRepository.observeStudents(
                teacherId = signedInUser.id,
                query = query,
                ageFilter = filter
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setSignedInUser(user: TeacherUser) {
        currentUser.value = user
        viewModelScope.launch {
            settingsRepository.setActiveTeacherId(user.id)
            WorkerScheduler.scheduleCloudSync(appContext, user.id)
            _messages.emit("Welcome ${user.name}")
        }
    }

    fun clearSignedInUser() {
        currentUser.value = null
        viewModelScope.launch {
            settingsRepository.setActiveTeacherId("")
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateAgeFilter(filter: AgeFilter) {
        ageFilter.value = filter
    }

    fun saveStudent(input: StudentFormInput, studentId: String?) {
        val signedInUser = currentUser.value
        if (signedInUser == null) {
            viewModelScope.launch { _messages.emit("Please sign in first.") }
            return
        }

        val parsedAge = input.age.toIntOrNull()
        if (input.name.isBlank() || parsedAge == null) {
            viewModelScope.launch { _messages.emit("Name and valid age are required.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val student = StudentEntity(
                id = studentId ?: UUID.randomUUID().toString(),
                teacherId = signedInUser.id,
                name = input.name.trim(),
                age = parsedAge,
                contact = input.contact.trim(),
                parentContact = input.parentContact.trim(),
                email = input.email.trim(),
                birthday = input.birthday.trim(),
                notes = input.notes.trim(),
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            studentRepository.saveStudent(student)
            if (settings.value.cloudSyncEnabled) {
                WorkerScheduler.triggerCloudSyncNow(appContext, signedInUser.id)
            }
            _messages.emit("Student saved.")
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            studentRepository.deleteStudent(student)
            if (settings.value.cloudSyncEnabled) {
                WorkerScheduler.triggerCloudSyncNow(appContext, student.teacherId)
            }
            _messages.emit("Student deleted.")
        }
    }

    suspend fun getStudent(studentId: String): StudentEntity? {
        return studentRepository.getStudent(studentId)
    }

    fun exportCsv(exportManager: ExportManager) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = students.value
            if (list.isEmpty()) {
                _messages.emit("No students to export.")
                return@launch
            }
            val file = exportManager.exportCsv(list)
            _messages.emit("CSV exported to ${file.absolutePath}")
        }
    }

    fun exportPdf(exportManager: ExportManager) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = students.value
            if (list.isEmpty()) {
                _messages.emit("No students to export.")
                return@launch
            }
            val file = exportManager.exportPdf(list)
            _messages.emit("PDF exported to ${file.absolutePath}")
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun updateCloudSync(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCloudSyncEnabled(enabled) }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }

    fun postMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }
}

class MainViewModelFactory(
    private val appContext: Context,
    private val studentRepository: StudentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(appContext, studentRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
