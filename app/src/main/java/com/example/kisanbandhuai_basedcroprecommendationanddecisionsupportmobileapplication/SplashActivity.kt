package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : BaseActivity() {

    private var isNavigationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<LinearProgressIndicator>(R.id.progress_bar)
        val tvLoading = findViewById<TextView>(R.id.tv_loading)

        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 2000
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
            tvLoading.text = "${getString(R.string.loading)} $progress%"
            
            if (progress == 100 && !isNavigationStarted) {
                isNavigationStarted = true
                checkUserStatusAndNavigate()
            }
        }
        animator.start()
    }

    private fun checkUserStatusAndNavigate() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser != null) {
            // NETWORK SAFETY: Set a 5-second timeout. 
            // If Firestore doesn't respond, move to MainActivity anyway.
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!isFinishing) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            handler.postDelayed(timeoutRunnable, 5000)

            // Check if profile exists in Firestore
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    handler.removeCallbacks(timeoutRunnable) // Cancel timeout
                    if (!isFinishing) {
                        if (document.exists()) {
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            startActivity(Intent(this, ProfileSetupActivity::class.java))
                        }
                        finish()
                    }
                }
                .addOnFailureListener {
                    handler.removeCallbacks(timeoutRunnable) // Cancel timeout
                    if (!isFinishing) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
        } else {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }
}