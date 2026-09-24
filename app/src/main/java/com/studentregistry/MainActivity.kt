package com.studentregistry

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.studentregistry.data.AppDatabase
import com.studentregistry.data.SettingsRepository
import com.studentregistry.data.SchoolRepository
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
        val schoolRepository = SchoolRepository(applicationContext, database.schoolDao(), database.studentDao())
        val settingsRepository = SettingsRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(applicationContext, studentRepository, schoolRepository, settingsRepository)
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
                        FirebaseAuth.getInstance().signOut()
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
            val idToken = account?.idToken
            if (teacher == null || idToken.isNullOrBlank()) {
                viewModel.postMessage("Unable to read your Google account.")
                return
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener {
                    viewModel.setSignedInUser(teacher)
                }
                .addOnFailureListener { error ->
                    viewModel.postMessage(error.message ?: "Firebase sign-in failed.")
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
    return TeacherUser(
        id = email,
        name = displayName ?: email.substringBefore("@"),
        email = email
    )
}

private object Routes {
    const val LOGIN: String = "login"
    const val SCHOOL_ACCESS: String = "school-access"
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
    val schoolAccessState by viewModel.schoolAccessState.collectAsState()
    val students by viewModel.students.collectAsState()
    val searchQuery by viewModel.currentSearchQuery.collectAsState()
    val ageFilter by viewModel.selectedAgeFilter.collectAsState()
    val context = LocalContext.current
    val exportManager = remember(context) { ExportManager(context) }

    LaunchedEffect(user, schoolAccessState.school, schoolAccessState.loading) {
        when {
            user == null -> navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            schoolAccessState.loading -> Unit
            schoolAccessState.school == null -> navController.navigate(Routes.SCHOOL_ACCESS) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
            else -> navController.navigate(Routes.STUDENT_LIST) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = when {
            user == null -> Routes.LOGIN
            schoolAccessState.loading -> Routes.LOGIN
            schoolAccessState.school == null -> Routes.SCHOOL_ACCESS
            else -> Routes.STUDENT_LIST
        }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onGoogleSignIn = onGoogleSignIn,
                onAdminLogin = {
                    context.startActivity(Intent(context, AdminActivity::class.java))
                }
            )
        }

        composable(Routes.SCHOOL_ACCESS) {
            SchoolAccessScreen(
                teacherName = user?.name.orEmpty(),
                teacherEmail = user?.email.orEmpty(),
                requestNote = schoolAccessState.requestNote,
                onRequestNoteChange = viewModel::updateSchoolRequestNote,
                onSubmitRequest = viewModel::submitSchoolAccessRequest,
                onSignOut = onGoogleSignOut
            )
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
                    schoolName = schoolAccessState.school?.name.orEmpty(),
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
private const val ADMIN_PASSWORD = "1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(onGoogleSignIn: () -> Unit, onAdminLogin: () -> Unit) {
    var isSigningIn by remember { mutableStateOf(false) }
    var showAdminDialog by rememberSaveable { mutableStateOf(false) }
    var adminPassword by rememberSaveable { mutableStateOf("") }
    var adminError by rememberSaveable { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    val titleColor = colorScheme.onSurface
    val titleColorVariant = colorScheme.onSurfaceVariant
    val outlineColor = colorScheme.outlineVariant

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
                            tint = colorScheme.primary,
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
                        tint = colorScheme.primary,
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
                color = titleColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Securely manage your students' records, contact " +
                    "details, and birthdays — all backed up to your Google account.",
                style = MaterialTheme.typography.bodyMedium,
                color = titleColorVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- Quick context card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LoginSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
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
                            tint = colorScheme.primary,
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
                                color = titleColor
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
                            color = titleColorVariant
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
                    containerColor = colorScheme.primary,
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
                color = titleColorVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { showAdminDialog = true }) {
                Text("Admin login")
            }
        }
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminDialog = false
                adminPassword = ""
                adminError = null
            },
            title = { Text("Admin login") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter admin password to continue.")
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            adminError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        ),
                        isError = adminError != null,
                        colors = formFieldColors()
                    )
                    if (adminError != null) {
                        Text(adminError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (adminPassword == ADMIN_PASSWORD) {
                        showAdminDialog = false
                        adminPassword = ""
                        adminError = null
                        onAdminLogin()
                    } else {
                        adminError = "Invalid admin password."
                    }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminDialog = false
                    adminPassword = ""
                    adminError = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SchoolAccessScreen(
    teacherName: String,
    teacherEmail: String,
    requestNote: String,
    onRequestNoteChange: (String) -> Unit,
    onSubmitRequest: () -> Unit,
    onSignOut: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ask admin to add your school access",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hi $teacherName ($teacherEmail). You are signed in, but not assigned to any school yet. Ask your admin to add your email to the school's teacher list.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = requestNote,
                onValueChange = onRequestNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter school name, invite code, or admin details") },
                singleLine = false,
                minLines = 3,
                colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmitRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = Color.White)
            ) {
                Text("Send request")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
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
    val colorScheme = MaterialTheme.colorScheme
    val screenColor = colorScheme.background
    val cardColor = colorScheme.surface
    val elevatedCardColor = colorScheme.surfaceContainerLow
    val outlineColor = colorScheme.outlineVariant
    val primaryTint = colorScheme.primary.copy(alpha = 0.14f)
    val titleColor = colorScheme.onSurface
    val secondaryTextColor = colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = screenColor
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
                        color = titleColor
                    )
                    if (teacherName.isNotBlank()) {
                        Text(
                            text = teacherName,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }
                Row {
                    IconButton(onClick = onOpenExport) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = secondaryTextColor
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = secondaryTextColor
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
                    color = cardColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
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
                            tint = secondaryTextColor,
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = titleColor),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search by name, contact, or email...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = secondaryTextColor
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
                            color = if (selected) primaryTint else cardColor,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (selected) colorScheme.primary.copy(alpha = 0.3f) else outlineColor
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
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) colorScheme.primary else secondaryTextColor
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
                            color = titleColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try a different search or filter, or add your first student.",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
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
                                color = cardColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
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
                                                color = titleColor,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (birthdayBadge != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = primaryTint,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        text = birthdayBadge,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = secondaryTextColor
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Age: ${student.age}" +
                                                if (student.contact.isNotBlank()) " • ${student.contact}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryTextColor
                                        )
                                        if (student.email.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = colorScheme.surfaceContainerLow,
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Email,
                                                        contentDescription = null,
                                                        tint = colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = student.email,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = secondaryTextColor,
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
            containerColor = colorScheme.primary,
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
    var aadhaarNumber by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.aadhaarNumber.orEmpty()) }
    var apparId by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.apparId.orEmpty()) }
    var penId by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.penId.orEmpty()) }
    var fatherName by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.fatherName.orEmpty()) }
    var motherName by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.motherName.orEmpty()) }
    var religion by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.religion.orEmpty()) }
    var caste by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.caste.orEmpty()) }
    var bloodGroup by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.bloodGroup.orEmpty()) }
    var admissionNumber by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.admissionNumber.orEmpty()) }
    var admissionDate by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.admissionDate.orEmpty()) }
    var bankNameAddress by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.bankNameAddress.orEmpty()) }
    var email by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.email.orEmpty()) }
    var birthday by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.birthday.orEmpty()) }
    var notes by rememberSaveable(initialStudent?.id) { mutableStateOf(initialStudent?.notes.orEmpty()) }
    var attemptedSave by rememberSaveable(initialStudent?.id) { mutableStateOf(false) }
    var showBirthdayPicker by rememberSaveable { mutableStateOf(false) }
    var showAdmissionDatePicker by rememberSaveable { mutableStateOf(false) }
    val isEditing = initialStudent != null

    // Age is derived from Date of Birth and cannot be edited by hand.
    LaunchedEffect(birthday) {
        age = calculateAgeFromBirthday(birthday)
    }
    val formInput = StudentFormInput(
        name = name,
        age = age,
        contact = contact,
        parentContact = parentContact,
        aadhaarNumber = aadhaarNumber,
        apparId = apparId,
        penId = penId,
        fatherName = fatherName,
        motherName = motherName,
        religion = religion,
        caste = caste,
        bloodGroup = bloodGroup,
        admissionNumber = admissionNumber,
        admissionDate = admissionDate,
        bankNameAddress = bankNameAddress,
        email = email,
        birthday = birthday,
        notes = notes
    )
    val validationErrors = validateStudentForm(formInput)
    val nameError = visibleFieldError(validationErrors.name, name, attemptedSave)
    val ageError = visibleFieldError(validationErrors.age, age, attemptedSave)
    val birthdayError = visibleFieldError(validationErrors.birthday, birthday, attemptedSave)
    val contactError = visibleFieldError(validationErrors.contact, contact, attemptedSave)
    val parentContactError = visibleFieldError(validationErrors.parentContact, parentContact, attemptedSave)
    val emailError = visibleFieldError(validationErrors.email, email, attemptedSave)
    val aadhaarError = visibleFieldError(validationErrors.aadhaarNumber, aadhaarNumber, attemptedSave)
    val apparIdError = visibleFieldError(validationErrors.apparId, apparId, attemptedSave)
    val penIdError = visibleFieldError(validationErrors.penId, penId, attemptedSave)
    val fatherNameError = visibleFieldError(validationErrors.fatherName, fatherName, attemptedSave)
    val motherNameError = visibleFieldError(validationErrors.motherName, motherName, attemptedSave)
    val bloodGroupError = visibleFieldError(validationErrors.bloodGroup, bloodGroup, attemptedSave)
    val admissionDateError = visibleFieldError(validationErrors.admissionDate, admissionDate, attemptedSave)
    val bankNameAddressError = visibleFieldError(validationErrors.bankNameAddress, bankNameAddress, attemptedSave)
    val notesError = visibleFieldError(validationErrors.notes, notes, attemptedSave)
    val colorScheme = MaterialTheme.colorScheme
    val screenColor = colorScheme.background
    val cardColor = colorScheme.surface
    val outlineColor = colorScheme.outlineVariant
    val titleColor = colorScheme.onSurface
    val secondaryTextColor = colorScheme.onSurfaceVariant
    val primaryTint = colorScheme.primary.copy(alpha = 0.14f)

    Surface(modifier = Modifier.fillMaxSize(), color = screenColor) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = titleColor)
                }
                Text(
                    text = if (isEditing) "Edit Student" else "Add Student",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
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
                                .background(primaryTint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEditing) "Update Academic Profile" else "New Academic Profile",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Fill in student credentials for registration. Fields marked with * are required for enrollment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                if (!isEditing) {
                    RandomStudentDebugAction(
                        applyRandomStudent = { randomStudent ->
                            name = randomStudent.name
                            contact = randomStudent.contact
                            parentContact = randomStudent.parentContact
                            aadhaarNumber = randomStudent.aadhaarNumber
                            apparId = randomStudent.apparId
                            penId = randomStudent.penId
                            fatherName = randomStudent.fatherName
                            motherName = randomStudent.motherName
                            religion = randomStudent.religion
                            caste = randomStudent.caste
                            bloodGroup = randomStudent.bloodGroup
                            admissionNumber = randomStudent.admissionNumber
                            admissionDate = randomStudent.admissionDate
                            bankNameAddress = randomStudent.bankNameAddress
                            email = randomStudent.email
                            birthday = randomStudent.birthday
                            notes = randomStudent.notes
                            attemptedSave = false
                        }
                    )
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
                        isError = nameError != null,
                        colors = formFieldColors()
                    )
                    FormFieldCaption(nameError ?: "Legal first and family name", isError = nameError != null)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Date of Birth", required = true)
                            OutlinedTextField(
                                value = birthday,
                                onValueChange = { birthday = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("yyyy-MM-dd") },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showBirthdayPicker = true }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date of birth", modifier = Modifier.size(18.dp))
                                    }
                                },
                                isError = birthdayError != null,
                                colors = formFieldColors()
                            )
                            FormFieldCaption(birthdayError ?: "Format: YYYY-MM-DD", isError = birthdayError != null)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Age", required = true)
                            OutlinedTextField(
                                value = age,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                isError = ageError != null,
                                colors = formFieldColors()
                            )
                            FormFieldCaption(ageError ?: "Auto-calculated from date of birth", isError = ageError != null)
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
                        isError = contactError != null,
                        colors = formFieldColors()
                    )
                    FormFieldCaption(contactError ?: "Student personal mobile", isError = contactError != null)

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
                        isError = parentContactError != null,
                        colors = formFieldColors()
                    )
                    FormFieldCaption(parentContactError ?: "Primary parent or guardian mobile", isError = parentContactError != null)

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
                        isError = emailError != null,
                        colors = formFieldColors()
                    )
                    FormFieldCaption(emailError ?: "School or parent contact email", isError = emailError != null)
                }

                FormSectionCard {
                    FormSectionHeader(icon = Icons.Default.Badge, title = "Academic & Identity IDs")

                    FormFieldLabel("Student Aadhaar Card No.", required = false)
                    OutlinedTextField(
                        value = aadhaarNumber,
                        onValueChange = { aadhaarNumber = it.filter(Char::isDigit).take(12) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("12-digit Aadhaar number") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        isError = aadhaarError != null,
                        colors = formFieldColors()
                    )
                    if (aadhaarError != null) {
                        FormFieldCaption(aadhaarError, isError = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("APPAR ID", required = false)
                    OutlinedTextField(
                        value = apparId,
                        onValueChange = { apparId = it.filter(Char::isDigit).take(12) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("12-digit APPAR ID") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        isError = apparIdError != null,
                        colors = formFieldColors()
                    )
                    if (apparIdError != null) {
                        FormFieldCaption(apparIdError, isError = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("PEN ID", required = false)
                    OutlinedTextField(
                        value = penId,
                        onValueChange = { penId = it.filter(Char::isDigit).take(11) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("11-digit PEN ID") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        isError = penIdError != null,
                        colors = formFieldColors()
                    )
                    if (penIdError != null) {
                        FormFieldCaption(penIdError, isError = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Admission No.", required = false)
                    OutlinedTextField(
                        value = admissionNumber,
                        onValueChange = { admissionNumber = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter admission number") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        isError = false,
                        colors = formFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Date of Admission", required = false)
                    OutlinedTextField(
                        value = admissionDate,
                        onValueChange = { admissionDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("yyyy-MM-dd") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showAdmissionDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick admission date", modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        isError = admissionDateError != null,
                        colors = formFieldColors()
                    )
                    FormFieldCaption(admissionDateError ?: "Format: YYYY-MM-DD", isError = admissionDateError != null)
                }


                FormSectionCard {
                    FormSectionHeader(icon = Icons.Default.SupervisorAccount, title = "Family & Personal Details")

                    FormFieldLabel("Father's Name", required = false)
                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter father's name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        isError = aadhaarError != null,
                        colors = formFieldColors()
                    )
                    if (aadhaarError != null) {
                        FormFieldCaption(aadhaarError, isError = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Mother's Name", required = false)
                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter mother's name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        isError = aadhaarError != null,
                        colors = formFieldColors()
                    )
                    if (aadhaarError != null) {
                        FormFieldCaption(aadhaarError, isError = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Religion", required = false)
                            OutlinedTextField(
                                value = religion,
                                onValueChange = { religion = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = formFieldColors()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FormFieldLabel("Caste", required = false)
                            OutlinedTextField(
                                value = caste,
                                onValueChange = { caste = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = formFieldColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormFieldLabel("Blood Group", required = false)
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., O+") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        isError = bloodGroupError != null,
                        colors = formFieldColors()
                    )
                    if (bloodGroupError != null) {
                        FormFieldCaption(bloodGroupError, isError = true)
                    }
                }

                FormSectionCard {
                    FormSectionHeader(icon = Icons.Default.Badge, title = "Bank Information")
                    FormFieldLabel("Bank Name & Address", required = false)
                    OutlinedTextField(
                        value = bankNameAddress,
                        onValueChange = { bankNameAddress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        placeholder = { Text("Enter bank name, branch, and address") },
                        isError = bankNameAddressError != null,
                        colors = formFieldColors()
                    )
                    if (bankNameAddressError != null) {
                        FormFieldCaption(bankNameAddressError, isError = true)
                    }
                }

                // --- Notes & Observations ---
                FormSectionCard {
                    FormSectionHeader(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        title = "Notes & Observations",
                        trailing = {
                            Text(
                                text = "${notes.length} / 300",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor
                            )
                        }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 300) notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("Add classroom observations, medical notes, or guardian preferences...") },
                        isError = notesError != null,
                        colors = formFieldColors()
                    )
                    if (notesError != null) {
                        FormFieldCaption(notesError, isError = true)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confidential educator notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Bottom action bar ---
            Surface(color = screenColor, shadowElevation = 8.dp) {
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
                            containerColor = primaryTint,
                            contentColor = colorScheme.primary
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            attemptedSave = true
                            if (validationErrors.hasErrors) {
                                return@Button
                            }
                            onSave(formInput)
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = Color.White),
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

    if (showBirthdayPicker) {
        val birthdayPickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(birthday) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthdayPickerState.selectedDateMillis?.let { millis ->
                        birthday = utcMillisToIsoDate(millis)
                    }
                    showBirthdayPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = birthdayPickerState)
        }
    }

    if (showAdmissionDatePicker) {
        val admissionDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(admissionDate) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showAdmissionDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    admissionDatePickerState.selectedDateMillis?.let { millis ->
                        admissionDate = utcMillisToIsoDate(millis)
                    }
                    showAdmissionDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showAdmissionDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = admissionDatePickerState)
        }
    }
}

private fun calculateAgeFromBirthday(birthday: String): String {
    return runCatching {
        val birthDate = java.time.LocalDate.parse(birthday)
        val today = java.time.LocalDate.now()
        if (birthDate.isAfter(today)) return ""
        java.time.Period.between(birthDate, today).years
    }.getOrNull()?.toString().orEmpty()
}

private fun isoDateToUtcMillis(isoDate: String): Long? = runCatching {
    java.time.LocalDate.parse(isoDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

private fun utcMillisToIsoDate(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()

@Composable
private fun formFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = LoginPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedLeadingIconColor = LoginPrimary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    focusedTrailingIconColor = LoginPrimary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = LoginPrimary,
    errorCursorColor = MaterialTheme.colorScheme.error
)

@Composable
private fun FormSectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun FormSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        trailing?.invoke()
    }
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FormFieldLabel(text: String, required: Boolean) {
    Row {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (required) {
            Text(" *", style = MaterialTheme.typography.labelLarge, color = Color(0xFFBA1A1A))
        }
    }
}

private fun visibleFieldError(error: String?, value: String, attemptedSave: Boolean): String? {
    if (error == null) return null
    return if (attemptedSave || value.isNotBlank()) error else null
}

@Composable
private fun FormFieldCaption(text: String, isError: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailScreen(
    student: StudentEntity,
    schoolName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var aadhaarMasked by remember(student.id) { mutableStateOf(true) }
    val context = LocalContext.current

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = "Delete Student",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold
                )
            },
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
                title = {
                    Text(
                        text = "Student Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareStudentDetails(context, student) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share student details")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFFBFB),
                            contentColor = Color(0xFFDC2626)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text("Delete Student")
                    }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Record")
                    }
                }
            }
        }
    ) { paddingValues ->
        val contentScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(contentScroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudentDetailAffiliationCard(
                schoolName = schoolName.ifBlank { "Academic Institution" },
                schoolCode = "School #${student.schoolId.take(8)}"
            )

            StudentOverviewCard(student = student)

            StudentDetailSection(
                title = "Government & Academic IDs",
                icon = Icons.Default.Badge,
                trailingLabel = "Official"
            ) {
                StudentInfoItem(
                    label = "APAAR ID (One Nation One Student ID)",
                    value = detailValueOrFallback(student.apparId, "Not added"),
                    supporting = null
                )
                StudentInfoItem(
                    label = "Permanent Education No. (PEN)",
                    value = detailValueOrFallback(student.penId, "Not added"),
                    supporting = null
                )
                StudentInfoItem(
                    label = "Aadhaar Number",
                    value = when {
                        student.aadhaarNumber.isBlank() -> "Not added"
                        aadhaarMasked -> maskAadhaar(student.aadhaarNumber)
                        else -> student.aadhaarNumber
                    },
                    supporting = null,
                    trailingActions = {
                        if (student.aadhaarNumber.isNotBlank()) {
                            StudentActionIcon(
                                icon = if (aadhaarMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (aadhaarMasked) "Show Aadhaar number" else "Hide Aadhaar number",
                                onClick = { aadhaarMasked = !aadhaarMasked }
                            )
                        }
                    }
                )
                StudentInfoItem(
                    label = "Admission No. & Date",
                    value = detailValueOrFallback(student.admissionNumber, "Admission number pending"),
                    supporting = student.admissionDate.takeIf { it.isNotBlank() }?.let { "Enrolled ${formatStudentDate(it)}" },
                    supportingColor = MaterialTheme.colorScheme.outline
                )
            }

            StudentDetailSection(
                title = "Contact & Communication",
                icon = Icons.Default.ContactPhone,
                trailingLabel = "Verified"
            ) {
                StudentInfoItem(
                    label = "Student Contact Phone",
                    value = detailValueOrFallback(student.contact, "No student phone added"),
                    supporting = null,
                    trailingActions = {
                        if (student.contact.isNotBlank()) {
                            StudentActionIcon(
                                icon = Icons.Default.Phone,
                                contentDescription = "Call student",
                                onClick = { openDialer(context, student.contact) }
                            )
                            StudentWhatsAppActionIcon(
                                contentDescription = "Message on WhatsApp",
                                onClick = { openWhatsApp(context, student.contact) }
                            )
                        }
                    }
                )
                StudentInfoItem(
                    label = "Academic & Student Email",
                    value = detailValueOrFallback(student.email, "No email added"),
                    supporting = null,
                    trailingActions = {
                        if (student.email.isNotBlank()) {
                            StudentActionIcon(
                                icon = Icons.Default.Email,
                                contentDescription = "Open email app",
                                onClick = { openEmailApp(context, student.email) }
                            )
                        }
                    }
                )
            }

            StudentDetailSection(
                title = "Personal & Demographics",
                icon = Icons.Default.Person,
                trailingTint = Color(0xFFFFE4E0),
                trailingContentColor = Color(0xFFDC2626),
                trailingLabel = student.bloodGroup.takeIf { it.isNotBlank() }?.let { "Blood Group: $it" } ?: "Profile"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentMiniInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Birthday",
                        value = detailValueOrFallback(formatStudentDate(student.birthday), "Not added"),
                        supporting = "Official Certificate"
                    )
                    StudentMiniInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Age",
                        value = if (student.age > 0) "${student.age} Years" else "Not added",
                        supporting = "As of Academic Year"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentMiniInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Religion",
                        value = detailValueOrFallback(student.religion, "Not declared"),
                        supporting = "Declared in Profile"
                    )
                    StudentMiniInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Caste / Category",
                        value = detailValueOrFallback(student.caste, "Not declared"),
                        supporting = "Quota: Non-Reserved"
                    )
                }
            }

            StudentDetailSection(
                title = "Family & Parent Details",
                icon = Icons.Default.SupervisorAccount,
                trailingLabel = "Emergency Contact"
            ) {
                StudentInfoItem(
                    label = "Father's Name",
                    value = detailValueOrFallback(student.fatherName, "Not added"),
                    supporting = null
                )
                StudentInfoItem(
                    label = "Mother's Name",
                    value = detailValueOrFallback(student.motherName, "Not added"),
                    supporting = null
                )
                StudentInfoItem(
                    label = "Parent Contact Phone",
                    value = detailValueOrFallback(student.parentContact, "No parent number added"),
                    supporting = buildString {
                        val names = listOf(student.fatherName, student.motherName).filter { it.isNotBlank() }
                        if (names.isNotEmpty()) append(names.joinToString(" / ", prefix = "(" , postfix = ")"))
                    }.ifBlank { null }
                )
            }

            StudentDetailSection(
                title = "Banking Information",
                icon = Icons.Default.Badge,
                trailingLabel = "DBT Linked"
            ) {
                StudentInfoItem(
                    label = "Registered Bank Name & Address",
                    value = detailValueOrFallback(student.bankNameAddress, "No bank details added"),
                    supporting = "Linked for institutional stipends and state merit scholarships"
                )
            }

            StudentDetailSection(
                title = "Educator Notes & Observations",
                icon = Icons.AutoMirrored.Filled.Notes,
                trailingLabel = student.updatedAt.takeIf { it > 0 }?.let { "Updated ${formatRelativeUpdate(it)}" } ?: "Updated recently"
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = detailValueOrFallback(student.notes, "No educator notes added yet."),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded by school staff",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Private Note",
                                style = MaterialTheme.typography.labelLarge,
                                color = LoginPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentDetailAffiliationCard(
    schoolName: String,
    schoolCode: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LoginPrimary.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = LoginPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AFFILIATED INSTITUTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = schoolName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = LoginPrimary.copy(alpha = 0.10f)
            ) {
                Text(
                    text = schoolCode,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = LoginPrimary
                )
            }
        }
    }
}

@Composable
private fun StudentOverviewCard(student: StudentEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, LoginPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = studentInitials(student.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = LoginPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append("Grade 10")
                            if (student.caste.isNotBlank()) append(" • ${student.caste}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudentPillChip(
                            text = "Active & Verified Record",
                            container = Color(0xFFE6F7F3),
                            content = Color(0xFF0F766E)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudentPillChip(
                    text = detailValueOrFallback(student.admissionNumber, "Admission ID Pending"),
                    modifier = Modifier.weight(1f)
                )
                StudentPillChip(
                    text = student.admissionDate.takeIf { it.isNotBlank() }?.let { "Enrolled • ${formatAcademicYear(it)}" }
                        ?: "Enrollment Pending",
                    modifier = Modifier.weight(1f)
                )
            }

            StudentPillChip(
                text = buildString {
                    append(if (student.age > 0) "${student.age} Years" else "Age Pending")
                    student.birthday.takeIf { it.isNotBlank() }?.let {
                        append(" • ")
                        append(formatStudentDate(it))
                    }
                }
            )
        }
    }
}

@Composable
private fun StudentDetailSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingLabel: String? = null,
    trailingTint: Color = LoginPrimary.copy(alpha = 0.10f),
    trailingContentColor: Color = LoginPrimary,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(LoginPrimary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = LoginPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                trailingLabel?.let {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = trailingTint
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = trailingContentColor
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun StudentInfoItem(
    label: String,
    value: String,
    supporting: String?,
    supportingColor: Color = Color.Unspecified,
    trailingActions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    val resolvedSupportingColor = if (supportingColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        supportingColor
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = resolvedSupportingColor
                    )
                }
            }
            trailingActions?.invoke(this)
        }
    }
}

@Composable
private fun StudentActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LoginPrimary
        )
    }
}

