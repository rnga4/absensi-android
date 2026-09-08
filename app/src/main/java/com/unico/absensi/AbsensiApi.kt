package com.unico.absensi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

object AbsensiApi {

    private var client: ApiClient? = null

    fun init(api: ApiClient) {
        client = api
    }

    private fun api(): ApiClient =
        client ?: throw IllegalStateException("AbsensiApi not initialized")

    fun login(username: String, password: String): SessionUser {
        val json = JSONObject(api().login(username, password))
        if (!json.optBoolean("success", false)) {
            throw ApiClient.HttpException(401, "Login gagal")
        }
        val user = SessionUser(
            role = json.optString("role", "admin"),
            username = json.optString("username", username),
            name = json.optString("name", username),
            empCode = if (json.has("emp_code")) json.optString("emp_code") else null,
            dept = if (json.has("dept") && !json.isNull("dept")) json.optString("dept") else null
        )
        Prefs.saveSession(user)
        return user
    }

    fun logout() {
        try {
            api().get(ApiConfig.LOGOUT)
        } catch (_: Exception) {
        }
        Prefs.clear()
    }

    fun vote(empCode: String): VoteResult {
        val raw = api().vote(empCode)
        val json = JSONObject(raw)
        return VoteResult(
            success = json.optBoolean("success", false),
            state = json.optString("state", ""),
            myVote = json.optBoolean("my_vote", false),
            loveCount = json.optInt("love_count", 0),
            message = if (json.has("message")) json.optString("message") else null
        )
    }

    fun adminDashboard(filter: String = ""): AdminDashboard {
        val query = mutableMapOf<String, String>()
        if (filter.isNotEmpty()) query["f"] = filter
        val json = JSONObject(api().get(ApiConfig.DASHBOARD, query))

        val statObj = json.optJSONObject("stat")
        val stat = AdminStat(
            total = statObj?.optInt("total", 0) ?: 0,
            hadir = statObj?.optInt("hadir", 0) ?: 0,
            telat = statObj?.optInt("telat", 0) ?: 0,
            belum = statObj?.optInt("belum", 0) ?: 0
        )

        val departments = mutableListOf<AdminDept>()
        val depts = json.optJSONArray("departments")
        if (depts != null) {
            for (i in 0 until depts.length()) {
                val d = depts.getJSONObject(i)
                val emps = mutableListOf<AdminEmployeeRow>()
                val arr = d.optJSONArray("employees")
                if (arr != null) {
                    for (j in 0 until arr.length()) {
                        val e = arr.getJSONObject(j)
                        emps.add(
                            AdminEmployeeRow(
                                code = e.optString("code", "-"),
                                name = e.optString("name", "-"),
                                inTime = e.optString("in", "-"),
                                outTime = e.optString("out", "-"),
                                status = e.optString("status", "-"),
                                late = e.optInt("late", 0)
                            )
                        )
                    }
                }
                departments.add(AdminDept(d.optString("department", "-"), emps))
            }
        }

        return AdminDashboard(
            date = json.optString("date", "-"),
            stat = stat,
            departments = departments
        )
    }

    fun employeeSelf(): EmployeeSelf {
        val json = JSONObject(api().get(ApiConfig.DASHBOARD))
        val empObj = json.optJSONObject("employee")
        val todayObj = json.optJSONObject("today")
        val today = EmpToday(
            date = todayObj?.optString("date", "-") ?: "-",
            inTime = todayObj?.optString("in", "-") ?: "-",
            outTime = todayObj?.optString("out", "-") ?: "-",
            status = todayObj?.optString("status", "-") ?: "-",
            late = todayObj?.optInt("late", 0) ?: 0
        )
        return EmployeeSelf(
            empCode = empObj?.optString("emp_code", "-") ?: "-",
            name = empObj?.optString("name", "-") ?: "-",
            dept = empObj?.optString("dept", "-") ?: "-",
            today = today
        )
    }

    fun getProfile(): UserProfile {
        val json = JSONObject(api().getProfile())
        return UserProfile(
            name = json.optString("name", "-"),
            username = json.optString("username", "-"),
            empCode = json.optString("emp_code", "-"),
            dept = json.optString("dept", "-"),
            role = json.optString("role", "employee"),
            hasPhoto = json.optBoolean("has_photo", false),
            photoUrl = if (json.has("photo_url") && !json.isNull("photo_url")) json.optString("photo_url") else null,
            loveCount = json.optInt("love_count", 0)
        )
    }

    fun changePassword(currentPassword: String, newPassword: String): String {
        val json = JSONObject(api().changePassword(currentPassword, newPassword))
        return json.optString("message", "Password berhasil diganti.")
    }

    fun uploadPhoto(bitmap: android.graphics.Bitmap): String {
        val json = JSONObject(api().uploadPhoto(bitmap))
        return json.optString("message", "Foto profil berhasil diperbarui.")
    }

    fun getPhoto(username: String): Bitmap? {
        return try {
            val bytes = api().getPhotoBytes(username)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun getPublicPhotoByEmp(empCode: String): Bitmap? {
        return try {
            val bytes = api().getPublicPhotoByEmp(empCode)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun history(empCode: String, offset: Int = 0): List<HistoryDay> {
        val json = JSONObject(
            api().get(ApiConfig.HISTORY, mapOf("code" to empCode, "offset" to offset.toString()))
        )
        val list = mutableListOf<HistoryDay>()
        val arr = json.optJSONArray("history")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val h = arr.getJSONObject(i)
                list.add(
                    HistoryDay(
                        date = h.optString("date", "-"),
                        dateFormatted = h.optString("date_formatted", "-"),
                        dayName = h.optString("day_name", "-"),
                        inTime = h.optString("in", "-"),
                        outTime = h.optString("out", "-"),
                        status = h.optString("status", "-"),
                        late = h.optInt("late", 0)
                    )
                )
            }
        }
        return list
    }
}
