package com.programer.moneytracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvDontHaveAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Sembunyikan ActionBar jika tidak diperlukan
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.et_email_login)
        etPassword = findViewById(R.id.et_password_login)
        btnLogin = findViewById(R.id.btn_login)
        tvDontHaveAccount = findViewById(R.id.tv_dont_have_account)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proses login dengan Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login sukses
                        val user = auth.currentUser
                        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                        // Arahkan ke MainActivity setelah login berhasil
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Tutup LoginActivity
                    } else {
                        // Login gagal
                        Toast.makeText(this, "${getString(R.string.login_failed)}: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvDontHaveAccount.setOnClickListener {
            // Arahkan ke halaman Registrasi jika pengguna belum punya akun
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // Tutup LoginActivity
        }
    }
}