@Composable
private fun StudentWhatsAppActionIcon(
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = Color(0xFF25D366)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "WA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StudentMiniInfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    supporting: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StudentPillChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = LoginPrimary.copy(alpha = 0.10f),
    content: Color = LoginPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

private fun detailValueOrFallback(value: String, fallback: String): String =
    value.ifBlank { fallback }

private fun shareStudentDetails(context: android.content.Context, student: com.studentregistry.data.StudentEntity) {
    val studentInfo = buildString {
        appendLine("Student Details")
        appendLine("==============================")
        appendLine("\nName: ${student.name}")
        if (student.birthday.isNotBlank()) {
            appendLine("Date of Birth: ${formatStudentDate(student.birthday)}")
        }
        appendLine("Age: ${student.age}")
        if (student.email.isNotBlank()) {
            appendLine("Email: ${student.email}")
        }
        if (student.contact.isNotBlank()) {
            appendLine("Contact: ${student.contact}")
        }
        if (student.parentContact.isNotBlank()) {
            appendLine("Parent Contact: ${student.parentContact}")
        }
        if (student.caste.isNotBlank()) {
            appendLine("Caste: ${student.caste}")
        }
        if (student.religion.isNotBlank()) {
            appendLine("Religion: ${student.religion}")
        }
        if (student.bloodGroup.isNotBlank()) {
            appendLine("Blood Group: ${student.bloodGroup}")
        }
        if (student.aadhaarNumber.isNotBlank()) {
            appendLine("Aadhaar: ${maskAadhaar(student.aadhaarNumber)}")
        }
        if (student.admissionNumber.isNotBlank()) {
            appendLine("Admission No: ${student.admissionNumber}")
        }
        if (student.admissionDate.isNotBlank()) {
            appendLine("Admission Date: ${formatStudentDate(student.admissionDate)}")
        }
        if (student.fatherName.isNotBlank()) {
            appendLine("Father: ${student.fatherName}")
        }
        if (student.motherName.isNotBlank()) {
            appendLine("Mother: ${student.motherName}")
        }
        if (student.bankNameAddress.isNotBlank()) {
            appendLine("Bank: ${student.bankNameAddress}")
        }
        if (student.notes.isNotBlank()) {
            appendLine("\nNotes: ${student.notes}")
        }
    }
    
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, studentInfo)
            putExtra(Intent.EXTRA_SUBJECT, "Student Details - ${student.name}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Student Details"))
    } catch (e: Exception) {
        android.util.Log.e("StudentDetail", "Failed to share student details", e)
    }
}



