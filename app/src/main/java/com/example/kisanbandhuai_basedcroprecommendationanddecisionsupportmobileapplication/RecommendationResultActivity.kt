package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import androidx.lifecycle.ViewModelProvider

class RecommendationResultActivity : BaseActivity() {

    private lateinit var viewModel: CropRecommendationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation_result)

        // Using ViewModelProvider directly since it requires application context for ONNX
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CropRecommendationViewModel::class.java]

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val n = intent.getIntExtra("N", 0)
        val p = intent.getIntExtra("P", 0)
        val k = intent.getIntExtra("K", 0)
        val ph = intent.getDoubleExtra("PH", 0.0)
        val temp = intent.getDoubleExtra("TEMP", 0.0)
        val humidity = intent.getDoubleExtra("HUMIDITY", 0.0)
        val rainfall = intent.getDoubleExtra("RAINFALL", 0.0)

        updateInputUI(n, p, k, ph, temp, humidity, rainfall)

        observeViewModel()
        setupBottomNavigation()

        val request = CropRequest(
            N = n.toFloat(),
            P = p.toFloat(),
            K = k.toFloat(),
            temperature = temp.toFloat(),
            humidity = humidity.toFloat(),
            ph = ph.toFloat(),
            rainfall = rainfall.toFloat()
        )
        
        viewModel.predictCrop(request)
    }

    private fun observeViewModel() {
        val loadingLayout = findViewById<View>(R.id.layout_loading)
        val resultCard = findViewById<View>(R.id.card_result)

        viewModel.predictionResult.observe(this) { response ->
            if (response != null && response.success) {
                // Handle Top 3 Predictions
                val predictions = response.top_predictions
                
                if (predictions.isNotEmpty()) {
                    // Prediction 1 (Main)
                    val p1 = predictions[0]
                    findViewById<TextView>(R.id.tv_crop_1)?.text = getLocalizedCropName(p1.cropName)
                    findViewById<TextView>(R.id.tv_prob_1)?.text = "${(p1.probability * 100).toInt()}%"
                    updateCropIcon(p1.cropName, findViewById(R.id.crop_icon_1))
                    
                    // Prediction 2
                    if (predictions.size > 1) {
                        val p2 = predictions[1]
                        val tvCrop2 = findViewById<TextView>(R.id.tv_crop_2)
                        val tvProb2 = findViewById<TextView>(R.id.tv_prob_2)
                        
                        tvCrop2?.text = getLocalizedCropName(p2.cropName)
                        tvProb2?.text = "${(p2.probability * 100).toInt()}%"
                        // Set suitable color for 2nd rank (Blue)
                        tvProb2?.setTextColor(ContextCompat.getColor(this, R.color.button_blue))
                    }
                    
                    // Prediction 3
                    if (predictions.size > 2) {
                        val p3 = predictions[2]
                        val tvCrop3 = findViewById<TextView>(R.id.tv_crop_3)
                        val tvProb3 = findViewById<TextView>(R.id.tv_prob_3)
                        
                        tvCrop3?.text = getLocalizedCropName(p3.cropName)
                        tvProb3?.text = "${(p3.probability * 100).toInt()}%"
                        // Set suitable color for 3rd rank (Secondary Gray)
                        tvProb3?.setTextColor(ContextCompat.getColor(this, R.color.button_red))
                    }
                } else {
                    // Fallback for single result mode
                    val rawCropName = response.recommended_crop
                    findViewById<TextView>(R.id.tv_crop_1)?.text = getLocalizedCropName(rawCropName)
                    findViewById<TextView>(R.id.tv_prob_1)?.text = "100%"
                    updateCropIcon(rawCropName, findViewById(R.id.crop_icon_1))
                }
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            loadingLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
            resultCard.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun getLocalizedCropName(crop: String): String {
        val resName = "crop_" + crop.lowercase().replace(" ", "")
        val resId = resources.getIdentifier(resName, "string", packageName)
        
        return if (resId != 0) {
            getString(resId)
        } else {
            crop.replaceFirstChar { it.uppercase() }
        }
    }

    private fun updateInputUI(n: Int, p: Int, k: Int, ph: Double, t: Double, h: Double, r: Double) {
        findViewById<TextView>(R.id.tv_param_n)?.text = "$n kg/ha"
        findViewById<TextView>(R.id.tv_param_p)?.text = "$p kg/ha"
        findViewById<TextView>(R.id.tv_param_k)?.text = "$k kg/ha"
        findViewById<TextView>(R.id.tv_param_ph)?.text = String.format("%.1f", ph)
        findViewById<TextView>(R.id.tv_param_temp)?.text = "${t.toInt()}°C"
        findViewById<TextView>(R.id.tv_param_humidity)?.text = "${h.toInt()}%"
        findViewById<TextView>(R.id.tv_param_rainfall)?.text = "${r.toInt()} mm"
    }

    private fun updateCropIcon(crop: String, iconView: ImageView?) {
        if (iconView == null) return
        when (crop.lowercase()) {
            "rice" -> iconView.setImageResource(R.drawable.ic_leaf)
            "cotton" -> iconView.setImageResource(R.drawable.ic_sprout_logo)
            else -> iconView.setImageResource(R.drawable.ic_leaf)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
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
}