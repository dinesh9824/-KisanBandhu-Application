package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import coil.load
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class WeatherInfoActivity : SwipeableActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_info)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        setupBottomNavigation()
        observeViewModel()
        checkLocationPermission()
    }

    private fun observeViewModel() {
        viewModel.weatherData.observe(this) { data ->
            if (data != null) {
                updateUI(data)
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001)
            return
        }
        getLocationAndFetchWeather()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndFetchWeather()
        } else {
            Toast.makeText(this, "Location permission denied. Showing default weather.", Toast.LENGTH_SHORT).show()
            viewModel.fetchWeather("Pune")
        }
    }

    private fun getLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.fetchWeather("${location.latitude},${location.longitude}")
                } else {
                    viewModel.fetchWeather("Pune")
                }
            }.addOnFailureListener {
                viewModel.fetchWeather("Pune")
            }
        }
    }

    private fun updateUI(data: WeatherResponse) {
        // Update Text Fields
        findViewById<TextView>(R.id.tv_location_name)?.text = "${data.location.name}, ${data.location.region}"
        findViewById<TextView>(R.id.tv_current_temp)?.text = "${data.current.tempC.toInt()}°C"
        findViewById<TextView>(R.id.tv_weather_condition)?.text = data.current.condition.text
        findViewById<TextView>(R.id.tv_humidity)?.text = "${data.current.humidity}%"
        findViewById<TextView>(R.id.tv_wind_speed)?.text = "${data.current.windKph.toInt()} km/h"
        findViewById<TextView>(R.id.tv_visibility)?.text = "${data.current.visKm.toInt()} km"
        
        // Load Main Icon
        val iconUrl = "https:${data.current.condition.icon}".replace("64x64", "128x128")
        findViewById<ImageView>(R.id.iv_weather_icon)?.load(iconUrl)

        // Update Forecast
        val forecastContainer = findViewById<LinearLayout>(R.id.ll_forecast_container) ?: return
        if (forecastContainer.childCount > 1) {
            forecastContainer.removeViews(1, forecastContainer.childCount - 1)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        data.forecast.forecastDay.forEach { dayData ->
            val date = sdf.parse(dayData.date)
            val dayName = dayFormat.format(date ?: Date())

            val itemView = LayoutInflater.from(this).inflate(R.layout.item_forecast_day, forecastContainer, false)
            itemView.findViewById<TextView>(R.id.tv_day_name).text = dayName
            itemView.findViewById<TextView>(R.id.tv_day_condition).text = dayData.day.condition.text
            itemView.findViewById<TextView>(R.id.tv_day_temp).text = "${dayData.day.maxTempC.toInt()}° ${dayData.day.minTempC.toInt()}°"
            itemView.findViewById<ImageView>(R.id.iv_day_icon).load("https:${dayData.day.condition.icon}")
            
            forecastContainer.addView(itemView)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        bottomNav.selectedItemId = R.id.nav_weather
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                R.id.nav_market -> {
                    val intent = Intent(this, MarketAnalysisActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                R.id.nav_weather -> true
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_weather
    }
}