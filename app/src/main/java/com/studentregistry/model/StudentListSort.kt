package com.studentregistry.model

enum class StudentListSort(val label: String) {
    ALL("All"),
    NAME_ASC("A-Z"),
    NAME_DESC("Z-A"),
    ADDED_DATE("By Added Date"),
    ADMISSION_NUMBER("By Admission Number")
}
