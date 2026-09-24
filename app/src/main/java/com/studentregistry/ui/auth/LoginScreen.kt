package com.studentregistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

private const val ADMIN_PASSWORD = "1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onGoogleSignIn: () -> Unit, onAdminLogin: () -> Unit) {
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
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
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

