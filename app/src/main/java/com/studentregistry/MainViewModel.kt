package com.studentregistry

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.SchoolEntity
import com.studentregistry.data.SchoolRepository
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

data class StudentFormErrors(
    val name: String? = null,
    val age: String? = null,
    val contact: String? = null,
    val parentContact: String? = null,
    val aadhaarNumber: String? = null,
    val apparId: String? = null,
    val penId: String? = null,
    val fatherName: String? = null,
    val motherName: String? = null,
    val religion: String? = null,
    val caste: String? = null,
    val bloodGroup: String? = null,
    val admissionNumber: String? = null,
    val admissionDate: String? = null,
    val bankNameAddress: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val notes: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(
            name,
            age,
            contact,
            parentContact,
            aadhaarNumber,
            apparId,
            penId,
            fatherName,
            motherName,
            religion,
            caste,
            bloodGroup,
            admissionNumber,
            admissionDate,
            bankNameAddress,
            email,
            birthday,
            notes
        ).any { it != null }

    fun firstMessage(): String? = listOf(
        name,
        age,
        contact,
        parentContact,
        aadhaarNumber,
        apparId,
        penId,
        fatherName,
        motherName,
        religion,
        caste,
        bloodGroup,
        admissionNumber,
        admissionDate,
        bankNameAddress,
        email,
        birthday,
        notes
    ).firstOrNull { it != null }
}

private val validBloodGroups = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

private fun isValidIsoDate(value: String): Boolean = runCatching {
    java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
}.isSuccess

private fun hasValidPhoneDigits(value: String): Boolean {
    val digits = value.filter(Char::isDigit)
    return digits.length in 10..15
}

private fun validateOptionalName(value: String, label: String): String? {
    val normalized = value.trim()
    if (normalized.isBlank()) return null
    return if (normalized.length < 2) "Enter a valid $label." else null
}

fun validateStudentForm(input: StudentFormInput): StudentFormErrors {
    val trimmedName = input.name.trim()
    val ageValue = input.age.trim().toIntOrNull()
    val trimmedContact = input.contact.trim()
    val trimmedParentContact = input.parentContact.trim()
    val trimmedAadhaar = input.aadhaarNumber.filter(Char::isDigit)
    val trimmedApparId = input.apparId.filter(Char::isDigit)
    val trimmedPenId = input.penId.filter(Char::isDigit)
    val trimmedEmail = input.email.trim()
    val trimmedBirthday = input.birthday.trim()
    val trimmedAdmissionDate = input.admissionDate.trim()
    val trimmedBloodGroup = input.bloodGroup.trim().uppercase()
    val trimmedBank = input.bankNameAddress.trim()
    val notes = input.notes.trim()

    return StudentFormErrors(
        name = when {
            trimmedName.isBlank() -> "Full name is required."
            trimmedName.length < 2 -> "Enter a valid full name."
            else -> null
        },
        age = when {
            input.age.trim().isBlank() -> "Select date of birth to auto-calculate age."
            ageValue == null || ageValue !in 3..120 -> "Date of birth results in an invalid age."
            else -> null
        },
        contact = if (trimmedContact.isNotBlank() && !hasValidPhoneDigits(trimmedContact)) {
            "Enter a valid 10 digit student contact number."
        } else null,
        parentContact = if (trimmedParentContact.isNotBlank() && !hasValidPhoneDigits(trimmedParentContact)) {
            "Enter a valid 10 digit parent contact number."
        } else null,
        aadhaarNumber = if (input.aadhaarNumber.isNotBlank() && trimmedAadhaar.length != 12) {
            "Aadhaar number must be exactly 12 digits."
        } else null,
        apparId = if (input.apparId.isNotBlank() && trimmedApparId.length != 12) {
            "APPAR ID must be exactly 12 digits."
        } else null,
        penId = if (input.penId.isNotBlank() && trimmedPenId.length != 11) {
            "PEN ID must be exactly 11 digits."
        } else null,
        fatherName = validateOptionalName(input.fatherName, "father's name"),
        motherName = validateOptionalName(input.motherName, "mother's name"),
        religion = null,
        caste = null,
        bloodGroup = if (trimmedBloodGroup.isNotBlank() && trimmedBloodGroup !in validBloodGroups) {
            "Enter a valid blood group like A+, B+, AB+, or O-."
        } else null,
        admissionNumber = null,
        admissionDate = if (trimmedAdmissionDate.isNotBlank() && !isValidIsoDate(trimmedAdmissionDate)) {
            "Admission date must use YYYY-MM-DD format."
        } else null,
        bankNameAddress = if (trimmedBank.isNotBlank() && trimmedBank.length < 5) {
            "Enter bank name and address."
        } else null,
        email = if (trimmedEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            "Enter a valid email address."
        } else null,
        birthday = when {
            trimmedBirthday.isBlank() -> "Date of birth is required."
            !isValidIsoDate(trimmedBirthday) -> "Date of birth must use YYYY-MM-DD format."
            else -> null
        },
        notes = if (notes.length > 300) "Notes cannot exceed 300 characters." else null
    )
}

