package com.studentregistry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SchoolAccessScreen(
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
