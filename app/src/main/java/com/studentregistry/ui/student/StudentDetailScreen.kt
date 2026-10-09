package com.studentregistry

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentregistry.data.StudentEntity

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
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
                    label = "Father's Phone Number",
                    value = detailValueOrFallback(student.fatherPhone, "No father's number added"),
                    supporting = null,
                    trailingActions = {
                        if (student.fatherPhone.isNotBlank()) {
                            StudentActionIcon(
                                icon = Icons.Default.Phone,
                                contentDescription = "Call father",
                                onClick = { openDialer(context, student.fatherPhone) }
                            )
                            StudentWhatsAppActionIcon(
                                contentDescription = "Message father on WhatsApp",
                                onClick = { openWhatsApp(context, student.fatherPhone) }
                            )
                        }
                    }
                )
                StudentInfoItem(
                    label = "Mother's Name",
                    value = detailValueOrFallback(student.motherName, "Not added"),
                    supporting = null
                )
                StudentInfoItem(
                    label = "Mother's Phone Number",
                    value = detailValueOrFallback(student.motherPhone, "No mother's number added"),
                    supporting = null,
                    trailingActions = {
                        if (student.motherPhone.isNotBlank()) {
                            StudentActionIcon(
                                icon = Icons.Default.Phone,
                                contentDescription = "Call mother",
                                onClick = { openDialer(context, student.motherPhone) }
                            )
                            StudentWhatsAppActionIcon(
                                contentDescription = "Message mother on WhatsApp",
                                onClick = { openWhatsApp(context, student.motherPhone) }
                            )
                        }
                    }
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
        Icon(
            painter = painterResource(id = R.drawable.ic_whatsapp),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
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

private fun shareStudentDetails(context: Context, student: StudentEntity) {
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
        if (student.fatherPhone.isNotBlank()) {
            appendLine("Father Phone: ${student.fatherPhone}")
        }
        if (student.motherPhone.isNotBlank()) {
            appendLine("Mother Phone: ${student.motherPhone}")
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
        Log.e("StudentDetail", "Failed to share student details", e)
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

private fun openDialer(context: Context, phoneNumber: String) {
    val normalized = phoneNumber.filter { it.isDigit() || it == '+' }
    if (normalized.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$normalized")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("StudentDetail", "Failed to open dialer", e)
    }
}

private fun openWhatsApp(context: Context, phoneNumber: String) {
    val digits = phoneNumber.filter(Char::isDigit)
    if (digits.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$digits")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("StudentDetail", "Failed to open WhatsApp", e)
    }
}

private fun openEmailApp(context: Context, email: String) {
    if (email.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("StudentDetail", "Failed to open email app", e)
    }
}
