package com.studentregistry.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.studentregistry.data.StudentEntity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportManager(private val context: Context) {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun exportCsv(students: List<StudentEntity>): File {
        val targetDir = context.getExternalFilesDir("exports") ?: context.filesDir
        val file = File(targetDir, "students-${LocalDateTime.now().format(formatter)}.csv")
        val lines = buildList {
            add("Name,Age,Student Contact,Parent Contact,Aadhaar Number,APPAR ID,PEN ID,Father Name,Mother Name,Religion,Caste,Blood Group,Admission Number,Admission Date,Bank Name & Address,Email,Birthday,Notes")
            students.forEach { student ->
                add(
                    listOf(
                        student.name,
                        student.age.toString(),
                        student.contact,
                        student.parentContact,
                        student.aadhaarNumber,
                        student.apparId,
                        student.penId,
                        student.fatherName,
                        student.motherName,
                        student.religion,
                        student.caste,
                        student.bloodGroup,
                        student.admissionNumber,
                        student.admissionDate,
                        student.bankNameAddress,
                        student.email,
                        student.birthday,
                        student.notes.replace(",", ";")
                    ).joinToString(",")
                )
            }
        }
        file.writeText(lines.joinToString(separator = "\n"))
        return file
    }

    fun exportPdf(students: List<StudentEntity>): File {
        val targetDir = context.getExternalFilesDir("exports") ?: context.filesDir
        val file = File(targetDir, "students-${LocalDateTime.now().format(formatter)}.pdf")
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f }

        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        var y = 40f
        canvas.drawText("Student Registry Export", 40f, y, paint)
        y += 30f
        students.forEach { student ->
            canvas.drawText(
                "${student.name} | Age ${student.age} | ${student.contact} | Parent ${student.parentContact} | ${student.admissionNumber} | ${student.birthday}",
                40f,
                y,
                paint
            )
            y += 20f
        }
        document.finishPage(page)

        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}
