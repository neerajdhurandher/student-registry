package com.studentregistry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
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
