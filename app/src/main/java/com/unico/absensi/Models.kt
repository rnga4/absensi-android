package com.unico.absensi

sealed class ListRow {
    data class DeptHeader(val name: String, val count: Int = 0) : ListRow()
    data class Employee(val empCode: String, val name: String, val dept: String) : ListRow()
}
