package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class ProfileSetupActivity : AppCompatActivity() {

    private var selectedLocation: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    
    private lateinit var etName: EditText
    private lateinit var btnStart: MaterialButton

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                Toast.makeText(this, "Photo Captured!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        etName = findViewById(R.id.et_setup_name)
        btnStart = findViewById(R.id.btn_start_farming)
        val btnTakePhoto = findViewById<MaterialButton>(R.id.btn_take_photo)
        val btnGetLocation = findViewById<MaterialButton>(R.id.btn_get_location)

        setupLocationSelection()
        updateButtonStyle()

        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonStyle()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnGetLocation.setOnClickListener {
            checkLocationPermission()
        }

        btnTakePhoto.setOnClickListener {
            if (checkCameraPermission()) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(intent)
            } else {
                requestCameraPermission()
            }
        }

        btnStart.setOnClickListener {
            val name = etName.text.toString()
            if (name.isEmpty() || selectedLocation == null) {
                Toast.makeText(this, "Please enter your name and select location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveProfileAndStart(name, selectedLocation!!)
        }
    }

    private fun updateButtonStyle() {
        val isReady = etName.text.toString().isNotEmpty() && selectedLocation != null
        if (isReady) {
            btnStart.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        } else {
            btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#81C784"))
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: "Unknown Location"
                        val state = address.adminArea ?: ""
                        selectedLocation = "$city, $state"
                        
                        // Show current location visually
                        val tvPune = findViewById<TextView>(R.id.loc_pune)
                        val tvDelhi = findViewById<TextView>(R.id.loc_delhi)
                        val tvMumbai = findViewById<TextView>(R.id.loc_mumbai)
                        listOf(tvPune, tvDelhi, tvMumbai).forEach { it.setBackgroundResource(R.drawable.input_field_bg) }
                        
                        Toast.makeText(this, "Location found: $selectedLocation", Toast.LENGTH_SHORT).show()
                        updateButtonStyle()
                    }
                }
            }
        }
    }

    private fun checkCameraPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun requestCameraPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1004)

    private fun setupLocationSelection() {
        val tvPune = findViewById<TextView>(R.id.loc_pune)
        val tvDelhi = findViewById<TextView>(R.id.loc_delhi)
        val tvMumbai = findViewById<TextView>(R.id.loc_mumbai)

        val locationViews = listOf(tvPune, tvDelhi, tvMumbai)
        locationViews.forEach { view ->
            view.setOnClickListener {
                locationViews.forEach { it.setBackgroundResource(R.drawable.input_field_bg) }
                view.setBackgroundResource(R.drawable.option_item_selector)
                selectedLocation = view.text.toString().replace("📍 ", "")
                updateButtonStyle()
            }
        }
    }

    private fun saveProfileAndStart(name: String, location: String) {
        val uid = auth.currentUser?.uid ?: return
        btnStart.isEnabled = false
        btnStart.text = "Setting up..."

        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "location" to location,
            "mobileNumber" to (auth.currentUser?.phoneNumber ?: ""),
            "language" to "English",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(userMap, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnStart.isEnabled = true
                btnStart.text = "START FARMING / शुरू करें"
            }
    }
}