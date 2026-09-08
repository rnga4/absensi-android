package com.unico.absensi

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

fun Bitmap.toCircularDrawable(resources: Resources): RoundedBitmapDrawable {
    return RoundedBitmapDrawableFactory.create(resources, this).apply {
        isCircular = true
        setAntiAlias(true)
    }
}