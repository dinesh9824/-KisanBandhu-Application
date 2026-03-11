package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : SwipeableActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvFarmSize: TextView
    private lateinit var tvCurrentCrops: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Views with NEW distinct IDs that match activity_profile.xml
        tvName = findViewById(R.id.tv_profile_name)
        tvLocation = findViewById(R.id.tv_profile_location)
        ivAvatar = findViewById(R.id.iv_profile_avatar)
        tvPhone = findViewById(R.id.tv_profile_phone)
        tvEmail = findViewById(R.id.tv_profile_email)
        tvFarmSize = findViewById(R.id.tv_profile_farm_size_val)
        tvCurrentCrops = findViewById(R.id.tv_profile_current_crops_val)

        setupBottomNavigation()
        loadUserProfile()
        
        // Setup Click Listeners
        findViewById<View>(R.id.btn_edit_farm)?.setOnClickListener {
            startActivity(Intent(this, FarmInfoActivity::class.java))
        }

        findViewById<View>(R.id.btn_edit_contact)?.setOnClickListener {
            startActivity(Intent(this, ContactInfoActivity::class.java))
        }

        findViewById<View>(R.id.btn_menu_edit_profile)?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_menu_crop_history)?.setOnClickListener {
            startActivity(Intent(this, CropHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btn_menu_settings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.btn_menu_help)?.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { document, error ->
            if (error != null) return@addSnapshotListener
            
            if (document != null && document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                
                // Update User Info
                tvName.text = profile?.name ?: "User"
                tvLocation.text = "📍 ${profile?.location ?: "Location"}"
                
                // Update Contact Info
                tvPhone.text = profile?.mobileNumber ?: "+91 XXXXX XXXXX"
                tvEmail.text = profile?.email ?: "email@example.com"
                
                // Update Farm Information (Real-time updates)
                val farmSizeText = if (profile != null && profile.farmSize > 0) {
                    "${profile.farmSize} ${profile.farmSizeUnit}"
                } else {
                    "Not Set"
                }
                tvFarmSize.text = farmSizeText
                tvCurrentCrops.text = if (!profile?.currentCrops.isNullOrEmpty()) profile?.currentCrops else "--"
                
                // Load Image
                if (!profile?.profileImageUrl.isNullOrEmpty()) {
                    ivAvatar.load(profile?.profileImageUrl) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.ic_sprout_logo)
                        error(R.drawable.ic_sprout_logo)
                    }
                } else {
                    // Clear image if URL is empty/null
                    ivAvatar.setImageResource(R.drawable.ic_sprout_logo)
                    ivAvatar.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        bottomNav.selectedItemId = R.id.nav_profile
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
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_profile
    }
}