package com.unico.absensi

object ApiConfig {

    private val defaultBaseUrls = listOf(
        "http://192.168.1.37:9790",
        "http://100.102.13.11:9790"
    )

    val baseUrls: List<String>
        get() {
            val custom = Prefs.getCustomBaseUrl()
            if (custom.isNullOrBlank()) return defaultBaseUrls
            return listOf(custom) + defaultBaseUrls.filter { it != custom }
        }

    const val LOGIN = "/api_login.php"
    const val PROFILE = "/profile.php?format=json"
    const val DASHBOARD = "/api_dashboard.php"
    const val HISTORY = "/api_history.php"
    const val PUBLIC = "/api_public.php"
    const val VOTE = "/api_vote.php"
    const val LOGOUT = "/logout.php"

    fun getOrderedBaseUrls(): List<String> {
        val lastUrl = Prefs.getLastBaseUrl()
        if (lastUrl != null && baseUrls.contains(lastUrl)) {
            return listOf(lastUrl) + baseUrls.filter { it != lastUrl }
        }
        return baseUrls
    }

    fun url(path: String): List<String> = getOrderedBaseUrls().map { it + path }
}
