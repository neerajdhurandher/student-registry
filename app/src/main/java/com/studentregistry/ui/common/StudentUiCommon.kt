package com.studentregistry

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Academic Clarity Material 3 palette (from Stitch design brief)
val LoginPrimary = Color(0xFF1A56DB)
val LoginPrimaryContainer = Color(0xFFDBEAFE)
val LoginOnPrimaryContainer = Color(0xFF1E40AF)
val LoginTertiary = Color(0xFF0D9488)
val LoginTertiaryContainer = Color(0x1A0D9488)
val LoginSurfaceCanvas = Color(0xFFF8FAFC)
val LoginSurfaceCard = Color(0xFFFFFFFF)
val LoginOutlineVariant = Color(0xFFE2E8F0)
val LoginOnSurface = Color(0xFF0F172A)
val LoginOnSurfaceVariant = Color(0xFF434654)

// Additional Academic Clarity Material 3 tokens (shared with LoginScreen palette above)
val ListSecondary = Color(0xFF516070)
val ListOutline = Color(0xFF737686)
val ListSurfaceContainer = Color(0xFFEAEDFF)
val ListSurfaceContainerLow = Color(0xFFF2F3FF)
val ListSecondaryContainer = Color(0xFFD5E4F8)
val ListPrimaryFixed = Color(0xFFDBE1FF)
val ListPrimaryFixedDim = Color(0xFFB5C4FF)
val ListSurfaceContainerHigh = Color(0xFFE2E7FF)
val ListTertiaryFixed = Color(0xFF89F5E7)
val ListOnTertiaryFixedVariant = Color(0xFF005049)

val avatarPalette = listOf(
    ListPrimaryFixed to LoginPrimary,
    ListSecondaryContainer to Color(0xFF0E1D2B),
    ListSurfaceContainerHigh to LoginPrimary,
    ListPrimaryFixedDim to Color(0xFF00174D)
)

fun studentInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

fun formatBirthdayBadge(birthday: String): String? {
    if (birthday.isBlank()) return null
    return try {
        val parsed = java.time.LocalDate.parse(birthday)
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
    } catch (_: Exception) {
        null
    }
}

fun calculateAgeFromBirthday(birthday: String): String {
    return runCatching {
        val birthDate = java.time.LocalDate.parse(birthday)
        val today = java.time.LocalDate.now()
        if (birthDate.isAfter(today)) return ""
        java.time.Period.between(birthDate, today).years
    }.getOrNull()?.toString().orEmpty()
}

fun isoDateToUtcMillis(isoDate: String): Long? = runCatching {
    java.time.LocalDate.parse(isoDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

fun utcMillisToIsoDate(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()

@Composable
fun formFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = LoginPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedLeadingIconColor = LoginPrimary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    focusedTrailingIconColor = LoginPrimary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = LoginPrimary,
    errorCursorColor = MaterialTheme.colorScheme.error
)
