
package com.example.lab9

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherActivity : BaseActivity() {

    private lateinit var citySpinner: Spinner
    private lateinit var weatherIcon: ImageView
    private lateinit var temperatureText: TextView
    private lateinit var weatherDescription: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView
    private lateinit var dateText: TextView
    private lateinit var progressBar: ProgressBar

    private val cities = arrayOf("Саратов", "Энгельс", "Москва", "Якутск", "Калининград", "Владивосток", "Сочи")

    override fun onCreate(savedInstanceState: Bundle?) {
        currentNavItemId = R.id.nav_weather
        super.onCreate(savedInstanceState)

        layoutInflater.inflate(R.layout.activity_weather, findViewById(R.id.content_frame))

        initViews()
        setupCitySpinner()

        fetchWeatherData(cities[0])
    }

    private fun initViews() {
        citySpinner = findViewById(R.id.citySpinner)
        weatherIcon = findViewById(R.id.weatherIcon)
        temperatureText = findViewById(R.id.temperatureText)
        weatherDescription = findViewById(R.id.weatherDescription)
        humidityText = findViewById(R.id.humidityText)
        windText = findViewById(R.id.windText)
        dateText = findViewById(R.id.dateText)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupCitySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = adapter

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchWeatherData(cities[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchWeatherData(city: String) {
        progressBar.isVisible = true
        val apiKey = "1c8cad50173a6da7f90ec49368a06df5"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&lang=ru&appid=$apiKey"

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                parseWeatherData(response)
                progressBar.isVisible = false
            },
            { error ->
                Toast.makeText(this, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                progressBar.isVisible = false
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun parseWeatherData(response: JSONObject) {
        try {
            val main = response.getJSONObject("main")
            val temp = main.getDouble("temp")
            val humidity = main.getInt("humidity")

            val weatherArray = response.getJSONArray("weather")
            val weather = weatherArray.getJSONObject(0)
            val description = weather.getString("description")
            val icon = weather.getString("icon")

            val wind = response.getJSONObject("wind")
            val windSpeed = wind.getDouble("speed")

            val cityName = response.getString("name")

            val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
            val date = dateFormat.format(Date())

            runOnUiThread {
                temperatureText.text = "${temp.toInt()}°C"
                weatherDescription.text = description.capitalize()
                humidityText.text = "Влажность: $humidity%"
                windText.text = "Ветер: ${windSpeed.toInt()} м/с"
                dateText.text = "$cityName, $date"
                setWeatherIcon(icon)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setWeatherIcon(iconCode: String) {
        val iconRes = when (iconCode) {
            "01d" -> R.drawable.ic_sunny
            "01n" -> R.drawable.ic_clear_night
            "02d" -> R.drawable.ic_cloud
            "02n" -> R.drawable.ic_cloud
            "03d", "03n" -> R.drawable.ic_cloud
            "04d", "04n" -> R.drawable.ic_cloud
            "09d", "09n" -> R.drawable.ic_rain
            "10d" -> R.drawable.ic_rain
            "10n" -> R.drawable.ic_rain
            "11d", "11n" -> R.drawable.ic_thunderstorm
            "13d", "13n" -> R.drawable.ic_snowing
            else -> R.drawable.ic_unknown
        }

        weatherIcon.setImageResource(iconRes)

        if (isDarkTheme()) {
            weatherIcon.setColorFilter(
                ContextCompat.getColor(this, android.R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            weatherIcon.clearColorFilter()
        }
    }
}