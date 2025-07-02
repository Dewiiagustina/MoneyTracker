package com.programer.moneytracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView // <--- Tambahkan import ImageView jika belum ada
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileHeaderName: TextView // Ini adalah TextView untuk Nama di bawah logo
    private lateinit var btnChangePasswordProfile: Button
    private lateinit var btnLogout: Button

    // ImageView untuk profil (logo) yang juga sudah ada di layout
    private lateinit var ivProfilePhoto: ImageView // <--- Deklarasi ImageView baru/update


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // supportActionBar?.title akan diatur secara dinamis (tetap sama)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inisialisasi komponen UI
        // tvProfileFullName = findViewById(R.id.tv_profile_full_name) // <--- HAPUS INISIALISASI INI
        tvProfileEmail = findViewById(R.id.tv_profile_email)
        tvProfileHeaderName = findViewById(R.id.tv_profile_header_name) // Inisialisasi TextView untuk nama di bawah logo
        btnChangePasswordProfile = findViewById(R.id.btn_change_password_profile)
        btnLogout = findViewById(R.id.btn_logout)

        // Inisialisasi ImageView untuk logo/gambar profile (jika ada)
        val ivProfilePhoto: ImageView = findViewById(R.id.ivProfilePhoto) // Asumsi ID adalah 'profile' dari XML baru Anda

        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvProfileEmail.text = (currentUser.email ?: "N/A") // Format email lebih sederhana

            val userId = currentUser.uid
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName")

                        // Atur nama lengkap di TextView header
                        tvProfileHeaderName.text = fullName ?: getString(R.string.profile_page_title) // Fallback ke "Nama User"

                        // Baris ini tidak lagi diperlukan karena tvProfileFullName dihapus dari layout
                        // tvProfileFullName.text = getString(R.string.full_name_display) + " " + (fullName ?: "Belum Diatur")

                        // Atur judul ActionBar secara dinamis
                        supportActionBar?.title = fullName ?: getString(R.string.profile_page_title)
                    } else {
                        Log.d("ProfileActivity", "No such document for user $userId")
                        tvProfileHeaderName.text = getString(R.string.profile_page_title)
                        // tvProfileFullName.text = getString(R.string.full_name_display) + " Belum Diatur (Firestore)"
                        supportActionBar?.title = getString(R.string.profile_page_title)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("ProfileActivity", "Error getting user data from Firestore", exception)
                    tvProfileHeaderName.text = getString(R.string.profile_page_title)
                    // tvProfileFullName.text = getString(R.string.full_name_display) + " Gagal Memuat"
                    supportActionBar?.title = getString(R.string.profile_page_title)
                }
        } else {
            Log.e("ProfileActivity", "No current user, redirecting to WelcomeActivity.")
            Toast.makeText(this, "Tidak ada pengguna login, mohon login kembali.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnChangePasswordProfile.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Anda telah keluar.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