data class StudentFormInput(
    val name: String,
    val age: String,
    val contact: String,
    val parentContact: String,
    val aadhaarNumber: String,
    val apparId: String,
    val penId: String,
    val fatherName: String,
    val motherName: String,
    val religion: String,
    val caste: String,
    val bloodGroup: String,
    val admissionNumber: String,
    val admissionDate: String,
    val bankNameAddress: String,
    val email: String,
    val birthday: String,
    val notes: String
)

data class SchoolAccessUiState(
    val school: SchoolEntity? = null,
    val loading: Boolean = false,
    val requestNote: String = ""
) {
    val needsHelp: Boolean
        get() = school == null && !loading
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val appContext: Context,
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val currentUser = MutableStateFlow<TeacherUser?>(null)
    private val searchQuery = MutableStateFlow("")
    private val ageFilter = MutableStateFlow(AgeFilter.ALL)
    private val _messages = MutableSharedFlow<String>()
    private val schoolAccessUiState = MutableStateFlow(SchoolAccessUiState(loading = false))

    val user: StateFlow<TeacherUser?> = currentUser
    val schoolAccessState: StateFlow<SchoolAccessUiState> = schoolAccessUiState
    val selectedAgeFilter: StateFlow<AgeFilter> = ageFilter
    val currentSearchQuery: StateFlow<String> = searchQuery
    val messages = _messages.asSharedFlow()

    val settings: StateFlow<UiSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiSettings()
    )

    val schools: StateFlow<List<SchoolEntity>> = schoolRepository.observeSchools().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private data class StudentListScope(
        val signedInUser: TeacherUser?,
        val query: String,
        val filter: AgeFilter,
        val schoolId: String
    )

    val students: StateFlow<List<StudentEntity>> = combine(
        currentUser,
        searchQuery,
        ageFilter,
        settings
    ) { signedInUser, query, filter, settings ->
        StudentListScope(
            signedInUser = signedInUser,
            query = query,
            filter = filter,
            schoolId = settings.activeSchoolId
        )
    }.flatMapLatest { scope ->
        if (scope.signedInUser == null || scope.schoolId.isBlank()) {
            flowOf(emptyList())
        } else {
            studentRepository.observeStudents(
                schoolId = scope.schoolId,
                query = scope.query,
                ageFilter = scope.filter
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
            schoolAccessUiState.value = SchoolAccessUiState(loading = true)
            val school = schoolRepository.findSchoolForTeacher(user.email)
            if (school != null) {
                val syncedSchool = schoolRepository.syncSchoolFromFirestore(school.id) ?: school
                schoolRepository.cacheSchool(syncedSchool)
                settingsRepository.setActiveSchoolId(syncedSchool.id)
                schoolAccessUiState.value = SchoolAccessUiState(school = syncedSchool)
                WorkerScheduler.scheduleCloudSync(appContext, syncedSchool.id, user.email)
                _messages.emit("Welcome ${user.name}")
            } else {
                settingsRepository.setActiveSchoolId("")
                schoolAccessUiState.value = SchoolAccessUiState(school = null)
                _messages.emit("Ask admin to add your email to a school.")
            }
        }
    }

    fun clearSignedInUser() {
        currentUser.value = null
        viewModelScope.launch {
            settingsRepository.setActiveTeacherId("")
            settingsRepository.setActiveSchoolId("")
            schoolAccessUiState.value = SchoolAccessUiState(loading = false)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateAgeFilter(filter: AgeFilter) {
        ageFilter.value = filter
    }

    fun updateSchoolRequestNote(note: String) {
        schoolAccessUiState.value = schoolAccessUiState.value.copy(requestNote = note)
    }

    fun submitSchoolAccessRequest() {
        val signedInUser = currentUser.value ?: return
        val note = schoolAccessUiState.value.requestNote.trim()
        if (note.isBlank()) {
            viewModelScope.launch { _messages.emit("Please enter your details for the admin.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            schoolRepository.submitAccessRequest(signedInUser.email, signedInUser.name, note)
            _messages.emit("Request sent to admin.")
        }
    }

    fun createSchool(name: String, address: String, contactInformation: String, teacherEmails: List<String>) {
        val normalizedName = name.trim()
        if (normalizedName.length < 2) {
            viewModelScope.launch { _messages.emit("Enter a valid school name.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val school = schoolRepository.createSchool(normalizedName, address, contactInformation, teacherEmails)
                schoolAccessUiState.value = schoolAccessUiState.value.copy(school = school)
                settingsRepository.setActiveSchoolId(school.id)
            }.onSuccess {
                _messages.emit("School created successfully.")
            }.onFailure { error ->
                _messages.emit(error.message ?: "Unable to create school.")
            }
        }
    }

    fun addTeacherToSchool(schoolId: String, teacherEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                schoolRepository.addTeacherToSchool(schoolId, teacherEmail)
            }.onSuccess { updated ->
                if (updated != null) {
                    schoolAccessUiState.value = schoolAccessUiState.value.copy(school = updated)
                    settingsRepository.setActiveSchoolId(updated.id)
                    _messages.emit("Teacher added successfully.")
                } else {
                    _messages.emit("School not found.")
                }
            }.onFailure { error ->
                _messages.emit(error.message ?: "Unable to add teacher.")
            }
        }
    }

    fun updateSchool(schoolId: String, name: String, address: String, contactInformation: String) {
        val normalizedName = name.trim()
        if (normalizedName.length < 2) {
            viewModelScope.launch { _messages.emit("Enter a valid school name.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                schoolRepository.updateSchool(schoolId, normalizedName, address, contactInformation)
            }.onSuccess { updated ->
                if (updated != null) {
                    schoolAccessUiState.value = schoolAccessUiState.value.copy(school = updated)
                    _messages.emit("School updated successfully.")
                } else {
                    _messages.emit("School not found.")
                }
            }.onFailure { error ->
                _messages.emit(error.message ?: "Unable to update school.")
            }
        }
    }

    fun setSchoolActive(schoolId: String, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                schoolRepository.setSchoolActive(schoolId, isActive)
            }.onSuccess { updated ->
                if (updated != null) {
                    val currentAccessSchool = schoolAccessUiState.value.school
                    if (currentAccessSchool?.id == updated.id) {
                        schoolAccessUiState.value = schoolAccessUiState.value.copy(school = updated)
                    }
                    _messages.emit(if (isActive) "School activated successfully." else "School deactivated successfully.")
                } else {
                    _messages.emit("School not found.")
                }
            }.onFailure { error ->
                _messages.emit(error.message ?: "Unable to update school status.")
            }
        }
    }

    fun deleteSchool(schoolId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                schoolRepository.deleteSchool(schoolId)
            }.onSuccess { deleted ->
                if (deleted) {
                    val activeSchoolId = settings.value.activeSchoolId
                    if (activeSchoolId == schoolId) {
                        settingsRepository.setActiveSchoolId("")
                    }
                    val currentAccessSchool = schoolAccessUiState.value.school
                    if (currentAccessSchool?.id == schoolId) {
                        schoolAccessUiState.value = schoolAccessUiState.value.copy(school = null)
                    }
                    _messages.emit("School deleted successfully.")
                } else {
                    _messages.emit("School not found.")
                }
            }.onFailure { error ->
                _messages.emit(error.message ?: "Unable to delete school.")
            }
        }
    }

    fun observeStudentsForSchool(schoolId: String) = studentRepository.observeStudentsForSchool(schoolId)

    fun addStudentForSchool(schoolId: String, teacherEmail: String, input: StudentFormInput) {
        val normalizedSchoolId = schoolId.trim()
        val normalizedTeacherEmail = teacherEmail.trim().lowercase()
        if (normalizedSchoolId.isBlank()) {
            viewModelScope.launch { _messages.emit("School is not assigned yet.") }
            return
        }
        if (normalizedTeacherEmail.isBlank()) {
            viewModelScope.launch { _messages.emit("Select a teacher first.") }
            return
        }

        val validation = validateStudentForm(input)
        if (validation.hasErrors) {
            viewModelScope.launch { _messages.emit(validation.firstMessage() ?: "Please correct the form errors.") }
            return
        }

        val parsedAge = input.age.trim().toInt()

        viewModelScope.launch(Dispatchers.IO) {
            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                teacherId = normalizedTeacherEmail,
                schoolId = normalizedSchoolId,
                name = input.name.trim(),
                age = parsedAge,
                contact = input.contact.trim(),
                parentContact = input.parentContact.trim(),
                aadhaarNumber = input.aadhaarNumber.trim(),
                apparId = input.apparId.trim(),
                penId = input.penId.trim(),
                fatherName = input.fatherName.trim(),
                motherName = input.motherName.trim(),
                religion = input.religion.trim(),
                caste = input.caste.trim(),
                bloodGroup = input.bloodGroup.trim(),
                admissionNumber = input.admissionNumber.trim(),
                admissionDate = input.admissionDate.trim(),
                bankNameAddress = input.bankNameAddress.trim(),
                email = input.email.trim(),
                birthday = input.birthday.trim(),
                notes = input.notes.trim(),
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            studentRepository.saveStudent(student)
            if (settings.value.cloudSyncEnabled) {
                WorkerScheduler.triggerCloudSyncNow(appContext, normalizedSchoolId, normalizedTeacherEmail)
            }
            _messages.emit("Student added successfully.")
        }
    }

    fun saveStudent(input: StudentFormInput, studentId: String?) {
        val signedInUser = currentUser.value
        if (signedInUser == null) {
            viewModelScope.launch { _messages.emit("Please sign in first.") }
            return
        }
        val schoolId = settings.value.activeSchoolId
        if (schoolId.isBlank()) {
            viewModelScope.launch { _messages.emit("School is not assigned yet.") }
            return
        }

        val validation = validateStudentForm(input)
        if (validation.hasErrors) {
            viewModelScope.launch { _messages.emit(validation.firstMessage() ?: "Please correct the form errors.") }
            return
        }

        val parsedAge = input.age.trim().toInt()

        viewModelScope.launch(Dispatchers.IO) {
            val student = StudentEntity(
                id = studentId ?: UUID.randomUUID().toString(),
                teacherId = signedInUser.id,
                schoolId = schoolId,
                name = input.name.trim(),
                age = parsedAge,
                contact = input.contact.trim(),
                parentContact = input.parentContact.trim(),
                aadhaarNumber = input.aadhaarNumber.trim(),
                apparId = input.apparId.trim(),
                penId = input.penId.trim(),
                fatherName = input.fatherName.trim(),
                motherName = input.motherName.trim(),
                religion = input.religion.trim(),
                caste = input.caste.trim(),
                bloodGroup = input.bloodGroup.trim(),
                admissionNumber = input.admissionNumber.trim(),
                admissionDate = input.admissionDate.trim(),
                bankNameAddress = input.bankNameAddress.trim(),
                email = input.email.trim(),
                birthday = input.birthday.trim(),
                notes = input.notes.trim(),
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            studentRepository.saveStudent(student)
            if (settings.value.cloudSyncEnabled) {
                WorkerScheduler.triggerCloudSyncNow(appContext, schoolId, signedInUser.email)
            }
            _messages.emit("Student saved.")
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            studentRepository.deleteStudent(student)
            if (settings.value.cloudSyncEnabled) {
                WorkerScheduler.triggerCloudSyncNow(appContext, student.schoolId, student.teacherId)
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
    private val schoolRepository: SchoolRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(appContext, studentRepository, schoolRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
