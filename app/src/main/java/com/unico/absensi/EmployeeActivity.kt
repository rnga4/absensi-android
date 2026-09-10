package com.unico.absensi

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class EmployeeActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmpCode: TextView
    private lateinit var tvDept: TextView
    private lateinit var flAvatar: FrameLayout
    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var tvStatus: TextView
    private lateinit var pbAttendance: android.widget.ProgressBar
    private lateinit var tvIn: TextView
    private lateinit var tvOut: TextView
    private lateinit var tvLoveCount: TextView
    private lateinit var btnPublic: MaterialButton
    private lateinit var cardAttendance: View

    private var username = ""
    private var pulseAnim: android.animation.ObjectAnimator? = null
    private var isLoading = false

    private val autoRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            refreshTodayStatus()
            autoRefreshHandler.postDelayed(this, 60000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee)

        tvName = findViewById(R.id.tvName)
        tvEmpCode = findViewById(R.id.tvEmpCode)
        tvDept = findViewById(R.id.tvDept)
        flAvatar = findViewById(R.id.flAvatar)
        ivAvatar = findViewById(R.id.ivAvatar)
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        tvStatus = findViewById(R.id.tvStatus)
        pbAttendance = findViewById(R.id.pbAttendance)
        cardAttendance = findViewById(R.id.cardAttendance)
        tvIn = findViewById(R.id.tvIn)
        tvOut = findViewById(R.id.tvOut)
        tvLoveCount = findViewById(R.id.tvLoveCount)
        btnPublic = findViewById(R.id.btnPublic)

        btnPublic.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            Haptics.click(this)
            showProfileMenu()
        }

        flAvatar.setOnClickListener {
            Haptics.click(this)
            startActivity(Intent(this, ProfileSettingsActivity::class.java))
        }

        val prefsUser = Prefs.current()
        username = prefsUser?.username ?: ""
        tvName.text = prefsUser?.name ?: "-"
        val code = prefsUser?.empCode ?: ""
        tvEmpCode.text = "EMP CODE  $code"
        tvAvatarInitial.text = AdminAdapter.getInitials(prefsUser?.name ?: "?")

        cardAttendance.setOnClickListener {
            Haptics.click(this)
            startActivity(Intent(this, HistoryActivity::class.java)
                .putExtra("emp_code", code)
                .putExtra("emp_name", prefsUser?.name))
        }
        btnPublic.setOnClickListener {
            Haptics.click(this)
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun showProfileMenu() {
        val anchor = findViewById<View>(R.id.btnMenu)
        val view = LayoutInflater.from(this).inflate(R.layout.popup_profile_menu, null)
        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popup.isFocusable = true
        popup.elevation = 12f
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<View>(R.id.menuProfile).setOnClickListener {
            popup.dismiss()
            startActivity(Intent(this, ProfileSettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.menuLogout).setOnClickListener {
            popup.dismiss()
            LogoutHelper.confirmAndLogout(this)
        }

        val density = view.resources.displayMetrics.density
        val widthPx = view.resources.displayMetrics.widthPixels
        val menuWidth = (248f * density).toInt()
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val anchorRight = loc[0] + anchor.width
        val overflow = anchorRight + menuWidth - widthPx
        val offsetX = if (overflow > 0) -overflow else 0
        popup.showAsDropDown(anchor, offsetX, 8)
    }

    override fun onResume() {
        super.onResume()
        loadData()
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 60000)
    }

    override fun onPause() {
        super.onPause()
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
    }

    private fun loadData() {
        if (isLoading) return
        isLoading = true

        pulseAnim?.cancel()
        pbAttendance.visibility = View.VISIBLE
        pulseAnim = android.animation.ObjectAnimator.ofFloat(tvStatus, "alpha", 0.35f, 1.0f).apply {
            duration = 750
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE
            start()
        }

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
                isLoading = false
                pulseAnim?.cancel()
                tvStatus.alpha = 1.0f
                pbAttendance.visibility = View.GONE

                if (profile != null) {
                    tvName.text = profile.name
                    tvLoveCount.text = "❤️ ${profile.loveCount}"
                }

                if (bmp != null) {
                    ivAvatar.setImageDrawable(bmp.toCircularDrawable(ivAvatar.resources))
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                } else {
                    ivAvatar.visibility = View.GONE
                    tvAvatarInitial.visibility = View.VISIBLE
                }

                renderSelf(self, showError = true)

                cardAttendance.alpha = 0.5f
                cardAttendance.animate().alpha(1.0f).setDuration(250).start()
            }
        }.start()
    }

    private fun refreshTodayStatus() {
        if (isLoading) return
        isLoading = true
        Thread {
            val self = try {
                AbsensiApi.employeeSelf()
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                isLoading = false
                renderSelf(self, showError = false)
            }
        }.start()
    }

    private fun renderSelf(self: EmployeeSelf?, showError: Boolean) {
        if (self == null) {
            if (showError) {
                tvStatus.text = "[ GAGAL MEMUAT STATUS ]"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.danger))
            }
            return
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
    }

    override fun onBackPressed() {
        ExitHelper.confirmAndExit(this)
    }
}
