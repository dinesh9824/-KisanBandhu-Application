package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CropRecommendationActivity : BaseActivity() {

    private lateinit var cardSelectSource: MaterialCardView
    private lateinit var layoutInputForm: LinearLayout
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var tvSelectedSourceName: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Parameters for ML model
    private var nitrogen: Int = 90
    private var phosphorus: Int = 45
    private var potassium: Int = 43
    private var phLevel: Double = 6.8
    private var temperature: Double = 28.0
    private var humidity: Double = 60.0
    private var rainfall: Double = 100.0
    private var currentCrops: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_recommendation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cardSelectSource = findViewById(R.id.card_select_source)
        layoutInputForm = findViewById(R.id.layout_input_form)
        btnAnalyze = findViewById(R.id.btn_analyze)
        tvSelectedSourceName = findViewById(R.id.tv_selected_source_name)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btn_choose_source).setOnClickListener { showChooseSourceDialog() }
        findViewById<View>(R.id.btn_change_source)?.setOnClickListener { showChooseSourceDialog() }
        findViewById<View>(R.id.btn_npk).setOnClickListener { showNPKBottomSheet() }
        findViewById<View>(R.id.btn_ph).setOnClickListener { showPHBottomSheet() }
        findViewById<View>(R.id.btn_humidity).setOnClickListener { showHumidityBottomSheet() }
        
        findViewById<MaterialButton>(R.id.btn_edit_history).setOnClickListener { showEditHistoryDialog() }

        setupBottomNavigation()
        observeWeather()
        fetchLocationAndWeather()
        loadCropHistory()

        btnAnalyze.setOnClickListener {
            val intent = Intent(this, RecommendationResultActivity::class.java).apply {
                putExtra("N", nitrogen)
                putExtra("P", phosphorus)
                putExtra("K", potassium)
                putExtra("PH", phLevel)
                putExtra("TEMP", temperature)
                putExtra("HUMIDITY", humidity)
                putExtra("RAINFALL", rainfall)
            }
            startActivity(intent)
        }
    }

    private fun showEditHistoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.edit_history))
        val input = EditText(this)
        input.setText(currentCrops)
        builder.setView(input)
        builder.setPositiveButton(getString(R.string.save_value)) { dialog, _ ->
            val newHistory = input.text.toString()
            updateCropHistory(newHistory)
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateCropHistory(newHistory: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("currentCrops", newHistory)
            .addOnSuccessListener {
                // Removed toast for pure UI logic
                currentCrops = newHistory
            }
    }

    private fun loadCropHistory() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { document, _ ->
            if (document != null && document.exists()) {
                val crops = document.getString("currentCrops")
                if (!crops.isNullOrEmpty()) {
                    currentCrops = crops
                }
            }
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_market -> {
                    startActivity(Intent(this, MarketAnalysisActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_weather -> {
                    startActivity(Intent(this, WeatherInfoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchLocationAndWeather() {
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
                temperature = data.current.tempC
                humidity = data.current.humidity.toDouble()
                rainfall = data.forecast.forecastDay.getOrNull(0)?.day?.totalPrecipMm ?: data.current.precipMm
                if (rainfall == 0.0) rainfall = 100.0
                updateLiveUI()
            }
        }
    }

    private fun updateLiveUI() {
        val tvTemp = findViewById<TextView>(R.id.tv_live_temp)
        val tvRain = findViewById<TextView>(R.id.tv_live_rainfall)
        
        tvTemp?.text = "${temperature.toInt()}°C"
        tvRain?.text = "${rainfall.toInt()} mm"
    }

    private fun showChooseSourceDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_choose_data_source, null)
        
        view.findViewById<LinearLayout>(R.id.option_soil_test_kit).setOnClickListener {
            updateSource(getString(R.string.soil_test_kit))
            dialog.dismiss()
        }
        
        view.findViewById<LinearLayout>(R.id.option_lab_report).setOnClickListener {
            updateSource(getString(R.string.lab_report))
            dialog.dismiss()
        }
        
        view.findViewById<LinearLayout>(R.id.option_previous_records).setOnClickListener {
            updateSource(getString(R.string.previous_records))
            dialog.dismiss()
        }
        
        view.findViewById<LinearLayout>(R.id.option_estimate_values).setOnClickListener {
            updateSource(getString(R.string.estimate_values))
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun updateSource(sourceName: String) {
        tvSelectedSourceName.text = "${getString(R.string.data_source_pref)}: $sourceName"
        switchToInputForm()
    }

    private fun showNPKBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.activity_npk_input, null)
        
        view.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }

        val etN = view.findViewById<EditText>(R.id.et_nitrogen)
        val etP = view.findViewById<EditText>(R.id.et_phosphorus)
        val etK = view.findViewById<EditText>(R.id.et_potassium)

        etN.setText(nitrogen.toString())
        etP.setText(phosphorus.toString())
        etK.setText(potassium.toString())

        view.findViewById<ImageButton>(R.id.btn_n_up).setOnClickListener { changeValue(etN, 1) }
        view.findViewById<ImageButton>(R.id.btn_n_down).setOnClickListener { changeValue(etN, -1) }
        view.findViewById<ImageButton>(R.id.btn_p_up).setOnClickListener { changeValue(etP, 1) }
        view.findViewById<ImageButton>(R.id.btn_p_down).setOnClickListener { changeValue(etP, -1) }
        view.findViewById<ImageButton>(R.id.btn_k_up).setOnClickListener { changeValue(etK, 1) }
        view.findViewById<ImageButton>(R.id.btn_k_down).setOnClickListener { changeValue(etK, -1) }

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            nitrogen = etN.text.toString().toIntOrNull() ?: nitrogen
            phosphorus = etP.text.toString().toIntOrNull() ?: phosphorus
            potassium = etK.text.toString().toIntOrNull() ?: potassium
            findViewById<TextView>(R.id.tv_npk_summary)?.text = "N: $nitrogen | P: $phosphorus | K: $potassium"
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun changeValue(editText: EditText, delta: Int) {
        val currentVal = editText.text.toString().toIntOrNull() ?: 0
        editText.setText((currentVal + delta).coerceAtLeast(0).toString())
    }

    private fun showPHBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.activity_ph_input, null)
        
        view.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }

        val tvValue = view.findViewById<TextView>(R.id.tv_ph_value)
        val slider = view.findViewById<Slider>(R.id.slider_ph)
        val tvStatus = view.findViewById<TextView>(R.id.tv_ph_status)

        slider.value = phLevel.toFloat()
        
        fun updatePhUI(value: Float) {
            tvValue.text = String.format("%.1f", value)
            when {
                value < 6.0 -> {
                    tvStatus?.text = getString(R.string.ph_status_acidic)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                }
                value <= 7.5 -> {
                    tvStatus?.text = getString(R.string.ph_status_neutral)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.brand_green))
                }
                else -> {
                    tvStatus?.text = getString(R.string.ph_status_alkaline)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_purple))
                }
            }
        }
        
        updatePhUI(phLevel.toFloat())

        slider.addOnChangeListener { _, value, _ -> 
            updatePhUI(value)
        }
        
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            phLevel = slider.value.toDouble()
            findViewById<TextView>(R.id.tv_ph_summary)?.text = String.format("%.1f", phLevel)
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showHumidityBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.activity_moisture_input, null)
        
        // Use the consistent btn_close ID now
        view.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
        
        val btnUp = view.findViewById<ImageButton>(R.id.btn_moisture_up)
        val btnDown = view.findViewById<ImageButton>(R.id.btn_moisture_down)

        val tvValue = view.findViewById<TextView>(R.id.tv_moisture_value)
        val slider = view.findViewById<Slider>(R.id.slider_moisture)
        val etHum = view.findViewById<EditText>(R.id.et_moisture)
        val tvStatus = view.findViewById<TextView>(R.id.tv_moisture_status)

        slider.value = humidity.toFloat()
        etHum?.setText(humidity.toInt().toString())
        
        var isUpdating = false

        fun updateMoistureUI(value: Float, updateSlider: Boolean, updateEditText: Boolean) {
            isUpdating = true
            
            tvValue.text = "${value.toInt()}%"
            if (updateSlider) slider.value = value
            if (updateEditText) etHum?.setText(value.toInt().toString())
            
            when {
                value < 30 -> {
                    tvStatus?.text = getString(R.string.moisture_dry)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                }
                value <= 60 -> {
                    tvStatus?.text = getString(R.string.moisture_good)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.brand_green))
                }
                else -> {
                    tvStatus?.text = getString(R.string.moisture_wet)
                    tvStatus?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                }
            }
            
            isUpdating = false
        }
        
        updateMoistureUI(humidity.toFloat(), updateSlider = false, updateEditText = false)
        
        btnUp?.setOnClickListener {
            val currentVal = etHum?.text.toString().toFloatOrNull() ?: 0f
            if(currentVal < 100) updateMoistureUI(currentVal + 1, true, true)
        }
        
        btnDown?.setOnClickListener {
            val currentVal = etHum?.text.toString().toFloatOrNull() ?: 0f
            if(currentVal > 0) updateMoistureUI(currentVal - 1, true, true)
        }

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdating) {
                updateMoistureUI(value, updateSlider = false, updateEditText = true)
            }
        }
        
        etHum?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdating) {
                    val value = s?.toString()?.toFloatOrNull()
                    if (value != null && value in 0f..100f) {
                        updateMoistureUI(value, updateSlider = true, updateEditText = false)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val humVal = etHum?.text.toString().toDoubleOrNull() ?: humidity
            humidity = humVal
            findViewById<TextView>(R.id.tv_humidity_summary)?.text = "${humidity.toInt()} %"
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun switchToInputForm() {
        cardSelectSource.visibility = View.GONE
        layoutInputForm.visibility = View.VISIBLE
        btnAnalyze.visibility = View.VISIBLE
    }
}