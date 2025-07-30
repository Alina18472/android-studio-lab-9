

package com.example.lab9

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: ImageView
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvLogin: TextView
    private lateinit var tvBirthDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_profile, findViewById(R.id.content_frame))
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: run {
            showErrorAndFinish("Ошибка авторизации")
            return
        }

        initViews()
        setupButtonListeners()
        loadUserData()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvLogin = findViewById(R.id.tvLogin)
        tvBirthDate = findViewById(R.id.tvBirthDate)
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            startActivityForResult(
                Intent(this, EditProfileActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                },
                EDIT_PROFILE_REQUEST
            )
        }

        findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    updateUIWithUserData(document)
                } else {
                    showError("Данные пользователя не найдены")
                    logout()
                }
            }
            .addOnFailureListener { e ->
                showError("Ошибка загрузки: ${e.message}")
            }
    }

    private fun updateUIWithUserData(document: com.google.firebase.firestore.DocumentSnapshot) {

        tvFirstName.text = document.getString("firstName")
        tvLastName.text = document.getString("lastName")
        tvLogin.text = document.getString("login")
        tvBirthDate.text = document.getString("birthDate")


        document.getString("avatarBase64")?.takeIf { it != "default" }?.let { base64 ->
            loadAvatar(base64)
        } ?: ivAvatar.setImageResource(R.drawable.ic_default_avatar)
    }

    private fun loadAvatar(avatarBase64: String) {
        try {
            val bitmap = convertBase64ToBitmap(avatarBase64)
            Glide.with(this)
                .load(bitmap)
                .circleCrop()
                .into(ivAvatar)
        } catch (e: Exception) {
            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun logout() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == Activity.RESULT_OK) {
            loadUserData()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun convertBase64ToBitmap(base64Str: String): Bitmap {
        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorAndFinish(message: String) {
        showError(message)
        finish()
    }


    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Смена пароля")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()


        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword).text.toString()
                val newPassword = dialogView.findViewById<EditText>(R.id.etNewPassword).text.toString()
                val confirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword).text.toString()

                if (validatePasswords(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword, dialog)
                }
            }
        }

        dialog.show()
    }
    private fun validatePasswords(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        return when {
            currentPassword.isEmpty() -> {
                Toast.makeText(this, "Введите текущий пароль", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword.isEmpty() -> {
                Toast.makeText(this, "Введите новый пароль", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword.length < 6 -> {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword != confirmPassword -> {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    private fun changePassword(currentPassword: String, newPassword: String, dialog: AlertDialog) {

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val storedPassword = document.getString("password") ?: ""

                if (storedPassword == currentPassword) {

                    db.collection("users").document(userId)
                        .update("password", newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Пароль изменён!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val EDIT_PROFILE_REQUEST = 1
    }
}