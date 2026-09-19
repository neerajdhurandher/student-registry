package com.studentregistry

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.StudentEntity
import com.studentregistry.data.StudentRepository
import com.studentregistry.export.ExportManager
import com.studentregistry.model.AgeFilter
import com.studentregistry.model.TeacherUser
import com.studentregistry.ui.theme.StudentRegistryTheme

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private val googleSignInClient by lazy {
        val webClientId = getString(R.string.default_web_client_id)
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        GoogleSignIn.getClient(
            this,
            signInOptions
        )
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        val studentRepository = StudentRepository(database.studentDao())
        val settingsRepository = SettingsRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(applicationContext, studentRepository, settingsRepository)
        )[MainViewModel::class.java]

        GoogleSignIn.getLastSignedInAccount(this)?.toTeacherUser()?.let(viewModel::setSignedInUser)

        setContent {
            val settings by viewModel.settings.collectAsState()
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
                StudentRegistryNavGraph(
                    viewModel = viewModel,
                    onGoogleSignIn = ::launchGoogleSignIn,
                    onGoogleSignOut = {
                        googleSignInClient.signOut()
                        viewModel.clearSignedInUser()
                    }
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        if (!hasInternetConnection()) {
            viewModel.postMessage("Google sign-in needs an active internet connection.")
            return
        }

        val playServicesStatus = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            if (googleApiAvailability.isUserResolvableError(playServicesStatus)) {
                googleApiAvailability.getErrorDialog(
                    this,
                    playServicesStatus,
                    GOOGLE_PLAY_SERVICES_REQUEST_CODE
                )?.show()
            } else {
                viewModel.postMessage("Google Play services is unavailable on this device.")
            }
            return
        }

        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleSignInResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK && data == null) {
            viewModel.postMessage("Google sign-in was cancelled.")
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val teacher = account?.toTeacherUser()
            if (teacher != null) {
                viewModel.setSignedInUser(teacher)
            } else {
                viewModel.postMessage("Unable to read your Google account.")
            }
        } catch (error: ApiException) {
            viewModel.postMessage(buildGoogleSignInErrorMessage(error))
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun buildGoogleSignInErrorMessage(error: ApiException): String {
        return when (error.statusCode) {
        CommonStatusCodes.NETWORK_ERROR ->
            "Google sign-in couldn't reach Google services. Check internet and update Google Play services."

        CommonStatusCodes.DEVELOPER_ERROR ->
            "Google sign-in is misconfigured. Re-download google-services.json after adding SHA-1 and SHA-256 for com.studentregistry."

        CommonStatusCodes.INVALID_ACCOUNT ->
            "The selected Google account isn't valid on this device."

        CommonStatusCodes.CANCELED ->
            "Google sign-in was cancelled."

        else -> {
            val statusName = CommonStatusCodes.getStatusCodeString(error.statusCode)
            "Google sign-in failed (${error.statusCode}: $statusName)."
        }
        }
    }

    companion object {
        private const val GOOGLE_PLAY_SERVICES_REQUEST_CODE = 9001
    }
}

private fun GoogleSignInAccount.toTeacherUser(): TeacherUser? {
    val email = email ?: return null
    val teacherId = id ?: email
    return TeacherUser(
        id = teacherId,
        name = displayName ?: email.substringBefore("@"),
        email = email
    )
}

private object Routes {
    const val LOGIN: String = "login"
    const val STUDENT_LIST: String = "student-list"
    const val STUDENT_DETAIL: String = "student-detail/{studentId}"
    const val ADD_EDIT: String = "add-edit?studentId={studentId}"
    const val SETTINGS: String = "settings"
    const val EXPORT: String = "export"
}

