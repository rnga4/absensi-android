package com.unico.absensi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class EmployeeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee)

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmpCode = findViewById<TextView>(R.id.tvEmpCode)
        val tvDept = findViewById<TextView>(R.id.tvDept)
        val flAvatar = findViewById<FrameLayout>(R.id.flAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        val tvAvatarInitial = findViewById<TextView>(R.id.tvAvatarInitial)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvIn = findViewById<TextView>(R.id.tvIn)
        val tvOut = findViewById<TextView>(R.id.tvOut)
        val btnHistory = findViewById<MaterialButton>(R.id.btnHistory)
        val btnPublic = findViewById<MaterialButton>(R.id.btnPublic)

        btnPublic.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            LogoutHelper.confirmAndLogout(this)
        }

        flAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileSettingsActivity::class.java))
        }

        val prefsUser = Prefs.current()
        val username = prefsUser?.username ?: ""
        tvName.text = prefsUser?.name ?: "-"
        val code = prefsUser?.empCode ?: ""
        tvEmpCode.text = "EMP CODE  $code"
        tvAvatarInitial.text = AdminAdapter.getInitials(prefsUser?.name ?: "?")

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java)
                .putExtra("emp_code", code)
                .putExtra("emp_name", prefsUser?.name))
        }
        btnPublic.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        Thread {
            val self = try {
                AbsensiApi.employeeSelf()
            } catch (e: Exception) {
                null
            }
            val bmp = if (username.isNotEmpty()) {
                try {
                    AbsensiApi.getPhoto(username)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            runOnUiThread {
                if (bmp != null) {
                    ivAvatar.setImageBitmap(bmp)
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                }
                if (self == null) {
                    tvStatus.text = "Gagal memuat status"
                    return@runOnUiThread
                }
                tvDept.text = self.dept
                val t = self.today
                tvStatus.text = when (t.status) {
                    "hadir" -> "[ HADIR ]  ·  ${t.date}"
                    "telat" -> "[ TELAT ${t.late} MENIT ]  ·  ${t.date}"
                    "belum" -> "[ BELUM ABSEN ]  ·  ${t.date}"
                    else -> "[ ${t.status.uppercase()} ]  ·  ${t.date}"
                }
                val statusColor = when (t.status) {
                    "hadir" -> R.color.success
                    "telat" -> R.color.warning
                    "belum" -> R.color.danger
                    else -> R.color.text_primary
                }
                tvStatus.setTextColor(ContextCompat.getColor(this, statusColor))
                tvIn.text = t.inTime
                tvOut.text = t.outTime
            }
        }.start()
    }
}
