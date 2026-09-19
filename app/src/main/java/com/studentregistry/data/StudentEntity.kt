package com.studentregistry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val name: String,
    val age: Int,
    val contact: String,
    val parentContact: String = "",
    val email: String,
    val birthday: String,
    val notes: String,
    val updatedAt: Long,
    val isSynced: Boolean
)
