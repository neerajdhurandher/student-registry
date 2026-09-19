package com.studentregistry.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studentregistry.data.StudentEntity
import kotlinx.coroutines.tasks.await

class CloudSyncService(private val context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun hasFirebaseConfig(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun syncStudents(teacherId: String, students: List<StudentEntity>): Boolean {
        if (!hasFirebaseConfig()) {
            return false
        }

        val collection = firestore
            .collection("teachers")
            .document(teacherId)
            .collection("students")

        val remoteStudents = collection.get().await()
        val localIds = students.map { it.id }.toSet()
        for (document in remoteStudents.documents) {
            if (document.id !in localIds) {
                collection.document(document.id).delete().await()
            }
        }

        for (student in students) {
            val payload: Map<String, Any> = mapOf(
                "id" to student.id,
                "teacherId" to student.teacherId,
                "name" to student.name,
                "age" to student.age,
                "contact" to student.contact,
                "email" to student.email,
                "birthday" to student.birthday,
                "notes" to student.notes,
                "updatedAt" to student.updatedAt
            )
            collection.document(student.id).set(payload, SetOptions.merge()).await()
        }
        return true
    }
}
