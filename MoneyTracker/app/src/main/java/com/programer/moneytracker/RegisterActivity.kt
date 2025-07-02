package com.programer.moneytracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log // Import Log untuk debugging
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Import FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore // Deklarasi Firestore
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvAlreadyHaveAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance() // Inisialisasi Firestore

        etFullName = findViewById(R.id.et_full_name_register)
        etEmail = findViewById(R.id.et_email_register)
        etPassword = findViewById(R.id.et_password_register)
        etConfirmPassword = findViewById(R.id.et_confirm_password_register)
        btnRegister = findViewById(R.id.btn_register)
        tvAlreadyHaveAccount = findViewById(R.id.tv_already_have_account)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, getString(R.string.password_length_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.password_mismatch_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Registrasi sukses, dapatkan UID pengguna
                        val user = auth.currentUser
                        val userId = user?.uid

                        if (userId != null) {
                            // Buat objek data pengguna untuk disimpan di Firestore
                            val userMap = hashMapOf(
                                "fullName" to fullName,
                                "email" to email
                            )

                            // Simpan data pengguna di koleksi 'users' dengan UID sebagai ID dokumen
                            firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Log.d("RegisterActivity", "User data added to Firestore for $userId")
                                    Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
                                    // Arahkan ke MainActivity setelah registrasi berhasil
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("RegisterActivity", "Error adding user data to Firestore", e)
                                    // Handle kegagalan penyimpanan data (opsional, bisa tetap login)
                                    Toast.makeText(this, "${getString(R.string.registration_success_but_data_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                                    // Tetap arahkan ke MainActivity meskipun data Firestore gagal disimpan
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        } else {
                            // Ini seharusnya tidak terjadi jika task.isSuccessful
                            Toast.makeText(this, getString(R.string.registration_failed) + ": User UID is null", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Registrasi gagal di Firebase Auth
                        Toast.makeText(this, "${getString(R.string.registration_failed)}: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // ... (OnClickListener untuk tvAlreadyHaveAccount) ...
        val alreadyHaveAccountText = "${getString(R.string.already_have_account)} ${getString(R.string.login_link_text)}"
        val spannableString = SpannableString(alreadyHaveAccountText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = Color.parseColor("#4CAF50")
            }
        }

        val loginText = getString(R.string.login_link_text)
        val startIndex = alreadyHaveAccountText.indexOf(loginText)
        val endIndex = startIndex + loginText.length

        if (startIndex != -1) {
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        tvAlreadyHaveAccount.text = spannableString
        tvAlreadyHaveAccount.movementMethod = LinkMovementMethod.getInstance()
    }
}
