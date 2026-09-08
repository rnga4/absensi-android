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
        val pbAttendance = findViewById<android.widget.ProgressBar>(R.id.pbAttendance)
        val cardAttendance = findViewById<View>(R.id.cardAttendance)
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

        // Loading animation on attendance status
        val pulseAnim = android.animation.ObjectAnimator.ofFloat(tvStatus, "alpha", 0.35f, 1.0f).apply {
            duration = 750
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE
            start()
        }

        val tvLoveCount = findViewById<TextView>(R.id.tvLoveCount)

        Thread {
            val self = try {
                AbsensiApi.employeeSelf()
            } catch (e: Exception) {
                null
            }
            val profile = try {
                AbsensiApi.getProfile()
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
                pulseAnim.cancel()
                tvStatus.alpha = 1.0f
                pbAttendance.visibility = View.GONE

                if (profile != null) {
                    tvLoveCount.text = "❤️ ${profile.loveCount}"
                }

                if (bmp != null) {
                    ivAvatar.alpha = 0f
                    ivAvatar.setImageDrawable(bmp.toCircularDrawable(ivAvatar.resources))
                    ivAvatar.visibility = View.VISIBLE
                    ivAvatar.animate().alpha(1f).setDuration(250).start()
                    tvAvatarInitial.visibility = View.GONE
                }
                if (self == null) {
                    tvStatus.text = "[ GAGAL MEMUAT STATUS ]"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.danger))
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
                tvIn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvOut.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

                cardAttendance.alpha = 0.5f
                cardAttendance.animate().alpha(1.0f).setDuration(250).start()
            }
        }.start()
    }

    override fun onBackPressed() {
        ExitHelper.confirmAndExit(this)
    }
}
