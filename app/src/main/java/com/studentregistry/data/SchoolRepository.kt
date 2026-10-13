package com.studentregistry.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SchoolRepository(
    private val context: Context,
    private val schoolDao: SchoolDao,
    private val studentDao: StudentDao
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun cacheSchool(school: SchoolEntity) {
        schoolDao.upsert(school)
    }

    suspend fun getCachedSchool(id: String): SchoolEntity? = schoolDao.getById(id)

    fun observeSchools(): Flow<List<SchoolEntity>> = schoolDao.observeSchools()

    suspend fun getCachedSchools(): List<SchoolEntity> = schoolDao.getAll()

    suspend fun createSchool(
        name: String,
        address: String,
        contactInformation: String,
        teacherEmails: List<String>
    ): SchoolEntity {
        val school = SchoolEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            address = address.trim(),
            contactInformation = contactInformation.trim(),
            teacherEmailsCsv = teacherEmails.joinToString(","),
            studentIdsCsv = "",
            isActive = true,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        cacheSchool(school)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            writeSchoolDocument(
                school = school,
                teacherEmails = teacherEmails,
                includeStudents = true
            )
        }
        return school
    }

    suspend fun addTeacherToSchool(schoolId: String, teacherEmail: String): SchoolEntity? {
        val normalizedSchoolId = schoolId.trim()
        val normalizedTeacherEmail = teacherEmail.trim().lowercase()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return null
        if (normalizedTeacherEmail.isBlank()) return null

        val current = getCachedSchool(normalizedSchoolId) ?: findSchoolById(normalizedSchoolId) ?: return null
        val teachers = current.teacherEmailsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        teachers.add(normalizedTeacherEmail)
        val updated = current.copy(
            teacherEmailsCsv = teachers.joinToString(","),
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        cacheSchool(updated)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            firestore.collection("schools").document(normalizedSchoolId).set(
                schoolPayload(
                    school = updated,
                    teachersField = FieldValue.arrayUnion(normalizedTeacherEmail),
                    includeStudents = false
                ),
                SetOptions.merge()
            ).await()
        }
        return updated
    }

    suspend fun updateSchool(
        schoolId: String,
        name: String,
        address: String,
        contactInformation: String
    ): SchoolEntity? {
        val normalizedSchoolId = schoolId.trim()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return null

        val current = getCachedSchool(normalizedSchoolId) ?: findSchoolById(normalizedSchoolId) ?: return null
        val updated = current.copy(
            name = name.trim(),
            address = address.trim(),
            contactInformation = contactInformation.trim(),
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        cacheSchool(updated)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            writeSchoolDocument(
                school = updated,
                teacherEmails = parseCsv(updated.teacherEmailsCsv),
                includeStudents = false
            )
        }
        return updated
    }

    suspend fun setSchoolActive(schoolId: String, isActive: Boolean): SchoolEntity? {
        val normalizedSchoolId = schoolId.trim()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return null

        val current = getCachedSchool(normalizedSchoolId) ?: findSchoolById(normalizedSchoolId) ?: return null
        val updated = current.copy(
            isActive = isActive,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        cacheSchool(updated)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            firestore.collection("schools").document(normalizedSchoolId).set(
                schoolPayload(
                    school = updated,
                    teachersField = parseCsv(updated.teacherEmailsCsv),
                    includeStudents = false
                ),
                SetOptions.merge()
            ).await()
        }
        return updated
    }

    suspend fun deleteSchool(schoolId: String): Boolean {
        val normalizedSchoolId = schoolId.trim()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return false

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            deleteSchoolFromFirestore(normalizedSchoolId)
        }
        schoolDao.deleteById(normalizedSchoolId)
        studentDao.deleteForSchool(normalizedSchoolId)
        return true
    }

    suspend fun findSchoolForTeacher(teacherEmail: String): SchoolEntity? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        if (teacherEmail.isBlank()) return null
        val snapshot = firestore.collection("schools")
            .whereArrayContains("teachers", teacherEmail)
            .limit(1)
            .get()
            .await()
        val document = snapshot.documents.firstOrNull { it.getBoolean("isActive") != false } ?: return null
        return document.toSchoolEntity()
    }

    suspend fun findSchoolById(schoolId: String): SchoolEntity? {
        val normalizedSchoolId = schoolId.trim()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return null
        if (FirebaseApp.getApps(context).isEmpty()) return null
        val snapshot = firestore.collection("schools").document(normalizedSchoolId).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toSchoolEntity()
    }

    suspend fun syncSchoolFromFirestore(schoolId: String): SchoolEntity? {
        val normalizedSchoolId = schoolId.trim()
        if (normalizedSchoolId.isBlank() || normalizedSchoolId.contains("/")) return null
        if (FirebaseApp.getApps(context).isEmpty()) return getCachedSchool(normalizedSchoolId)

        val schoolSnapshot = firestore.collection("schools").document(normalizedSchoolId).get().await()
        if (!schoolSnapshot.exists()) return getCachedSchool(normalizedSchoolId)

        val school = schoolSnapshot.toSchoolEntity(isSynced = true)
        cacheSchool(school)

        val studentSnapshots = firestore.collection("students")
            .whereEqualTo("schoolId", normalizedSchoolId)
            .get()
            .await()

        studentSnapshots.documents
            .mapNotNull { it.toStudentEntityOrNull() }
            .forEach { studentDao.upsert(it) }

        return school
    }

    suspend fun syncAllSchoolsFromFirestore(): List<SchoolEntity> {
        if (FirebaseApp.getApps(context).isEmpty()) return getCachedSchools()

        val schoolSnapshots = firestore.collection("schools").get().await().documents
        val students = firestore.collection("students")
            .get()
            .await()
            .documents
            .mapNotNull { it.toStudentEntityOrNull() }

        val studentIdsBySchool = students
            .groupBy { it.schoolId }
            .mapValues { (_, schoolStudents) -> schoolStudents.map { it.id }.sorted() }
        val schools = schoolSnapshots.map { snapshot ->
            snapshot.toSchoolEntity(
                studentIdsOverride = studentIdsBySchool[snapshot.id].orEmpty(),
                isSynced = true
            )
        }

        schoolDao.upsertAll(schools)
        studentDao.upsertAll(students)

        val remoteSchoolIds = schools.map { it.id }.toSet()
        schoolDao.getAll()
            .filterNot { it.id in remoteSchoolIds || !it.isSynced }
            .forEach { schoolDao.deleteById(it.id) }

        val remoteStudentIds = students.map { it.id }.toSet()
        studentDao.getAll()
            .filterNot { it.id in remoteStudentIds || !it.isSynced }
            .forEach { studentDao.delete(it) }

        return schools.sortedBy { it.name.lowercase() }
    }

    suspend fun addStudentToSchool(student: StudentEntity): SchoolEntity {
        val school = getCachedSchool(student.schoolId) ?: findSchoolById(student.schoolId)
            ?: error("School not found.")
        val updatedSchool = school.copy(
            studentIdsCsv = (parseCsv(school.studentIdsCsv) + student.id).distinct().joinToString(","),
            isSynced = false,
            updatedAt = maxOf(school.updatedAt, student.updatedAt)
        )

        studentDao.upsert(student.copy(isSynced = false))
        schoolDao.upsert(updatedSchool)

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            firestore.collection("students").document(student.id)
                .set(studentPayload(student), SetOptions.merge())
                .await()
            writeSchoolDocument(
                school = updatedSchool,
                teacherEmails = parseCsv(updatedSchool.teacherEmailsCsv),
                includeStudents = true
            )
        }

        return updatedSchool
    }

    suspend fun submitAccessRequest(teacherEmail: String, teacherName: String, note: String) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val payload = mapOf(
            "teacherEmail" to teacherEmail,
            "teacherName" to teacherName,
            "note" to note,
            "status" to "pending",
            "requestedAt" to System.currentTimeMillis()
        )
        firestore.collection("school_access_requests").add(payload).await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toSchoolEntity(
        studentIdsOverride: List<String>? = null,
        isSynced: Boolean = true
    ): SchoolEntity {
        val teachers = get("teachers") as? List<*> ?: emptyList<Any>()
        val students = studentIdsOverride ?: (get("students") as? List<*>)?.filterIsInstance<String>().orEmpty()
        return SchoolEntity(
            id = id,
            name = getString("name").orEmpty(),
            address = getString("address").orEmpty(),
            contactInformation = getString("contactInformation").orEmpty(),
            teacherEmailsCsv = teachers.filterIsInstance<String>().joinToString(","),
            studentIdsCsv = students.joinToString(","),
            isActive = getBoolean("isActive") ?: true,
            isSynced = isSynced,
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toStudentEntityOrNull(): StudentEntity? {
        val studentId = id.ifBlank { getString("id").orEmpty() }
        val name = getString("name").orEmpty()
        val schoolId = getString("schoolId").orEmpty()
        if (studentId.isBlank() || schoolId.isBlank() || name.isBlank()) return null
        return StudentEntity(
            id = studentId,
            teacherId = getString("teacherId").orEmpty(),
            schoolId = schoolId,
            name = name,
            age = (getLong("age") ?: 0L).toInt(),
            contact = getString("contact").orEmpty(),
            fatherPhone = getString("fatherPhone")?.takeIf { it.isNotBlank() }
                ?: getString("parentContact").orEmpty(),
            motherPhone = getString("motherPhone").orEmpty(),
            aadhaarNumber = getString("aadhaarNumber").orEmpty(),
            apparId = getString("apparId").orEmpty(),
            penId = getString("penId").orEmpty(),
            fatherName = getString("fatherName").orEmpty(),
            motherName = getString("motherName").orEmpty(),
            religion = getString("religion").orEmpty(),
            caste = getString("caste").orEmpty(),
            bloodGroup = getString("bloodGroup").orEmpty(),
            admissionNumber = getString("admissionNumber").orEmpty(),
            admissionDate = getString("admissionDate").orEmpty(),
            bankNameAddress = getString("bankNameAddress").orEmpty(),
            email = getString("email").orEmpty(),
            birthday = getString("birthday").orEmpty(),
            notes = getString("notes").orEmpty(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            isSynced = true
        )
    }

    private suspend fun writeSchoolDocument(
        school: SchoolEntity,
        teacherEmails: List<String>,
        includeStudents: Boolean
    ) {
        firestore.collection("schools").document(school.id).set(
            schoolPayload(
                school = school,
                teachersField = teacherEmails,
                includeStudents = includeStudents
            ),
            SetOptions.merge()
        ).await()
    }

    private fun studentPayload(student: StudentEntity): Map<String, Any> {
        return mapOf(
            "id" to student.id,
            "teacherId" to student.teacherId,
            "schoolId" to student.schoolId,
            "name" to student.name,
            "age" to student.age,
            "contact" to student.contact,
            "fatherPhone" to student.fatherPhone,
            "motherPhone" to student.motherPhone,
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
    }

    private fun schoolPayload(
        school: SchoolEntity,
        teachersField: Any,
        includeStudents: Boolean
    ): Map<String, Any> {
        return buildMap {
            put("name", school.name)
            put("address", school.address)
            put("contactInformation", school.contactInformation)
            put("teachers", teachersField)
            put("isActive", school.isActive)
            put("updatedAt", school.updatedAt)
            if (includeStudents) {
                put("students", parseCsv(school.studentIdsCsv))
            }
        }
    }

    private suspend fun deleteSchoolFromFirestore(schoolId: String) {
        val studentDocs = firestore.collection("students")
            .whereEqualTo("schoolId", schoolId)
            .get()
            .await()
            .documents

        studentDocs.chunked(400).forEach { chunk ->
            val batch: WriteBatch = firestore.batch()
            chunk.forEach { document -> batch.delete(document.reference) }
            batch.commit().await()
        }

        firestore.collection("schools").document(schoolId).delete().await()
    }

    private fun parseCsv(csv: String): List<String> = csv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
