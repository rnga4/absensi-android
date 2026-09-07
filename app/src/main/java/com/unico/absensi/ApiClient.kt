package com.unico.absensi

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(context: Context) {

    private val cookieStorage = PersistentCookieStorage(context)

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieStorage)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
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

    /** GET JSON with login session. Returns body or throws. */
    fun get(path: String, query: Map<String, String> = emptyMap()): String {
        return requestWithFailover(ApiConfig.url(path)) { base ->
            val url = withQuery(base, query)
            client.newCall(Request.Builder().url(url).get().build()).execute()
        }
    }

    /** GET public endpoint (no login required). */
    fun getPublic(): String = get(ApiConfig.PUBLIC)

    private fun requestWithFailover(
        urls: List<String>,
        block: (String) -> okhttp3.Response
    ): String {
        var lastError: Exception? = null
        for (url in urls) {
            try {
                block(url).use { resp ->
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful) return body
                    if (resp.code == 401) {
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

    private fun withQuery(base: String, query: Map<String, String>): String {
        if (query.isEmpty()) return base
        val params = query.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        return "$base?$params"
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    class HttpException(val code: Int, val body: String) :
        Exception("HTTP $code")
}

private class PersistentCookieStorage(private val context: Context) : CookieJar {

    private val prefs =
        context.getSharedPreferences("absensi_cookies", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            if (cookie.name.equals("PHPSESSID", ignoreCase = true)) {
                prefs.edit().putString("PHPSESSID", cookie.value).apply()
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val value = prefs.getString("PHPSESSID", null) ?: return emptyList()
        val sessionCookie = Cookie.Builder()
            .domain(url.host)
            .path("/")
            .name("PHPSESSID")
            .value(value)
            .build()
        return listOf(sessionCookie)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
