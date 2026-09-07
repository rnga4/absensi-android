package com.unico.absensi

object ApiConfig {

    val baseUrls = listOf(
        "http://192.168.1.37:9790",
        "http://100.102.13.11:9790"
    )

    const val LOGIN = "/api_login.php"
    const val DASHBOARD = "/api_dashboard.php"
    const val HISTORY = "/api_history.php"
    const val PUBLIC = "/api_public.php"
    const val LOGOUT = "/logout.php"

    fun url(path: String): List<String> = baseUrls.map { it + path }
}
