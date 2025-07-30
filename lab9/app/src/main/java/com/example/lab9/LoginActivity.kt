
package com.example.lab9

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        db = FirebaseFirestore.getInstance()


        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                login.isEmpty() -> showError("Введите логин")
                password.isEmpty() -> showError("Введите пароль")
                else -> authenticateUser(login, password)
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun authenticateUser(login: String, password: String) {

        db.collection("users")
            .whereEqualTo("login", login)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showError("Пользователь не найден")
                } else {

                    val userDoc = documents.documents[0]
                    val storedPassword = userDoc.getString("password") ?: ""

                    if (storedPassword == password) {




                        navigateToProfile(userDoc.id)
                    } else {
                        showError("Неверный пароль")
                    }
                }
            }
            .addOnFailureListener { e ->
                showError("Ошибка входа: ${e.message}")
            }
    }



    private fun navigateToProfile(userId: String) {
        startActivity(
            Intent(this, ProfileActivity::class.java).apply {
                putExtra("USER_ID", userId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}