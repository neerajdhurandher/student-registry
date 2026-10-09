package com.studentregistry

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.SchoolEntity
import com.studentregistry.data.SchoolRepository
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.StudentEntity
import com.studentregistry.data.StudentRepository
import com.studentregistry.ui.theme.StudentRegistryTheme
import kotlinx.coroutines.flow.Flow

private val AdminPrimary = Color(0xFF1A56DB)
private val AdminPrimaryContainer = Color(0xFFDBEAFE)
private val AdminTertiary = Color(0xFF0D9488)
private val AdminTertiaryContainer = Color(0x1A0D9488)

private enum class SchoolStatusFilter {
    ALL,
    ACTIVE,
    INACTIVE
}

private enum class SchoolDirectoryTab {
    TEACHERS,
    STUDENTS
}

class AdminActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        val studentRepository = StudentRepository(database.studentDao())
        val schoolRepository = SchoolRepository(applicationContext, database.schoolDao(), database.studentDao())
        val settingsRepository = SettingsRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(applicationContext, studentRepository, schoolRepository, settingsRepository)
        )[MainViewModel::class.java]

        setContent {
            val settings by viewModel.settings.collectAsState()
            val schools by viewModel.schools.collectAsState()
            val context = LocalContext.current

            StudentRegistryTheme(
                darkTheme = settings.darkTheme,
                fontScale = settings.fontScale
            ) {
                LaunchedEffect(Unit) {
                    viewModel.messages.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                AdminScreen(
                    schools = schools,
                    onBack = { finish() },
                    onCreateSchool = viewModel::createSchool,
                    onAddTeacher = viewModel::addTeacherToSchool,
                    observeStudentsForSchool = viewModel::observeStudentsForSchool,
                    onAddStudent = viewModel::addStudentForSchool,
                    onUpdateSchool = viewModel::updateSchool,
                    onSetSchoolActive = viewModel::setSchoolActive,
                    onDeleteSchool = viewModel::deleteSchool
                )
            }
        }
    }
}

