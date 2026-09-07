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

    private val urls = listOf(
        "http://192.168.1.37:9790/api_public.php",
        "http://100.102.13.11:9790/api_public.php"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue
                val json = JSONObject(body)

                val allPresent = json.optBoolean("all_present", true)
                val total = json.optInt("total_not_absen", 0)

                if (!allPresent && total > 0) {
                    NotificationHelper.show(
                        applicationContext,
                        "Belum Absen",
                        "$total karyawan belum absen hari ini"
                    )
                }
                return Result.success()
            } catch (e: Exception) {
                continue
            }
        }
        return Result.retry()
    }
}
