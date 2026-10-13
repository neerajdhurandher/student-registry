package com.studentregistry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String = "",
    val contactInformation: String = "",
    val teacherEmailsCsv: String = "",
    val studentIdsCsv: String = "",
    val isActive: Boolean = true,
    val isSynced: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
