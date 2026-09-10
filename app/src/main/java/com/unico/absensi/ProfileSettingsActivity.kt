package com.unico.absensi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var flAvatar: FrameLayout
    private lateinit var tvName: TextView
    private lateinit var tvEmpInfo: TextView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tvPassMessage: TextView
    private lateinit var btnSavePassword: MaterialButton
    private lateinit var tvAboutName: TextView
    private lateinit var tvAboutCopyright: TextView
    private lateinit var tvLicense: TextView
    private lateinit var etServerUrl: EditText
    private lateinit var btnSaveServer: MaterialButton

    private var username = ""

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                uploadSelectedPhoto(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        flAvatar = findViewById(R.id.flAvatar)
        tvName = findViewById(R.id.tvName)
        tvEmpInfo = findViewById(R.id.tvEmpInfo)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvPassMessage = findViewById(R.id.tvPassMessage)
        btnSavePassword = findViewById(R.id.btnSavePassword)
        tvAboutName = findViewById(R.id.tvAboutName)
        tvAboutCopyright = findViewById(R.id.tvAboutCopyright)
        tvLicense = findViewById(R.id.tvLicense)
        etServerUrl = findViewById(R.id.etServerUrl)
        btnSaveServer = findViewById(R.id.btnSaveServer)

        val customUrl = Prefs.getCustomBaseUrl()
        etServerUrl.setText(customUrl ?: ApiConfig.baseUrls.first())

        btnSaveServer.setOnClickListener {
            Haptics.click(this)
            saveServerUrl()
        }

        applyAvatarClip()

        val session = Prefs.current()
        username = session?.username ?: ""
        tvName.text = session?.name ?: "-"
        tvEmpInfo.text = buildString {
            append("EMP CODE ")
            append(session?.empCode?.takeIf { it.isNotBlank() } ?: "-")
            session?.dept?.takeIf { it.isNotBlank() && it != "-" && it != "null" }?.let {
                append("  ·  ")
                append(it)
            }
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            Haptics.click(this)
            finish()
        }

        flAvatar.setOnClickListener {
            Haptics.click(this)
            pickPhotoLauncher.launch("image/*")
        }

        tvAboutName.text = "${getString(R.string.app_name)}  v${BuildConfig.VERSION_NAME}"
        tvAboutCopyright.text = "© 2026 rnga4"
        tvLicense.setOnClickListener {
            Haptics.click(this)
            showLicenseDialog()
        }

        btnSavePassword.setOnClickListener {
            Haptics.click(this)
            changePassword()
        }

        loadProfile()
    }

    private fun applyAvatarClip() {
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(this@ProfileSettingsActivity, R.color.avatar_1))
            setStroke(
                (1.5f * resources.displayMetrics.density).toInt(),
                ContextCompat.getColor(this@ProfileSettingsActivity, R.color.border_dark)
            )
        }
        ivAvatar.background = circle
        tvAvatarInitial.background = circle
        ivAvatar.clipToOutline = true
        tvAvatarInitial.clipToOutline = true
    }

    private fun loadProfile() {
        Thread {
            val profile = try {
                AbsensiApi.getProfile()
            } catch (e: Exception) {
                null
            }
            runOnUiThreadSafe {
                if (profile != null) {
                    tvName.text = profile.name
                    tvEmpInfo.text = buildString {
                        append("EMP CODE ")
                        append(profile.empCode.ifBlank { "-" })
                        if (profile.dept.isNotBlank() && profile.dept != "-" && profile.dept != "null") {
                            append("  ·  ")
                            append(profile.dept)
                        }
                    }
                }
                loadAvatar(profile?.hasPhoto == true)
            }
        }.start()
    }

    private fun loadAvatar(hasPhoto: Boolean) {
        if (!hasPhoto || username.isEmpty()) {
            showInitial()
            return
        }
        Thread {
            val bmp = AbsensiApi.getPhoto(username)
            runOnUiThreadSafe {
                if (bmp != null) {
                    ivAvatar.setImageDrawable(bmp.toCircularDrawable(ivAvatar.resources))
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                } else {
                    showInitial()
                }
            }
        }.start()
    }

    private fun showInitial() {
        ivAvatar.visibility = View.GONE
        tvAvatarInitial.visibility = View.VISIBLE
        val name = tvName.text.toString()
        tvAvatarInitial.text = AdminAdapter.getInitials(name.ifBlank { "?" })
    }

    private fun uploadSelectedPhoto(uri: Uri) {
        val bitmap = decodeUri(uri) ?: run {
            tvPassMessage.text = "Gagal membaca gambar."
            tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.danger))
            tvPassMessage.visibility = View.VISIBLE
            return
        }
        flAvatar.isEnabled = false
        flAvatar.alpha = 0.6f

        Thread {
            val msg = try {
                AbsensiApi.uploadPhoto(bitmap)
            } catch (e: Exception) {
                null
            }
            runOnUiThreadSafe {
                flAvatar.isEnabled = true
                flAvatar.alpha = 1.0f
                tvPassMessage.visibility = View.VISIBLE
                if (msg != null) {
                    setResult(RESULT_OK)
                    tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.success))
                    tvPassMessage.text = msg
                    ivAvatar.setImageDrawable(bitmap.toCircularDrawable(ivAvatar.resources))
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                } else {
                    tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.danger))
                    tvPassMessage.text = "Gagal mengunggah foto."
                }
            }
        }.start()
    }

    private fun decodeUri(uri: Uri): Bitmap? {
        return try {
            val bmp = with(contentResolver) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                val maxDim = 2400
                while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }
            bmp?.let {
                if (it.width > 2400 || it.height > 2400) {
                    val scale = 2400f / maxOf(it.width, it.height)
                    Bitmap.createScaledBitmap(it,
                        (it.width * scale).toInt(),
                        (it.height * scale).toInt(), true)
                } else {
                    it
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun changePassword() {
        val cur = etCurrentPassword.text.toString()
        val new = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        tvPassMessage.visibility = View.GONE

        if (cur.isEmpty() || new.isEmpty() || confirm.isEmpty()) {
            showPassError("Semua kolom password wajib diisi.")
            return
        }
        if (new.length < 6) {
            showPassError("Password baru minimal 6 karakter.")
            return
        }
        if (new != confirm) {
            showPassError("Password baru dan konfirmasi tidak cocok.")
            return
        }
        if (cur == new) {
            showPassError("Password baru tidak boleh sama dengan password lama.")
            return
        }

        btnSavePassword.isEnabled = false
        btnSavePassword.text = "Menyimpan..."
        Thread {
            val result = try {
                AbsensiApi.changePassword(cur, new)
            } catch (e: Exception) {
                null
            }
            runOnUiThreadSafe {
                btnSavePassword.isEnabled = true
                btnSavePassword.text = "Simpan Password"
                if (result != null) {
                    tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.success))
                    tvPassMessage.text = result
                    etCurrentPassword.text.clear()
                    etNewPassword.text.clear()
                    etConfirmPassword.text.clear()
                    MaterialAlertDialogBuilder(this)
                        .setMessage(result)
                        .setPositiveButton("Oke", null)
                        .show()
                } else {
                    showPassError("Gagal mengubah password. Cek password lama Anda.")
                }
            }
        }.start()
    }

    private fun saveServerUrl() {
        var url = etServerUrl.text.toString().trim()
        tvPassMessage.visibility = View.VISIBLE
        if (url.isEmpty() ||
            (!url.startsWith("http://") && !url.startsWith("https://"))
        ) {
            tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.danger))
            tvPassMessage.text = "Alamat server tidak valid. Contoh: http://192.168.1.37:9790"
            return
        }
        url = url.trimEnd('/')
        while (url.endsWith("/")) url = url.dropLast(1)
        Prefs.setCustomBaseUrl(url)
        tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.success))
        tvPassMessage.text = "Alamat server disimpan. Aplikasi akan memakai server ini lebih dulu."
    }

    private fun showLicenseDialog() {
        val licenseText = try {
            assets.open("LICENSE.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Copyright (c) 2026 rnga4\n\nMIT License. Bebas dipakai, dimodifikasi, dan didistribusikan dengan tetap mencantumkan atribusi."
        }
        val thirdParty = "\n\nLibrary pihak ketiga:\n" +
            "• OkHttp — Apache License 2.0\n" +
            "• Kotlin — Apache License 2.0\n" +
            "• AndroidX — Apache License 2.0\n" +
            "• Material Components — Apache License 2.0"
        MaterialAlertDialogBuilder(this)
            .setTitle("Lisensi MIT")
            .setMessage(licenseText + thirdParty)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showPassError(msg: String) {
        tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.danger))
        tvPassMessage.text = msg
        tvPassMessage.visibility = View.VISIBLE
    }
}