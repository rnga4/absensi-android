package com.unico.absensi

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ExitHelper {

    fun confirmAndExit(activity: AppCompatActivity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Keluar Aplikasi?")
            .setMessage("Yakin ingin menutup aplikasi absensi?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                activity.finishAffinity()
            }
            .show()
    }
}