@Composable
private fun StudentRegistryNavGraph(
    viewModel: MainViewModel,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val user by viewModel.user.collectAsState()
    val students by viewModel.students.collectAsState()
    val searchQuery by viewModel.currentSearchQuery.collectAsState()
    val ageFilter by viewModel.selectedAgeFilter.collectAsState()
    val context = LocalContext.current
    val exportManager = remember(context) { ExportManager(context) }

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(Routes.STUDENT_LIST) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user == null) Routes.LOGIN else Routes.STUDENT_LIST
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onGoogleSignIn = onGoogleSignIn)
        }

        composable(Routes.STUDENT_LIST) {
            StudentListScreen(
                teacherName = user?.name.orEmpty(),
                students = students,
                query = searchQuery,
                selectedFilter = ageFilter,
                onQueryChange = viewModel::updateSearchQuery,
                onFilterChange = viewModel::updateAgeFilter,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenExport = { navController.navigate(Routes.EXPORT) },
                onAddStudent = { navController.navigate("add-edit?studentId=") },
                onOpenStudent = { studentId -> navController.navigate("student-detail/$studentId") }
            )
        }

        composable(
            route = Routes.STUDENT_DETAIL,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
            var student by remember(studentId, students) { mutableStateOf(students.firstOrNull { it.id == studentId }) }

            LaunchedEffect(studentId, students) {
                if (student == null) {
                    student = viewModel.getStudent(studentId)
                }
            }

            student?.let { loadedStudent ->
                StudentDetailScreen(
                    student = loadedStudent,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("add-edit?studentId=${loadedStudent.id}") },
                    onDelete = {
                        viewModel.deleteStudent(loadedStudent)
                        navController.popBackStack()
                    }
                )
            } ?: Text("Student not found.", modifier = Modifier.padding(16.dp))
        }

        composable(
            route = Routes.ADD_EDIT,
            arguments = listOf(
                navArgument("studentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId").orEmpty().ifBlank { null }
            var initialStudent by remember(studentId, students) {
                mutableStateOf(studentId?.let { id -> students.firstOrNull { it.id == id } })
            }

            LaunchedEffect(studentId, students) {
                if (studentId != null && initialStudent == null) {
                    initialStudent = viewModel.getStudent(studentId)
                }
            }

            AddEditStudentScreen(
                initialStudent = initialStudent,
                onBack = { navController.popBackStack() },
                onSave = { input ->
                    viewModel.saveStudent(input, studentId)
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS) {
            val settings by viewModel.settings.collectAsState()
            SettingsScreen(
                darkTheme = settings.darkTheme,
                fontScale = settings.fontScale,
                notificationsEnabled = settings.notificationsEnabled,
                cloudSyncEnabled = settings.cloudSyncEnabled,
                onBack = { navController.popBackStack() },
                onDarkThemeChanged = viewModel::updateDarkTheme,
                onFontScaleChanged = viewModel::updateFontScale,
                onNotificationsChanged = viewModel::updateNotifications,
                onCloudSyncChanged = viewModel::updateCloudSync,
                onSignOut = onGoogleSignOut
            )
        }

        composable(Routes.EXPORT) {
            ExportScreen(
                filteredCount = students.size,
                onBack = { navController.popBackStack() },
                onExportCsv = { viewModel.exportCsv(exportManager) },
                onExportPdf = { viewModel.exportPdf(exportManager) }
            )
        }
    }
}

// Academic Clarity Material 3 palette (from Stitch design brief)
private val LoginPrimary = Color(0xFF1A56DB)
private val LoginPrimaryContainer = Color(0xFFDBEAFE)
private val LoginOnPrimaryContainer = Color(0xFF1E40AF)
private val LoginTertiary = Color(0xFF0D9488)
private val LoginTertiaryContainer = Color(0x1A0D9488)
private val LoginSurfaceCanvas = Color(0xFFF8FAFC)
private val LoginSurfaceCard = Color(0xFFFFFFFF)
private val LoginOutlineVariant = Color(0xFFE2E8F0)
private val LoginOnSurface = Color(0xFF0F172A)
private val LoginOnSurfaceVariant = Color(0xFF434654)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(onGoogleSignIn: () -> Unit) {
    var isSigningIn by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LoginSurfaceCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Branding / hero section ---
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(LoginTertiaryContainer, RoundedCornerShape(24.dp))
                        .border(1.dp, LoginPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(LoginPrimaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = LoginPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(LoginSurfaceCard, CircleShape)
                        .border(2.dp, LoginSurfaceCanvas, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = LoginPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = LoginPrimaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "TEACHER SIGN-IN",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LoginPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Student Registry",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = LoginOnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Securely manage your students' records, contact " +
                    "details, and birthdays — all backed up to your Google account.",
                style = MaterialTheme.typography.bodyMedium,
                color = LoginOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- Quick context card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LoginSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, LoginOutlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LoginSurfaceCanvas, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = LoginPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Teacher Workspace",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LoginOnSurface
                            )
                            Surface(
                                color = LoginTertiaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Ready",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoginTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Access your student roster, search, and export tools " +
                                "the moment you sign in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoginOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Authentication CTA ---
            Button(
                onClick = {
                    isSigningIn = true
                    onGoogleSignIn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Student Registry • Private & securely synced",
                style = MaterialTheme.typography.labelSmall,
                color = LoginOnSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

// Additional Academic Clarity Material 3 tokens (shared with LoginScreen palette above)
private val ListSecondary = Color(0xFF516070)
private val ListOutline = Color(0xFF737686)
private val ListSurfaceContainer = Color(0xFFEAEDFF)
private val ListSurfaceContainerLow = Color(0xFFF2F3FF)
private val ListSecondaryContainer = Color(0xFFD5E4F8)
private val ListPrimaryFixed = Color(0xFFDBE1FF)
private val ListPrimaryFixedDim = Color(0xFFB5C4FF)
private val ListSurfaceContainerHigh = Color(0xFFE2E7FF)
private val ListTertiaryFixed = Color(0xFF89F5E7)
private val ListOnTertiaryFixedVariant = Color(0xFF005049)

private val avatarPalette = listOf(
    ListPrimaryFixed to LoginPrimary,
    ListSecondaryContainer to Color(0xFF0E1D2B),
    ListSurfaceContainerHigh to LoginPrimary,
    ListPrimaryFixedDim to Color(0xFF00174D)
)

private fun studentInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private fun formatBirthdayBadge(birthday: String): String? {
    if (birthday.isBlank()) return null
    return try {
        val parsed = java.time.LocalDate.parse(birthday)
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentListScreen(
    teacherName: String,
    students: List<StudentEntity>,
    query: String,
    selectedFilter: AgeFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (AgeFilter) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit,
    onAddStudent: () -> Unit,
    onOpenStudent: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LoginSurfaceCanvas
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Custom top app bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Student Registry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = LoginOnSurface
                    )
                    if (teacherName.isNotBlank()) {
                        Text(
                            text = teacherName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ListSecondary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onOpenExport) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = LoginOnSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LoginOnSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // --- Search bar ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = LoginSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LoginOutlineVariant),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ListSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LoginOnSurface),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search by name, contact, or email...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ListOutline
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Age filter chips ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgeFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            onClick = { onFilterChange(filter) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) ListSecondaryContainer else LoginSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (selected) LoginPrimary.copy(alpha = 0.3f) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LoginPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) LoginPrimary else ListSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- Student record list ---
                if (students.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No students found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LoginOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try a different search or filter, or add your first student.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ListSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
                    ) {
                        items(students, key = { it.id }) { student ->
                            val avatarIndex = kotlin.math.abs(student.id.hashCode()) % avatarPalette.size
                            val (avatarBg, avatarFg) = avatarPalette[avatarIndex]
                            val birthdayBadge = formatBirthdayBadge(student.birthday)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenStudent(student.id) },
                                shape = RoundedCornerShape(16.dp),
                                color = LoginSurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LoginOutlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(avatarBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = studentInitials(student.name),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = avatarFg
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = student.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoginOnSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (birthdayBadge != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = ListSurfaceContainer,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        text = birthdayBadge,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LoginOnSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Age: ${student.age}" +
                                                if (student.contact.isNotBlank()) " • ${student.contact}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ListSecondary
                                        )
                                        if (student.email.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = ListSurfaceContainerLow,
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ListSecondaryContainer)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Email,
                                                        contentDescription = null,
                                                        tint = LoginPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = student.email,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LoginPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = ListOutline,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = onAddStudent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LoginPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add student")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditStudentScreen(
    initialStudent: StudentEntity?,
    onBack: () -> Unit,
    onSave: (StudentFormInput) -> Unit
) {
    var name by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.name.orEmpty()) }
    var age by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.age?.toString().orEmpty()) }
    var contact by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.contact.orEmpty()) }
    var parentContact by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.parentContact.orEmpty()) }
    var email by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.email.orEmpty()) }
    var birthday by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.birthday.orEmpty()) }
    var notes by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.notes.orEmpty()) }
    val isEditing = initialStudent != null

    Surface(modifier = Modifier.fillMaxSize(), color = LoginSurfaceCanvas) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LoginOnSurface)
                }
                Text(
                    text = if (isEditing) "Edit Student" else "Add Student",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LoginOnSurface
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Info banner ---
                FormSectionCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(LoginPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = LoginPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEditing) "Update Academic Profile" else "New Academic Profile",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LoginOnSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Fill in student credentials for registration. Fields marked with * are required for enrollment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ListSecondary
                            )
                        }
                    }
                }

                // --- Identity Details ---
                FormSectionCard {
                    FormSectionHeader(icon = Icons.Default.Person, title = "Identity Details")
                    FormFieldLabel("Full Name", required = true)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Sunil Kumar") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        colors = formFieldColors()
                    )
                    FormFieldCaption("Legal first and family name")

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Age", required = true)
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter(Char::isDigit) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                colors = formFieldColors()
                            )
                            FormFieldCaption("Valid age")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Date of Birth", required = false)
                            OutlinedTextField(
                                value = birthday,
                                onValueChange = { birthday = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("yyyy-MM-dd") },
                                singleLine = true,
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                colors = formFieldColors()
                            )
                            FormFieldCaption("Format: YYYY-MM-DD")
                        }
                    }
                }

                // --- Contact Channels ---
                FormSectionCard {
                    FormSectionHeader(icon = Icons.Default.ContactPhone, title = "Contact Channels")

                    FormFieldLabel("Student Contact Number", required = false)
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("+91 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        colors = formFieldColors()
                    )
                    FormFieldCaption("Student personal mobile")

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Parent's Contact Number", required = false)
                    OutlinedTextField(
                        value = parentContact,
                        onValueChange = { parentContact = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("+91 9876543210") },
                        leadingIcon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        colors = formFieldColors()
                    )
                    FormFieldCaption("Primary parent or guardian mobile")

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Email Address", required = false)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("student@school.edu") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        ),
                        colors = formFieldColors()
                    )
                    FormFieldCaption("School or parent contact email")
                }

                // --- Notes & Observations ---
                FormSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormSectionHeader(icon = Icons.AutoMirrored.Filled.Notes, title = "Notes & Observations")
                        Text(
                            text = "${notes.length} / 300",
                            style = MaterialTheme.typography.labelSmall,
                            color = ListSecondary
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 300) notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("Add classroom observations, medical notes, or guardian preferences...") },
                        colors = formFieldColors()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confidential educator notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = ListSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ListSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = ListSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Bottom action bar ---
            Surface(color = LoginSurfaceCanvas, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = LoginPrimaryContainer,
                            contentColor = LoginPrimary
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                StudentFormInput(
                                    name = name,
                                    age = age,
                                    contact = contact,
                                    parentContact = parentContact,
                                    email = email,
                                    birthday = birthday,
                                    notes = notes
                                )
                            )
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = LoginPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEditing) "Update Student" else "Save Student")
                    }
                }
            }
        }
    }
}

