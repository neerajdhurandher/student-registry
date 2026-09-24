package com.studentregistry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentregistry.data.StudentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
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

@Composable
private fun FormSectionCard(content: @Composable ColumnScope.() -> Unit) {
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
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
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
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FormFieldLabel(text: String, required: Boolean) {
    Row {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