private fun maskAadhaar(value: String): String {
    val digits = value.filter(Char::isDigit)
    return if (digits.length < 4) "Masked" else "•••• •••• ${digits.takeLast(4)}"
}

private fun formatStudentDate(value: String): String = runCatching {
    val date = java.time.LocalDate.parse(value)
    date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
}.getOrDefault(value.ifBlank { "Not added" })

private fun formatAcademicYear(value: String): String = runCatching {
    val date = java.time.LocalDate.parse(value)
    val year = date.year
    "${year}-${(year + 1).toString().takeLast(2)}"
}.getOrDefault(value)

private fun formatRelativeUpdate(updatedAt: Long): String {
    val days = java.time.Duration.ofMillis(
        (System.currentTimeMillis() - updatedAt).coerceAtLeast(0L)
    ).toDays()
    return when {
        days <= 0 -> "today"
        days == 1L -> "1d ago"
        else -> "${days}d ago"
    }
}

private fun openDialer(context: android.content.Context, phoneNumber: String) {
    val normalized = phoneNumber.filter { it.isDigit() || it == '+' }
    if (normalized.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$normalized")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("StudentDetail", "Failed to open dialer", e)
    }
}

private fun openWhatsApp(context: android.content.Context, phoneNumber: String) {
    val digits = phoneNumber.filter(Char::isDigit)
    if (digits.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$digits")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("StudentDetail", "Failed to open WhatsApp", e)
    }
}

private fun openEmailApp(context: android.content.Context, email: String) {
    if (email.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("StudentDetail", "Failed to open email app", e)
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
