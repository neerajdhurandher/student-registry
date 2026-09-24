package com.studentregistry.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studentregistry.data.SchoolEntity
import com.studentregistry.data.StudentEntity
import kotlinx.coroutines.tasks.await

class CloudSyncService(private val context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun hasFirebaseConfig(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun syncSchool(teacherEmail: String, school: SchoolEntity, students: List<StudentEntity>): Boolean {
        if (!hasFirebaseConfig()) return false

        val schoolRef = firestore.collection("schools").document(school.id)
        val remoteSnapshot = schoolRef.get().await()
        if (!remoteSnapshot.exists()) {
            schoolRef.set(
                mapOf(
                    "name" to school.name,
                    "address" to school.address,
                    "contactInformation" to school.contactInformation
                ),
                SetOptions.merge()
            ).await()
        }

        val remoteStudents = firestore.collection("students")
            .whereEqualTo("schoolId", school.id)
            .get()
            .await()

        val localIds = students.map { it.id }.toSet()
        for (document in remoteStudents.documents) {
            if (document.id !in localIds) {
                firestore.collection("students").document(document.id).delete().await()
            }
        }

        for (student in students) {
            val payload: Map<String, Any> = mapOf(
                "id" to student.id,
                "teacherId" to teacherEmail,
                "schoolId" to school.id,
                "name" to student.name,
                "age" to student.age,
                "contact" to student.contact,
                "parentContact" to student.parentContact,
                "aadhaarNumber" to student.aadhaarNumber,
                "apparId" to student.apparId,
                "penId" to student.penId,
                "fatherName" to student.fatherName,
                "motherName" to student.motherName,
                "religion" to student.religion,
                "caste" to student.caste,
                "bloodGroup" to student.bloodGroup,
                "admissionNumber" to student.admissionNumber,
                "admissionDate" to student.admissionDate,
                "bankNameAddress" to student.bankNameAddress,
                "email" to student.email,
                "birthday" to student.birthday,
                "notes" to student.notes,
                "updatedAt" to student.updatedAt
            )
            firestore.collection("students").document(student.id).set(payload, SetOptions.merge()).await()
        }

        schoolRef.set(
            mapOf(
                "teachers" to FieldValue.arrayUnion(teacherEmail),
                "students" to students.map { it.id }
            ),
            SetOptions.merge()
        ).await()

        return true
    }
}
