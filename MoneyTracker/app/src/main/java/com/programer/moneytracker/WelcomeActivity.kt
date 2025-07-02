package com.programer.moneytracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Hide ActionBar for a cleaner welcome screen
        supportActionBar?.hide()

        val btnCreateAccount: Button = findViewById(R.id.btn_create_account)
        val btnLoginWelcome: Button = findViewById(R.id.btn_login_welcome)

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnLoginWelcome.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Periksa status login saat Activity dimulai
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Pengguna sudah login, arahkan ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup WelcomeActivity agar tidak bisa kembali ke sini dengan tombol back
        }
    }
}
