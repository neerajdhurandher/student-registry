package com.studentregistry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.random.Random

@Composable
internal fun RandomStudentDebugAction(
    applyRandomStudent: (StudentFormInput) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Testing Tools",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Debug-only helper. This button is compiled from src/debug and will not appear in release builds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { applyRandomStudent(generateRandomStudentInput()) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Random")
            }
        }
    }
}

private fun generateRandomStudentInput(): StudentFormInput {
    val firstNames = listOf("Aarav", "Diya", "Ishaan", "Meera", "Kabir", "Anaya", "Vivaan", "Saanvi")
    val lastNames = listOf("Sharma", "Patel", "Verma", "Singh", "Gupta", "Joshi", "Nair", "Yadav")
    val religions = listOf("Hindu", "Muslim", "Sikh", "Christian", "Jain")
    val castes = listOf("General", "OBC", "SC", "ST")
    val bloodGroups = listOf("A+", "B+", "AB+", "O+", "A-", "B-", "O-")
    val cities = listOf("Jaipur", "Pune", "Indore", "Nagpur", "Lucknow", "Surat")
    val random = Random(System.currentTimeMillis())

    val firstName = firstNames.random(random)
    val lastName = lastNames.random(random)
    val studentName = "$firstName $lastName"
    val fatherName = "${firstNames.random(random)} $lastName"
    val motherName = "${firstNames.random(random)} ${lastNames.random(random)}"
    val birthday = LocalDate.now()
        .minusYears(random.nextInt(6, 17).toLong())
        .minusDays(random.nextInt(0, 365).toLong())
        .toString()
    val admissionDate = LocalDate.now()
        .minusDays(random.nextInt(10, 240).toLong())
        .toString()
    val studentNumber = buildPhoneNumber(random)
    val fatherNumber = buildPhoneNumber(random)
    val motherNumber = buildPhoneNumber(random)
    val emailHandle = "${firstName.lowercase()}.${lastName.lowercase()}${random.nextInt(10, 99)}"

    return StudentFormInput(
        name = studentName,
        age = calculateDebugAgeFromBirthday(birthday),
        contact = studentNumber,
        fatherPhone = fatherNumber,
        motherPhone = motherNumber,
        aadhaarNumber = buildDigits(random, 12),
        apparId = buildDigits(random, 12),
        penId = buildDigits(random, 11),
        fatherName = fatherName,
        motherName = motherName,
        religion = religions.random(random),
        caste = castes.random(random),
        bloodGroup = bloodGroups.random(random),
        admissionNumber = buildDigits(random, 6),
        admissionDate = admissionDate,
        bankNameAddress = "State Bank Branch, ${cities.random(random)}",
        email = "$emailHandle@school.edu",
        birthday = birthday,
        notes = "Generated debug profile for testing sync and form flow."
    )
}

private fun buildDigits(random: Random, length: Int): String =
    (1..length).joinToString(separator = "") { random.nextInt(0, 10).toString() }

private fun buildPhoneNumber(random: Random): String =
    "9" + buildDigits(random, 9)

private fun calculateDebugAgeFromBirthday(birthday: String): String = runCatching {
    val birthDate = LocalDate.parse(birthday)
    val today = LocalDate.now()
    if (birthDate.isAfter(today)) return ""
    java.time.Period.between(birthDate, today).years.toString()
}.getOrDefault("")
