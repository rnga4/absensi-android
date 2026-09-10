package com.unico.absensi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Prefs.init(this)
        AbsensiApi.init(ApiClient(this))

        if (Prefs.isLoggedIn()) {
            redirectToRole()
            return
        }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnPublic = findViewById<MaterialButton>(R.id.btnPublic)
        val tvError = findViewById<TextView>(R.id.tvLoginError)

        btnLogin.setOnClickListener {
            val u = etUsername.text.toString().trim()
            val p = etPassword.text.toString()
            if (u.isEmpty() || p.isEmpty()) return@setOnClickListener
            Haptics.click(this)

            tvError.visibility = View.GONE
            btnLogin.isEnabled = false
            btnLogin.text = "Memuat..."

            Thread {
                val user = try {
                    AbsensiApi.login(u, p)
                } catch (e: Exception) {
                    null
                }
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Masuk"
                    if (user != null) {
                        redirectToRole()
                    } else {
                        tvError.visibility = View.VISIBLE
                    }
                }
            }.start()
        }

        btnPublic.setOnClickListener {
            Haptics.click(this)
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun redirectToRole() {
        val role = Prefs.role()
        val intent = if (role == "admin") {
            Intent(this, AdminActivity::class.java)
        } else {
            Intent(this, EmployeeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        ExitHelper.confirmAndExit(this)
    }
}
