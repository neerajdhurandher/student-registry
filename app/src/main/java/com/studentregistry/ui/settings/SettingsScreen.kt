package com.studentregistry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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
