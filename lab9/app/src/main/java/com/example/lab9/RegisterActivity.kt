
package com.example.lab9

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                findViewById<ImageView>(R.id.ivAvatar).setImageURI(uri)

            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri?.let { uri ->
                findViewById<ImageView>(R.id.ivAvatar).setImageURI(uri)

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        db = FirebaseFirestore.getInstance()

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etBirthDate = findViewById<TextView>(R.id.etBirthDate)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnLoadAvatar = findViewById<Button>(R.id.btnLoadAvatar)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)

        btnBack.setOnClickListener { finish() }

        etBirthDate.setOnClickListener {
            showDatePickerDialog(etBirthDate)
        }

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val login = etLogin.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()

            if (validateInput(firstName, lastName, login, password, confirmPassword, birthDate)) {
                registerUser(firstName, lastName, login, password, birthDate)
            }
        }

        btnLoadAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Требуется разрешение на использование камеры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    selectedImageUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun showDatePickerDialog(etBirthDate: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val minCalendar = Calendar.getInstance()
        minCalendar.set(1900, Calendar.JANUARY, 1)

        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, -1 )

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d.%02d.%d", selectedDay, selectedMonth + 1, selectedYear)
            etBirthDate.setText(formattedDate)
        }, year, month, day).apply {
            datePicker.minDate = minCalendar.timeInMillis
            datePicker.maxDate = maxCalendar.timeInMillis
            show()
        }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        login: String,
        password: String,
        confirmPassword: String,
        birthDate: String
    ): Boolean {
        if (firstName.isEmpty()) {
            showError("Введите имя")
            return false
        }
        if (!firstName.matches(Regex("^[a-zA-Z]+$"))) {
            showError("Имя может содержать только латиницу")
            return false
        }
        if (lastName.isEmpty()) {
            showError("Введите фамилию")
            return false
        }
        if (!lastName.matches(Regex("^[a-zA-Z]+$"))) {
            showError("Фамилия может содержать только латиницу")
            return false
        }
        if (login.isEmpty()) {
            showError("Введите логин")
            return false
        }
        if (!login.matches(Regex("^[a-zA-Z0-9]+$"))) {
            showError("Логин может содержать только латиницу и цифры")
            return false
        }
        if (password.isEmpty()) {
            showError("Введите пароль")
            return false
        }
        if (password.length < 6) {
            showError("Пароль должен содержать минимум 6 символов")
            return false
        }

        if (!password.matches(Regex(".*[0-9].*"))) {
            showError("Пароль должен содержать хотя бы одну цифру")
            return false
        }
        if (password != confirmPassword) {
            showError("Пароли не совпадают")
            return false
        }
        if (birthDate.isEmpty()) {
            showError("Введите дату рождения")
            return false
        }
        return true
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun registerUser(
        firstName: String,
        lastName: String,
        login: String,
        password: String,
        birthDate: String
    ) {
        db.collection("users")
            .whereEqualTo("login", login)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    saveUserData(firstName, lastName, login, password, birthDate)
                } else {
                    showError("Этот логин уже занят")
                }
            }
            .addOnFailureListener { e ->
                showError("Ошибка проверки логина: ${e.message}")
            }
    }

    private fun saveUserData(
        firstName: String,
        lastName: String,
        login: String,
        password: String,
        birthDate: String
    ) {
        val avatarBase64 = selectedImageUri?.let { uri ->
            try {
                val bitmap = correctImageOrientation(uri)
                convertBitmapToBase64(bitmap)
            } catch (e: Exception) {
                Log.e("Register", "Ошибка обработки аватара", e)
                null
            }
        } ?: "default"

        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "login" to login,
            "password" to password,
            "birthDate" to birthDate,
            "avatarBase64" to avatarBase64,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .add(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                showError("Ошибка сохранения данных: ${e.message}")
            }
    }

    private fun correctImageOrientation(uri: Uri): Bitmap {
        // открываем поток для чтения изображения
        val inputStream = contentResolver.openInputStream(uri)!!
        // читаем EXIF данные
        val exif = ExifInterface(inputStream)
        inputStream.close()
        // получаем информацию об ориентации
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        // декодируем изображение в Bitmap
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        // корректируем ориентацию
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }



    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }



}