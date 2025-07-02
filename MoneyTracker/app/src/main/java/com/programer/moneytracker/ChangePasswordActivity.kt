package com.programer.moneytracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var btnChangePassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.change_password_title)

        auth = FirebaseAuth.getInstance()

        etCurrentPassword = findViewById(R.id.et_current_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password)
        btnChangePassword = findViewById(R.id.btn_change_password)

        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun changePassword() {
        val user = auth.currentUser
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

        if (user == null) {
            Toast.makeText(this, getString(R.string.reauthentication_required), Toast.LENGTH_SHORT).show()
            // Arahkan ke WelcomeActivity jika pengguna tidak login
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_password_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, getString(R.string.password_min_length_error), Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmNewPassword) {
            Toast.makeText(this, getString(R.string.new_password_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        // --- Langkah 1: Autentikasi Ulang Pengguna ---
        // Ini adalah langkah keamanan penting sebelum mengubah kata sandi atau email.
        // Firebase mengharuskan pengguna untuk mengautentikasi ulang dengan kredensial terbaru mereka.
        val credential = user.email?.let { EmailAuthProvider.getCredential(it, currentPassword) }

        if (credential == null) {
            Toast.makeText(this, getString(R.string.reauthentication_required), Toast.LENGTH_SHORT).show()
            return
        }

        user.reauthenticate(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("ChangePasswordActivity", "User re-authenticated.")
                    // --- Langkah 2: Ubah Kata Sandi ---
                    user.updatePassword(newPassword)
                        .addOnCompleteListener(this) { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("ChangePasswordActivity", "User password updated.")
                                Toast.makeText(this, getString(R.string.password_change_success), Toast.LENGTH_SHORT).show()
                                finish() // Tutup Activity setelah berhasil
                            } else {
                                Log.e("ChangePasswordActivity", "Error updating password", updateTask.exception)
                                Toast.makeText(this, "${getString(R.string.password_change_failed)}: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Log.e("ChangePasswordActivity", "Re-authentication failed", task.exception)
                    if (task.exception?.message?.contains("credential is incorrect") == true) {
                        Toast.makeText(this, getString(R.string.old_password_incorrect), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "${getString(R.string.password_change_failed)}: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
}
