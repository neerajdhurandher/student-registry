package com.studentregistry

import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studentregistry.export.ExportManager

@Composable
fun StudentRegistryNavGraph(
    viewModel: MainViewModel,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val user by viewModel.user.collectAsState()
    val schoolAccessState by viewModel.schoolAccessState.collectAsState()
    val sessionBootstrapComplete by viewModel.isSessionBootstrapComplete.collectAsState()
    val students by viewModel.students.collectAsState()
    val searchQuery by viewModel.currentSearchQuery.collectAsState()
    val studentListSort by viewModel.selectedStudentListSort.collectAsState()
    val context = LocalContext.current
    val exportManager = remember(context) { ExportManager(context) }

    if (!sessionBootstrapComplete) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Opening Student Registry...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

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
        },
        enterTransition = { forwardEnterTransition() },
        exitTransition = { forwardExitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() }
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
                selectedFilter = studentListSort,
                onQueryChange = viewModel::updateSearchQuery,
                onFilterChange = viewModel::updateStudentListSort,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenExport = { navController.navigate(Routes.EXPORT) },
                onExportSelectedCsv = { selectedStudents ->
                    viewModel.exportCsv(exportManager, selectedStudents)
                },
                onExportSelectedPdf = { selectedStudents ->
                    viewModel.exportPdf(exportManager, selectedStudents)
                },
                onDeleteSelected = viewModel::deleteStudents,
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

private fun forwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = 320),
        initialOffsetX = { it / 6 }
    ) + fadeIn(animationSpec = tween(durationMillis = 220))

private fun forwardExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = 280),
        targetOffsetX = { -it / 12 }
    ) + fadeOut(animationSpec = tween(durationMillis = 180))

private fun popEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = 320),
        initialOffsetX = { -it / 6 }
    ) + fadeIn(animationSpec = tween(durationMillis = 220))

private fun popExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = 280),
        targetOffsetX = { it / 12 }
    ) + fadeOut(animationSpec = tween(durationMillis = 180))
