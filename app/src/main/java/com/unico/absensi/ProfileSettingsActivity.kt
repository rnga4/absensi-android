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
    private lateinit var btnChangePhoto: MaterialButton

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
        btnChangePhoto = findViewById(R.id.btnChangePhoto)

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

        findViewById<TextView>(R.id.tvBack).setOnClickListener {
            finish()
        }

        btnChangePhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        btnSavePassword.setOnClickListener {
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
            runOnUiThread {
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
            runOnUiThread {
                if (bmp != null) {
                    ivAvatar.setImageBitmap(bmp)
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
        btnChangePhoto.isEnabled = false
        btnChangePhoto.text = "Mengunggah..."

        Thread {
            val msg = try {
                AbsensiApi.uploadPhoto(bitmap)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                btnChangePhoto.isEnabled = true
                btnChangePhoto.text = "Ganti Foto"
                tvPassMessage.visibility = View.VISIBLE
                if (msg != null) {
                    tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.success))
                    tvPassMessage.text = msg
                    ivAvatar.setImageBitmap(bitmap)
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
            runOnUiThread {
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

    private fun showPassError(msg: String) {
        tvPassMessage.setTextColor(ContextCompat.getColor(this, R.color.danger))
        tvPassMessage.text = msg
        tvPassMessage.visibility = View.VISIBLE
    }
}