package com.unico.absensi

import android.content.Context
import android.content.SharedPreferences

data class SessionUser(
    val role: String,
    val username: String,
    val name: String,
    val empCode: String? = null,
    val dept: String? = null
)

object Prefs {

    private const val FILE = "absensi_session"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_ROLE = "role"
    private const val KEY_USERNAME = "username"
    private const val KEY_NAME = "name"
    private const val KEY_EMP_CODE = "emp_code"
    private const val KEY_DEPT = "dept"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(user: SessionUser) {
        sp.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMP_CODE, user.empCode)
            .putString(KEY_DEPT, user.dept)
            .apply()
    }

    fun current(): SessionUser? {
        if (!isLoggedIn()) return null
        return SessionUser(
            role = sp.getString(KEY_ROLE, "admin") ?: "admin",
            username = sp.getString(KEY_USERNAME, "") ?: "",
            name = sp.getString(KEY_NAME, "") ?: "",
            empCode = sp.getString(KEY_EMP_CODE, null),
            dept = sp.getString(KEY_DEPT, null)
        )
    }

    fun isLoggedIn(): Boolean = sp.getBoolean(KEY_LOGGED_IN, false)

    fun role(): String = sp.getString(KEY_ROLE, "") ?: ""

    fun clear() {
        sp.edit().clear().apply()
    }
}
