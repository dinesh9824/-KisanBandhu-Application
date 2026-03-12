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
import com.google.android.material.button.MaterialButton

class RecommendationResultActivity : BaseActivity() {

    private lateinit var viewModel: CropRecommendationViewModel
    private var cropNames: ArrayList<String> = arrayListOf()
    private var cropProbs: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation_result)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CropRecommendationViewModel::class.java]

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btn_check_market).setOnClickListener {
            val intent = Intent(this, MarketAnalysisActivity::class.java)
            if (cropNames.isNotEmpty()) {
                intent.putStringArrayListExtra("recommended_crops", cropNames)
                intent.putStringArrayListExtra("recommended_probs", cropProbs)
            }
            startActivity(intent)
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
            N = n.toFloat(), P = p.toFloat(), K = k.toFloat(),
            temperature = temp.toFloat(), humidity = humidity.toFloat(),
            ph = ph.toFloat(), rainfall = rainfall.toFloat()
        )
        viewModel.predictCrop(request)
    }

    private fun observeViewModel() {
        val loadingLayout = findViewById<View>(R.id.layout_loading)
        val resultCard = findViewById<View>(R.id.card_result)

        viewModel.predictionResult.observe(this) { response ->
            if (response != null && response.success) {
                val predictions = response.top_predictions
                cropNames.clear()
                cropProbs.clear()
                
                if (predictions.isNotEmpty()) {
                    val p1 = predictions[0]
                    cropNames.add(p1.cropName)
                    cropProbs.add("${(p1.probability * 100).toInt()}")
                    findViewById<TextView>(R.id.tv_crop_1)?.text = getLocalizedCropName(p1.cropName)
                    findViewById<TextView>(R.id.tv_prob_1)?.text = "${(p1.probability * 100).toInt()}%"
                    updateCropIcon(p1.cropName, findViewById(R.id.crop_icon_1))
                    
                    if (predictions.size > 1) {
                        val p2 = predictions[1]
                        cropNames.add(p2.cropName)
                        cropProbs.add("${(p2.probability * 100).toInt()}")
                        findViewById<TextView>(R.id.tv_crop_2)?.text = getLocalizedCropName(p2.cropName)
                        findViewById<TextView>(R.id.tv_prob_2)?.text = "${(p2.probability * 100).toInt()}%"
                    }
                    if (predictions.size > 2) {
                        val p3 = predictions[2]
                        cropNames.add(p3.cropName)
                        cropProbs.add("${(p3.probability * 100).toInt()}")
                        findViewById<TextView>(R.id.tv_crop_3)?.text = getLocalizedCropName(p3.cropName)
                        findViewById<TextView>(R.id.tv_prob_3)?.text = "${(p3.probability * 100).toInt()}%"
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
            resultCard.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun getLocalizedCropName(crop: String): String {
        val resName = "crop_" + crop.lowercase().replace(" ", "")
        val resId = resources.getIdentifier(resName, "string", packageName)
        return if (resId != 0) getString(resId) else crop.replaceFirstChar { it.uppercase() }
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
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true
                }
                R.id.nav_market -> {
                    startActivity(Intent(this, MarketAnalysisActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true
                }
                else -> false
            }
        }
    }
}