package com.unico.absensi

import android.app.Activity

fun Activity.runOnUiThreadSafe(action: () -> Unit) {
    runOnUiThread {
        if (!isFinishing && !isDestroyed) {
            action()
        }
    }
}