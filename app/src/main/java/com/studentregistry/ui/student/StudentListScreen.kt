package com.studentregistry

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentregistry.data.StudentEntity
import com.studentregistry.model.StudentListSort

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    teacherName: String,
    students: List<StudentEntity>,
    query: String,
    selectedFilter: StudentListSort,
    onQueryChange: (String) -> Unit,
    onFilterChange: (StudentListSort) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit,
    onExportSelectedCsv: (List<StudentEntity>) -> Unit,
    onExportSelectedPdf: (List<StudentEntity>) -> Unit,
    onDeleteSelected: (List<StudentEntity>) -> Unit,
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
    var selectedStudentIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    val selectionMode = selectedStudentIds.isNotEmpty()
    val selectedStudents = remember(students, selectedStudentIds) {
        students.filter { it.id in selectedStudentIds }
    }

    LaunchedEffect(students) {
        val visibleIds = students.map { it.id }.toSet()
        selectedStudentIds = selectedStudentIds.filter { it in visibleIds }
    }

    fun toggleSelection(studentId: String) {
        selectedStudentIds = if (studentId in selectedStudentIds) {
            selectedStudentIds - studentId
        } else {
            selectedStudentIds + studentId
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = screenColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
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
                        text = if (selectionMode) "${selectedStudentIds.size} selected" else "Student Registry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    if (!selectionMode && teacherName.isNotBlank()) {
                        Text(
                            text = teacherName,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }
                Row {
                    if (selectionMode) {
                        IconButton(onClick = { showExportOptions = true }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export selected students",
                                tint = secondaryTextColor
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected students",
                                tint = colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            selectedStudentIds = emptyList()
                            showExportOptions = false
                            confirmDelete = false
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear selection",
                                tint = secondaryTextColor
                            )
                        }
                    } else {
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
                        BasicTextField(
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
                                        text = "Search by name, APAAR ID, admission no., or email ...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = secondaryTextColor
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (showSortMenu || selectedFilter != StudentListSort.ALL) {
                                        primaryTint
                                    } else {
                                        Color.Transparent
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter students",
                                        tint = if (showSortMenu || selectedFilter != StudentListSort.ALL) {
                                            colorScheme.primary
                                        } else {
                                            secondaryTextColor
                                        },
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                StudentListSort.entries.forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label) },
                                        onClick = {
                                            onFilterChange(filter)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (selectedFilter == filter) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                }
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
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(students, key = { it.id }) { student ->
                            val avatarIndex = kotlin.math.abs(student.id.hashCode()) % avatarPalette.size
                            val (avatarBg, avatarFg) = avatarPalette[avatarIndex]
                            val birthdayBadge = formatBirthdayBadge(student.birthday)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                toggleSelection(student.id)
                                            } else {
                                                onOpenStudent(student.id)
                                            }
                                        },
                                        onLongClick = { toggleSelection(student.id) }
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                color = if (student.id in selectedStudentIds) elevatedCardColor else cardColor,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (student.id in selectedStudentIds) colorScheme.primary else outlineColor
                                )
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
                                    if (student.id in selectedStudentIds) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    } else if (!selectionMode) {
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
    }

        if (!selectionMode) {
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

    if (showExportOptions && selectedStudents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showExportOptions = false },
            title = { Text("Export selected students") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Export ${selectedStudents.size} selected students.")
                    Button(
                        onClick = {
                            onExportSelectedCsv(selectedStudents)
                            showExportOptions = false
                            selectedStudentIds = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export CSV")
                    }
                    Button(
                        onClick = {
                            onExportSelectedPdf(selectedStudents)
                            showExportOptions = false
                            selectedStudentIds = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export PDF")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (confirmDelete && selectedStudents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete selected students") },
            text = { Text("Delete ${selectedStudents.size} selected students? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelected(selectedStudents)
                    confirmDelete = false
                    selectedStudentIds = emptyList()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
