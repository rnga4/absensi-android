package com.unico.absensi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(context: Context) {

    private val cookieStorage = PersistentCookieStorage(context)

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieStorage)
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val uploadClient = OkHttpClient.Builder()
        .cookieJar(cookieStorage)
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val formType = "application/x-www-form-urlencoded".toMediaType()

    /** POST form login, tries each base URL until one succeeds. Returns JSON body. */
    fun login(username: String, password: String): String {
        val body = "username=${encode(username)}&password=${encode(password)}"
            .toRequestBody(formType)
        return requestWithFailover(ApiConfig.url(ApiConfig.LOGIN)) { url ->
            client.newCall(Request.Builder().url(url).post(body).build()).execute()
        }
    }

    /** POST vote (love toggle) for employee. Returns JSON body. */
    fun vote(empCode: String): String {
        val body = "action=vote&emp_code=${encode(empCode)}"
            .toRequestBody(formType)
        return requestWithFailover(ApiConfig.url(ApiConfig.VOTE)) { url ->
            client.newCall(Request.Builder().url(url).post(body).build()).execute()
        }
    }

    /** GET JSON with login session. Returns body or throws. */
    fun get(path: String, query: Map<String, String> = emptyMap()): String {
        return requestWithFailover(ApiConfig.url(path)) { base ->
            val url = withQuery(base, query)
            client.newCall(Request.Builder().url(url).get().build()).execute()
        }
    }

    /** GET public endpoint (no login required). */
    fun getPublic(): String = get(ApiConfig.PUBLIC)

    /** GET profile (JSON via profile.php?format=json). */
    fun getProfile(): String {
        return requestWithFailover(ApiConfig.url(ApiConfig.PROFILE)) { url ->
            client.newCall(Request.Builder().url(url).get().build()).execute()
        }
    }

    /** GET profile photo bytes via photo.php?u=<username>. Returns ByteArray or throws. */
    fun getPhotoBytes(username: String): ByteArray {
        val path = "/photo.php?u=" + encode(username)
        var lastError: Exception? = null
        for (url in ApiConfig.url(path)) {
            try {
                val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                resp.use {
                    val bytes = it.body?.bytes()
                    if (bytes != null && it.isSuccessful) {
                        saveWorkingBaseUrlFrom(url)
                        return bytes
                    }
                    if (!it.isSuccessful) {
                        lastError = HttpException(it.code, "<photo>")
                        if (it.code == 401) handleSessionExpired()
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Semua server tidak dapat dijangkau")
    }

    /** GET public profile photo by emp_code (photo.php?emp=<code>&pub=1). Returns ByteArray or throws. */
    fun getPublicPhotoByEmp(empCode: String): ByteArray {
        val path = "/photo.php?emp=" + encode(empCode) + "&pub=1"
        var lastError: Exception? = null
        for (url in ApiConfig.url(path)) {
            try {
                val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
                resp.use {
                    val bytes = it.body?.bytes()
                    if (bytes != null && it.isSuccessful) {
                        saveWorkingBaseUrlFrom(url)
                        return bytes
                    }
                    if (!it.isSuccessful) {
                        lastError = HttpException(it.code, "<photo>")
                        if (it.code == 401) handleSessionExpired()
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Semua server tidak dapat dijangkau")
    }

    /** POST change password. Returns JSON body. */
    fun changePassword(currentPassword: String, newPassword: String): String {
        val body = ("action=password" +
            "&current_password=${encode(currentPassword)}" +
            "&new_password=${encode(newPassword)}" +
            "&confirm_password=${encode(newPassword)}")
            .toRequestBody(formType)
        return requestWithFailover(ApiConfig.url(ApiConfig.PROFILE)) { url ->
            client.newCall(Request.Builder().url(url).post(body).build()).execute()
        }
    }

    /** POST upload profile photo (multipart). Returns JSON body. */
    fun uploadPhoto(bitmap: Bitmap): String {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, bos)
        val bytes = bos.toByteArray()
        val imgType = "image/jpeg".toMediaType()
        val filePart = MultipartBody.Part.createFormData(
            "photo", "photo.jpg", bytes.toRequestBody(imgType)
        )
        val actionPart = MultipartBody.Part.createFormData(
            "action", "photo"
        )
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(actionPart)
            .addPart(filePart)
            .build()
        return requestWithFailover(ApiConfig.url(ApiConfig.PROFILE)) { url ->
            uploadClient.newCall(Request.Builder().url(url).post(body).build()).execute()
        }
    }

    private fun requestWithFailover(
        urls: List<String>,
        block: (String) -> okhttp3.Response
    ): String {
        var lastError: Exception? = null
        for (url in urls) {
            try {
                block(url).use { resp ->
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        saveWorkingBaseUrlFrom(url)
                        return body
                    }
                    if (resp.code == 401) {
                        handleSessionExpired()
                        throw HttpException(resp.code, body)
                    }
                    lastError = HttpException(resp.code, body)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Semua server tidak dapat dijangkau")
    }

    private fun handleSessionExpired() {
        Prefs.init(appContext)
        if (!Prefs.isLoggedIn()) return
        Prefs.clear()
        cookieStorage.clear()
        runCatching {
            val intent = Intent(appContext, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            appContext.startActivity(intent)
        }
    }

    private fun saveWorkingBaseUrlFrom(fullUrl: String) {
        for (base in ApiConfig.baseUrls) {
            if (fullUrl.startsWith(base)) {
                Prefs.saveLastBaseUrl(base)
                break
            }
        }
    }

    private fun withQuery(base: String, query: Map<String, String>): String {
        if (query.isEmpty()) return base
        val params = query.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        return "$base?$params"
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    class HttpException(val code: Int, val body: String) :
        Exception("HTTP $code")
}

class PersistentCookieStorage(private val context: Context) : CookieJar {

    private val prefs: android.content.SharedPreferences by lazy {
        try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                "absensi_cookies_secure",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("absensi_cookies", Context.MODE_PRIVATE)
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            if (cookie.name.equals("PHPSESSID", ignoreCase = true)) {
                prefs.edit().putString("PHPSESSID", cookie.value).apply()
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val value = prefs.getString("PHPSESSID", null) ?: return emptyList()
        val builder = Cookie.Builder()
            .name("PHPSESSID")
            .value(value)
            .path("/")

        val host = url.host
        if (host.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$"))) {
            builder.hostOnlyDomain(host)
        } else {
            builder.domain(host)
        }
        return listOf(builder.build())
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