@Composable
private fun AdminScreen(
    schools: List<SchoolEntity>,
    onBack: () -> Unit,
    onCreateSchool: (name: String, address: String, contact: String, teacherEmails: List<String>) -> Unit,
    onAddTeacher: (schoolId: String, teacherEmail: String) -> Unit,
    observeStudentsForSchool: (String) -> Flow<List<StudentEntity>>,
    onAddStudent: (schoolId: String, teacherEmail: String, input: StudentFormInput) -> Unit,
    onUpdateSchool: (schoolId: String, name: String, address: String, contact: String) -> Unit,
    onSetSchoolActive: (schoolId: String, isActive: Boolean) -> Unit,
    onDeleteSchool: (schoolId: String) -> Unit
) {
    var showSchoolDialog by rememberSaveable { mutableStateOf(false) }
    var showTeacherDialog by rememberSaveable { mutableStateOf(false) }
    var showStudentDialog by rememberSaveable { mutableStateOf(false) }
    var schoolBeingEdited by remember { mutableStateOf<SchoolEntity?>(null) }
    var schoolPendingStatusChange by remember { mutableStateOf<SchoolEntity?>(null) }
    var schoolPendingDeletion by remember { mutableStateOf<SchoolEntity?>(null) }
    var selectedSchoolId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var directoryQuery by rememberSaveable { mutableStateOf("") }
    var directoryTab by rememberSaveable { mutableStateOf(SchoolDirectoryTab.TEACHERS) }
    var schoolStatusFilter by rememberSaveable { mutableStateOf(SchoolStatusFilter.ALL) }
    var preselectedTeacherSchoolId by rememberSaveable { mutableStateOf("") }
    var preselectedTeacherSchoolName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val screenColor = colorScheme.background
    val cardColor = colorScheme.surface
    val outlineColor = colorScheme.outlineVariant
    val titleColor = colorScheme.onSurface
    val secondaryTextColor = colorScheme.onSurfaceVariant
    val selectedSchool = remember(schools, selectedSchoolId) { schools.firstOrNull { it.id == selectedSchoolId } }
    val filteredSchools = remember(schools, searchQuery, schoolStatusFilter) {
        val query = searchQuery.trim()
        schools
            .asSequence()
            .filter {
                when (schoolStatusFilter) {
                    SchoolStatusFilter.ALL -> true
                    SchoolStatusFilter.ACTIVE -> it.isActive
                    SchoolStatusFilter.INACTIVE -> !it.isActive
                }
            }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .toList()
    }
    val totalTeachers = remember(schools) { schools.sumOf { csvCount(it.teacherEmailsCsv) } }
    val totalStudents = remember(schools) { schools.sumOf { csvCount(it.studentIdsCsv) } }
    val studentsFlow = remember(selectedSchoolId) { selectedSchoolId?.let(observeStudentsForSchool) }
    val schoolStudents by (studentsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    LaunchedEffect(selectedSchoolId, selectedSchool) {
        if (selectedSchoolId != null && selectedSchool == null) {
            selectedSchoolId = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenColor)
    ) {
        if (selectedSchool != null) {
            SchoolDirectoryScreen(
                school = selectedSchool,
                students = schoolStudents,
                selectedTab = directoryTab,
                query = directoryQuery,
                onBack = {
                    selectedSchoolId = null
                    directoryQuery = ""
                    directoryTab = SchoolDirectoryTab.TEACHERS
                },
                onQueryChange = { directoryQuery = it },
                onTabChange = { directoryTab = it },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = {
                    if (directoryTab == SchoolDirectoryTab.TEACHERS) {
                        preselectedTeacherSchoolId = selectedSchool.id
                        preselectedTeacherSchoolName = selectedSchool.name
                        showTeacherDialog = true
                    } else {
                        val teacherEmails = parseCsv(selectedSchool.teacherEmailsCsv)
                        if (teacherEmails.isEmpty()) {
                            Toast.makeText(context, "Add a teacher first.", Toast.LENGTH_LONG).show()
                        } else {
                            showStudentDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (directoryTab == SchoolDirectoryTab.TEACHERS) "Add Teacher" else "Add Student",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = titleColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Console",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Text(
                            text = "School & Staff Management",
                            style = MaterialTheme.typography.bodyLarge,
                            color = secondaryTextColor
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2962E2), Color(0xFF1548C7))
                                ),
                                RoundedCornerShape(28.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.09f),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(132.dp)
                                .offset(x = 12.dp, y = 20.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFFDDE7FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "System Administrator",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = "Academic Year 2024–25",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminHeroStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Business,
                                    label = "Schools",
                                    value = schools.size.toString(),
                                    caption = "Active Schools"
                                )
                                AdminHeroStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Badge,
                                    label = "Teachers",
                                    value = totalTeachers.toString(),
                                    caption = "Teachers Enrolled"
                                )
                                AdminHeroStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.School,
                                    label = "Students",
                                    value = totalStudents.toString(),
                                    caption = "Enrolled Total"
                                )
                            }
                        }
                    }
                }

                AdminSearchBar(
                    query = searchQuery,
                    placeholder = "Search schools by name"
                ) { searchQuery = it }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminFilterChip(
                        selected = schoolStatusFilter == SchoolStatusFilter.ALL,
                        label = "All Campuses (${schools.size})",
                        onClick = { schoolStatusFilter = SchoolStatusFilter.ALL }
                    )
                    AdminFilterChip(
                        selected = schoolStatusFilter == SchoolStatusFilter.ACTIVE,
                        label = "Active",
                        onClick = { schoolStatusFilter = SchoolStatusFilter.ACTIVE }
                    )
                    AdminFilterChip(
                        selected = schoolStatusFilter == SchoolStatusFilter.INACTIVE,
                        label = "In-Active",
                        onClick = { schoolStatusFilter = SchoolStatusFilter.INACTIVE }
                    )
                }

                if (filteredSchools.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = cardColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No schools found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try a different search or create your first school.",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
                            )
                        }
                    }
                } else {
                    filteredSchools.forEach { school ->
                        AdminSchoolCard(
                            school = school,
                            onOpenSchool = {
                                selectedSchoolId = school.id
                                directoryQuery = ""
                                directoryTab = SchoolDirectoryTab.TEACHERS
                            },
                            onEditSchool = { schoolBeingEdited = school },
                            onToggleSchoolState = { schoolPendingStatusChange = school },
                            onDeleteSchool = { schoolPendingDeletion = school },
                            onAddTeacher = {
                                preselectedTeacherSchoolId = school.id
                                preselectedTeacherSchoolName = school.name
                                showTeacherDialog = true
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 92.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFEFF4FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5E4F8))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = AdminPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "FERPA & Ed-Data Privacy Compliant",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Only institutional domain accounts (@district / @school) are eligible for single-sign-on and administrative clearance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { showSchoolDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add School",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showSchoolDialog) {
        SchoolFormDialog(
            title = "Add School",
            description = "Create a school profile for academic records access.",
            includeTeachers = true,
            onDismiss = { showSchoolDialog = false },
            onSubmit = { name, address, contact, teacherCsv ->
                val teacherList = teacherCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                onCreateSchool(name, address, contact, teacherList)
                showSchoolDialog = false
            }
        )
    }

    if (showTeacherDialog) {
        var selectedSchoolId by rememberSaveable(preselectedTeacherSchoolId) { mutableStateOf(preselectedTeacherSchoolId) }
        var selectedSchoolName by rememberSaveable(preselectedTeacherSchoolName) { mutableStateOf(preselectedTeacherSchoolName) }
        var teacherEmail by rememberSaveable { mutableStateOf("") }
        var schoolError by rememberSaveable { mutableStateOf<String?>(null) }
        var teacherEmailError by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showTeacherDialog = false },
            title = {
                Text(
                    text = "Add Teacher",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Link a teacher Gmail account to this school.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                    OutlinedTextField(
                        value = selectedSchoolName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("School") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = schoolError != null,
                        colors = adminFormFieldColors()
                    )
                    if (schoolError != null) {
                        Text(
                            text = schoolError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = teacherEmail,
                        onValueChange = {
                            teacherEmail = it
                            teacherEmailError = null
                        },
                        label = { Text("Teacher Gmail") },
                        placeholder = { Text("teacher@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = teacherEmailError != null,
                        colors = adminFormFieldColors()
                    )
                    if (teacherEmailError != null) {
                        Text(
                            text = teacherEmailError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalizedEmail = teacherEmail.trim().lowercase()
                    val validSchool = selectedSchoolId.isNotBlank()
                    val validGmail = Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() &&
                        normalizedEmail.endsWith("@gmail.com")

                    schoolError = if (!validSchool) "Select a school." else null
                    teacherEmailError = if (!validGmail) "Enter a valid Gmail address." else null

                    if (!validSchool || !validGmail) return@TextButton

                    onAddTeacher(selectedSchoolId, normalizedEmail)
                    showTeacherDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTeacherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStudentDialog && selectedSchool != null) {
        AddStudentDialog(
            school = selectedSchool,
            teacherEmails = parseCsv(selectedSchool.teacherEmailsCsv),
            onDismiss = { showStudentDialog = false },
            onSubmit = { teacherEmail, input ->
                onAddStudent(selectedSchool.id, teacherEmail, input)
                showStudentDialog = false
            }
        )
    }

    schoolBeingEdited?.let { school ->
        SchoolFormDialog(
            title = "Edit School",
            description = "Update the school details shown to teachers and admins.",
            initialName = school.name,
            initialAddress = school.address,
            initialContact = school.contactInformation,
            includeTeachers = false,
            onDismiss = { schoolBeingEdited = null },
            onSubmit = { name, address, contact, _ ->
                onUpdateSchool(school.id, name, address, contact)
                schoolBeingEdited = null
            }
        )
    }

    schoolPendingStatusChange?.let { school ->
        val targetActiveState = !school.isActive
        AlertDialog(
            onDismissRequest = { schoolPendingStatusChange = null },
            title = {
                Text(
                    text = if (targetActiveState) "Activate School" else "Deactivate School",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (targetActiveState) {
                        "This school will become active again and teachers linked to it can sign in normally."
                    } else {
                        "This school will be marked inactive and linked teachers will no longer pass school access login."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetSchoolActive(school.id, targetActiveState)
                    schoolPendingStatusChange = null
                }) {
                    Text(if (targetActiveState) "Activate" else "Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolPendingStatusChange = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    schoolPendingDeletion?.let { school ->
        AlertDialog(
            onDismissRequest = { schoolPendingDeletion = null },
            title = {
                Text(
                    text = "Delete School",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete ${school.name} and remove its linked students from local and Firebase storage. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSchool(school.id)
                    schoolPendingDeletion = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SchoolDirectoryScreen(
    school: SchoolEntity,
    students: List<StudentEntity>,
    selectedTab: SchoolDirectoryTab,
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabChange: (SchoolDirectoryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val teacherRows = remember(school.teacherEmailsCsv) {
        parseCsv(school.teacherEmailsCsv)
            .map { email -> DirectoryRow(displayNameFromEmail(email), email) }
            .sortedBy { it.name.lowercase() }
    }
    val studentRows = remember(students) {
        students
            .map { student -> DirectoryRow(student.name.ifBlank { "Unknown Student" }, student.email.ifBlank { "No email added" }) }
            .sortedBy { it.name.lowercase() }
    }
    val visibleRows = remember(selectedTab, query, teacherRows, studentRows) {
        val normalizedQuery = query.trim()
        val source = if (selectedTab == SchoolDirectoryTab.TEACHERS) teacherRows else studentRows
        source.filter {
            normalizedQuery.isBlank() ||
                it.name.contains(normalizedQuery, ignoreCase = true) ||
                it.email.contains(normalizedQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = school.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = school.address.ifBlank { "School directory" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AdminSearchBar(
            query = query,
            placeholder = if (selectedTab == SchoolDirectoryTab.TEACHERS) {
                "Search teachers by name or email"
            } else {
                "Search students by name or email"
            },
            onValueChange = onQueryChange
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminFilterChip(
                selected = selectedTab == SchoolDirectoryTab.TEACHERS,
                label = "Teacher",
                onClick = { onTabChange(SchoolDirectoryTab.TEACHERS) }
            )
            AdminFilterChip(
                selected = selectedTab == SchoolDirectoryTab.STUDENTS,
                label = "Student",
                onClick = { onTabChange(SchoolDirectoryTab.STUDENTS) }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 92.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (selectedTab == SchoolDirectoryTab.TEACHERS) "Teachers" else "Students",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (visibleRows.isEmpty()) {
                    Text(
                        text = if (selectedTab == SchoolDirectoryTab.TEACHERS) {
                            "No teachers found for this school."
                        } else {
                            "No students found for this school."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    visibleRows.forEachIndexed { index, row ->
                        DirectoryListItem(name = row.name, email = row.email)
                        if (index != visibleRows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSearchBar(
    query: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun DirectoryListItem(name: String, email: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddStudentDialog(
    school: SchoolEntity,
    teacherEmails: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (teacherEmail: String, input: StudentFormInput) -> Unit
) {
    var selectedTeacherEmail by rememberSaveable(school.id) { mutableStateOf(teacherEmails.firstOrNull().orEmpty()) }
    var teacherExpanded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var teacherError by rememberSaveable { mutableStateOf<String?>(null) }
    val age = remember(birthday) { calculateAdminAgeFromBirthday(birthday) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Student",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add a student to ${school.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedTextField(
                        value = selectedTeacherEmail,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Teacher") },
                        placeholder = { Text("Select teacher") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { teacherExpanded = teacherEmails.size > 1 },
                        isError = teacherError != null,
                        colors = adminFormFieldColors()
                    )
                    DropdownMenu(
                        expanded = teacherExpanded,
                        onDismissRequest = { teacherExpanded = false }
                    ) {
                        teacherEmails.forEach { teacherEmail ->
                            DropdownMenuItem(
                                text = { Text(teacherEmail) },
                                onClick = {
                                    selectedTeacherEmail = teacherEmail
                                    teacherError = null
                                    teacherExpanded = false
                                }
                            )
                        }
                    }
                }
                if (teacherError != null) {
                    Text(
                        text = teacherError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student name") },
                    placeholder = { Text("e.g., Aarav Sharma") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("student@school.edu") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact number") },
                    placeholder = { Text("+91 9876543210") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                teacherError = if (selectedTeacherEmail.isBlank()) "Select a teacher first." else null
                if (teacherError != null) return@TextButton

                onSubmit(
                    selectedTeacherEmail,
                    StudentFormInput(
                        name = name,
                        age = age,
                        contact = contact,
                        fatherPhone = "",
                        motherPhone = "",
                        aadhaarNumber = "",
                        apparId = "",
                        penId = "",
                        fatherName = "",
                        motherName = "",
                        religion = "",
                        caste = "",
                        bloodGroup = "",
                        admissionNumber = "",
                        admissionDate = "",
                        bankNameAddress = "",
                        email = email,
                        birthday = birthday,
                        notes = ""
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class DirectoryRow(
    val name: String,
    val email: String
)

@Composable
private fun AdminHeroStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    caption: String
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE5EEFF),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun AdminFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) AdminPrimary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AdminPrimary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AdminSchoolCard(
    school: SchoolEntity,
    onOpenSchool: () -> Unit,
    onEditSchool: () -> Unit,
    onToggleSchoolState: () -> Unit,
    onDeleteSchool: () -> Unit,
    onAddTeacher: () -> Unit
) {
    val teacherCount = remember(school.teacherEmailsCsv) { csvCount(school.teacherEmailsCsv) }
    val studentCount = remember(school.studentIdsCsv) { csvCount(school.studentIdsCsv) }
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    var showMenu by rememberSaveable(school.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSchool),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (school.isActive) Color(0xFFE8F1FF) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (school.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (school.isActive) AdminPrimary else Color(0xFF6B7280)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditSchool) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = secondaryTextColor
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = secondaryTextColor
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (school.isActive) "Deactivate" else "Activate") },
                                onClick = {
                                    showMenu = false
                                    onToggleSchoolState()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDeleteSchool()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = school.address.ifBlank { "Address not added yet" },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AdminInfoMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    label = "$teacherCount Teachers"
                )
                AdminInfoMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    label = "$studentCount Students"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Teachers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                    Text(
                        text = if (teacherCount == 0) "No teacher assigned yet" else "$teacherCount teacher account(s) linked",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
                TextButton(onClick = onAddTeacher) {
                    Text("+ Add Teacher")
                }
            }
        }
    }
}

@Composable
private fun SchoolFormDialog(
    title: String,
    description: String,
    initialName: String = "",
    initialAddress: String = "",
    initialContact: String = "",
    initialTeachers: String = "",
    includeTeachers: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String, contact: String, teachers: String) -> Unit
) {
    var name by rememberSaveable(title, initialName) { mutableStateOf(initialName) }
    var address by rememberSaveable(title, initialAddress) { mutableStateOf(initialAddress) }
    var contact by rememberSaveable(title, initialContact) { mutableStateOf(initialContact) }
    var teachers by rememberSaveable(title, initialTeachers) { mutableStateOf(initialTeachers) }
    var schoolNameError by rememberSaveable(title) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        schoolNameError = null
                    },
                    label = { Text("School name") },
                    placeholder = { Text("e.g., ABC Public School") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = schoolNameError != null,
                    colors = adminFormFieldColors()
                )
                if (schoolNameError != null) {
                    Text(
                        text = schoolNameError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    placeholder = { Text("Street, city, district") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact information") },
                    placeholder = { Text("Phone number or office details") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminFormFieldColors()
                )
                if (includeTeachers) {
                    OutlinedTextField(
                        value = teachers,
                        onValueChange = { teachers = it },
                        label = { Text("Teacher emails") },
                        placeholder = { Text("Comma separated Gmail addresses") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = adminFormFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalizedName = name.trim()
                schoolNameError = when {
                    normalizedName.isBlank() -> "School name is required."
                    normalizedName.length < 2 -> "Enter a valid school name."
                    else -> null
                }
                if (schoolNameError != null) return@TextButton

                onSubmit(normalizedName, address, contact, teachers)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminInfoMetric(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AdminPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdminPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun csvCount(csv: String): Int = csv
    .split(",")
    .map { it.trim() }
    .count { it.isNotEmpty() }

private fun parseCsv(csv: String): List<String> = csv
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

private fun displayNameFromEmail(email: String): String {
    val localPart = email.substringBefore("@").replace('.', ' ').replace('_', ' ').trim()
    if (localPart.isBlank()) return email
    return localPart.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
}

private fun calculateAdminAgeFromBirthday(birthday: String): String = runCatching {
    val birthDate = java.time.LocalDate.parse(birthday)
    val today = java.time.LocalDate.now()
    java.time.Period.between(birthDate, today).years.coerceAtLeast(0).toString()
}.getOrDefault("")

@Composable
private fun adminFormFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AdminPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = AdminPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedLeadingIconColor = AdminPrimary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    focusedTrailingIconColor = AdminPrimary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = AdminPrimary,
    errorCursorColor = MaterialTheme.colorScheme.error
)
