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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
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
