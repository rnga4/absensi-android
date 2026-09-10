package com.unico.absensi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object LogoutHelper {

    fun confirmAndLogout(activity: AppCompatActivity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Logout")
            .setMessage("Yakin ingin keluar?")
            .setPositiveButton("Keluar") { _, _ ->
                Thread { AbsensiApi.logout() }.start()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("Batal", null)
            .setBackground(
                android.graphics.drawable.ColorDrawable(
                    androidx.core.content.ContextCompat.getColor(activity, R.color.bg_primary)
                )
            )
            .show()
    }
}