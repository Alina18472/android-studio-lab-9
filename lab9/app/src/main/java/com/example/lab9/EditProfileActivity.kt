
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

class EditProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private lateinit var userId: String
    private lateinit var etBirthDate: EditText
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
        setContentView(R.layout.activity_edit_profile)

        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: run {
            Toast.makeText(this, "Ошибка: ID пользователя не получен", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etBirthDate = findViewById(R.id.etBirthDate)
        loadUserData()


        etBirthDate.setOnClickListener {
            showDatePickerDialog(etBirthDate)
        }

        findViewById<Button>(R.id.btnLoadAvatar).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveChanges()
        }
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(packageManager)?.also {
                val photoFile = try {
                    createImageFile()
                } catch (e: IOException) {
                    null
                }
                photoFile?.also {
                    val photoUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    selectedImageUri = photoUri
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    cameraLauncher.launch(intent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
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
            Toast.makeText(this, "Требуется разрешение камеры", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val minCalendar = Calendar.getInstance()
        minCalendar.set(1900, Calendar.JANUARY, 1)

        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, -1)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d.%02d.%d", selectedDay, selectedMonth + 1, selectedYear)
            editText.setText(formattedDate)
        }, year, month, day).apply {
            datePicker.minDate = minCalendar.timeInMillis
            datePicker.maxDate = maxCalendar.timeInMillis
            show()
        }
    }

    private fun loadUserData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    findViewById<EditText>(R.id.etFirstName).setText(document.getString("firstName"))
                    findViewById<EditText>(R.id.etLastName).setText(document.getString("lastName"))
                    findViewById<EditText>(R.id.etLogin).setText(document.getString("login"))
                    etBirthDate.setText(document.getString("birthDate"))

                    document.getString("avatarBase64")?.takeIf { it != "default" }?.let { base64 ->
                        val bitmap = convertBase64ToBitmap(base64)
                        findViewById<ImageView>(R.id.ivAvatar).setImageBitmap(bitmap)
                    }
                } else {
                    Toast.makeText(this, "Данные пользователя не найдены", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val firstName = findViewById<EditText>(R.id.etFirstName).text.toString().trim()
        val lastName = findViewById<EditText>(R.id.etLastName).text.toString().trim()
        val newLogin = findViewById<EditText>(R.id.etLogin).text.toString().trim()
        val birthDate = etBirthDate.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || newLogin.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }
        val nameRegex = Regex("^[a-zA-Z]+$")
        if (!firstName.matches(nameRegex)) {
            findViewById<EditText>(R.id.etFirstName).error = "Только латинские буквы"
            return
        }
        if (!lastName.matches(nameRegex)) {
            findViewById<EditText>(R.id.etLastName).error = "Только латинские буквы"
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val currentLogin = document.getString("login") ?: ""

                if (newLogin != currentLogin) {
                    checkLoginAvailability(newLogin) { isAvailable ->
                        if (isAvailable) {
                            updateUserData(firstName, lastName, newLogin, birthDate)
                        } else {
                            Toast.makeText(this, "Этот логин уже занят", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    updateUserData(firstName, lastName, newLogin, birthDate)
                }
            }
    }

    private fun checkLoginAvailability(newLogin: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("login", newLogin)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun updateUserData(
        firstName: String,
        lastName: String,
        login: String,
        birthDate: String
    ) {
        val avatarBase64 = selectedImageUri?.let { uri ->
            try {
                val bitmap = correctImageOrientation(uri)
                convertBitmapToBase64(bitmap)
            } catch (e: Exception) {
                Log.e("EditProfile", "Ошибка обработки аватара", e)
                null
            }
        }

        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "login" to login,
            "birthDate" to birthDate
        )

        avatarBase64?.let {
            updates["avatarBase64"] = it
        }

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun correctImageOrientation(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)!!
        val exif = ExifInterface(inputStream)
        inputStream.close()

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

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

    private fun convertBase64ToBitmap(base64Str: String): Bitmap {
        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


}