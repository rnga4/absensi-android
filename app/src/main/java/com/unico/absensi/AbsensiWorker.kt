package com.unico.absensi

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AbsensiWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        Prefs.init(applicationContext)
        val urls = ApiConfig.url(ApiConfig.PUBLIC)

        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue
                if (!response.isSuccessful) continue

                for (base in ApiConfig.baseUrls) {
                    if (url.startsWith(base)) {
                        Prefs.saveLastBaseUrl(base)
                        break
                    }
                }

                val json = JSONObject(body)
                val allPresent = json.optBoolean("all_present", true)
                val total = json.optInt("total_not_absen", 0)

                if (!allPresent && total > 0 && shouldNotify()) {
                    NotificationHelper.show(
                        applicationContext,
                        "Belum Absen",
                        "$total karyawan belum absen hari ini"
                    )
                    Prefs.setLastNotifAt(System.currentTimeMillis())
                }
                return Result.success()
            } catch (e: Exception) {
                continue
            }
        }
        return Result.retry()
    }

    private fun shouldNotify(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < 6 || hour > 22) return false

        val last = Prefs.getLastNotifAt() ?: return true
        return System.currentTimeMillis() - last >= 3 * 60 * 60 * 1000L
    }
}
