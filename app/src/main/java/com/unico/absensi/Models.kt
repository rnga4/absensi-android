package com.unico.absensi

sealed class ListRow {
    data class DeptHeader(val name: String, val count: Int = 0) : ListRow()
    data class Employee(
        val empCode: String,
        val name: String,
        val dept: String,
        val loveCount: Int = 0,
        val hasLoved: Boolean = false
    ) : ListRow()
}

data class VoteResult(
    val success: Boolean,
    val state: String = "",
    val myVote: Boolean = false,
    val loveCount: Int = 0,
    val message: String? = null
)

sealed class AdminRow {
    data class Dept(val name: String, val count: Int) : AdminRow()
    data class Emp(val employee: AdminEmployeeRow) : AdminRow()
}

data class AdminStat(
    val total: Int = 0,
    val hadir: Int = 0,
    val telat: Int = 0,
    val belum: Int = 0
)

data class AdminEmployeeRow(
    val code: String,
    val name: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val late: Int
)

data class AdminDept(
    val department: String,
    val employees: List<AdminEmployeeRow>
)

data class AdminDashboard(
    val date: String,
    val stat: AdminStat,
    val departments: List<AdminDept>
)

data class EmpToday(
    val date: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val late: Int
)

data class EmployeeSelf(
    val empCode: String,
    val name: String,
    val dept: String,
    val today: EmpToday
)

data class UserProfile(
    val name: String,
    val username: String,
    val empCode: String,
    val dept: String,
    val role: String,
    val hasPhoto: Boolean,
    val photoUrl: String?,
    val loveCount: Int = 0
)

data class HistoryDay(
    val date: String,
    val dateFormatted: String,
    val dayName: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val late: Int
)
