package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import coil.load
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : SwipeableActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Card button clicks
        findViewById<MaterialCardView>(R.id.btn_crop_recommendation)?.setOnClickListener {
            startActivity(Intent(this, CropRecommendationActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.btn_crop_health)?.setOnClickListener {
            try {
                val intent = Intent(this, CropHealthActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error starting scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        findViewById<MaterialCardView>(R.id.btn_market_analysis)?.setOnClickListener {
            val intent = Intent(this, MarketAnalysisActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        // Location click to update
        findViewById<View>(R.id.layout_location)?.setOnClickListener {
            checkLocationPermission()
        }

        setupBottomNavigation()
        observeWeather()
        loadUserName()
        
        // Initial location check
        checkLocationPermission()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { document, error ->
            if (error != null) return@addSnapshotListener
            
            if (document != null && document.exists()) {
                val name = document.getString("name")
                if (!name.isNullOrEmpty()) {
                    val welcomeText = getString(R.string.welcome) + " " + name + "!"
                    findViewById<TextView>(R.id.tv_welcome_name)?.text = welcomeText
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1002)
            return
        }
        updateLocationAndWeather()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocationAndWeather()
        }
    }

    private fun updateLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    weatherViewModel.fetchWeather("${location.latitude},${location.longitude}")
                } else {
                    weatherViewModel.fetchWeather("Pune")
                }
            }
        }
    }

    private fun observeWeather() {
        weatherViewModel.weatherData.observe(this) { data ->
            if (data != null) {
                // Update Location Bar
                findViewById<TextView>(R.id.tv_current_location)?.text = "${getString(R.string.location_prefix)}: ${data.location.name}, ${data.location.region}"
                findViewById<TextView>(R.id.tv_location_mode)?.text = "" // Clear mode once live
                
                // Update Weather Card
                findViewById<TextView>(R.id.tv_weather_temp_main)?.text = "${data.current.tempC.toInt()}°C | ${data.current.condition.text}"
                findViewById<TextView>(R.id.tv_weather_details_main)?.text = "Humidity: ${data.current.humidity}% | Wind: ${data.current.windKph.toInt()} km/h"
                
                val iconUrl = "https:${data.current.condition.icon}".replace("64x64", "128x128")
                findViewById<ImageView>(R.id.iv_weather_icon_main)?.load(iconUrl)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_market -> {
                    val intent = Intent(this, MarketAnalysisActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                R.id.nav_weather -> {
                    val intent = Intent(this, WeatherInfoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
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
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
    }
}