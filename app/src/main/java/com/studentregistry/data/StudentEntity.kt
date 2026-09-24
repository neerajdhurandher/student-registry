package com.studentregistry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val schoolId: String,
    val name: String,
    val age: Int,
    val contact: String,
    val parentContact: String = "",
    val aadhaarNumber: String = "",
    val apparId: String = "",
    val penId: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val religion: String = "",
    val caste: String = "",
    val bloodGroup: String = "",
    val admissionNumber: String = "",
    val admissionDate: String = "",
    val bankNameAddress: String = "",
    val email: String,
    val birthday: String,
    val notes: String,
    val updatedAt: Long,
    val isSynced: Boolean
)