@Composable
private fun formFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginPrimary,
    focusedLabelColor = LoginPrimary,
    focusedLeadingIconColor = LoginPrimary,
    cursorColor = LoginPrimary
)

@Composable
private fun FormSectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = LoginSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, LoginOutlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun FormSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = LoginOnSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LoginOnSurface
        )
    }
    androidx.compose.material3.HorizontalDivider(color = LoginOutlineVariant)
}

@Composable
private fun FormFieldLabel(text: String, required: Boolean) {
    Row {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = LoginOnSurface)
        if (required) {
            Text(" *", style = MaterialTheme.typography.labelLarge, color = Color(0xFFBA1A1A))
        }
    }
}

@Composable
private fun FormFieldCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = ListSecondary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailScreen(
    student: StudentEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete student") },
            text = { Text("Are you sure you want to delete ${student.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Student Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(student.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Age: ${student.age}")
            Text("Student contact: ${student.contact}")
            Text("Parent contact: ${student.parentContact}")
            Text("Email: ${student.email}")
            Text("Birthday: ${student.birthday}")
            Text("Notes: ${student.notes}")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit")
            }
            Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    darkTheme: Boolean,
    fontScale: Float,
    notificationsEnabled: Boolean,
    cloudSyncEnabled: Boolean,
    onBack: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onCloudSyncChanged: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingSwitchRow("Dark theme", darkTheme, onDarkThemeChanged)
            SettingSwitchRow("Birthday notifications", notificationsEnabled, onNotificationsChanged)
            SettingSwitchRow("Cloud backup sync", cloudSyncEnabled, onCloudSyncChanged)
            Text("Font size")
            Slider(
                value = fontScale,
                onValueChange = onFontScaleChanged,
                valueRange = 0.85f..1.3f
            )
            Text("Current scale: ${"%.2f".format(fontScale)}x")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChanged)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreen(
    filteredCount: Int,
    onBack: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Export uses the currently filtered student list.")
            Text("Records in scope: $filteredCount")
            Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                Text("Export CSV")
            }
            Button(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                Text("Export PDF")
            }
        }
    }
}
