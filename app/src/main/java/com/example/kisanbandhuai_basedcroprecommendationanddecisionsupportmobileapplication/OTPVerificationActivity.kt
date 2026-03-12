package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class OTPVerificationActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var phoneNumber: String? = null
    
    private val otpTextViews = arrayOfNulls<TextView>(6)
    private var currentOtp = ""
    private lateinit var btnVerify: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()
        verificationId = intent.getStringExtra("verificationId")
        phoneNumber = intent.getStringExtra("phone")
        btnVerify = findViewById(R.id.btn_verify)

        findViewById<TextView>(R.id.tv_display_mobile).text = "+91 $phoneNumber"

        // Initialize OTP boxes
        otpTextViews[0] = findViewById(R.id.otp_1)
        otpTextViews[1] = findViewById(R.id.otp_2)
        otpTextViews[2] = findViewById(R.id.otp_3)
        otpTextViews[3] = findViewById(R.id.otp_4)
        otpTextViews[4] = findViewById(R.id.otp_5)
        otpTextViews[5] = findViewById(R.id.otp_6)

        setupNumpad()
        startResendTimer()

        btnVerify.setOnClickListener {
            if (currentOtp.length == 6) {
                if (verificationId == "MOCK_VERIFICATION_ID") {
                    Log.d("OTP_DEBUG", "Bypassing Firebase check for mock ID. Navigating to Profile Setup.")
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                    finish()
                } else {
                    verifyCode(currentOtp)
                }
            } else {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun setupNumpad() {
        val buttons = listOf(
            R.id.num_0 to "0", R.id.num_1 to "1", R.id.num_2 to "2",
            R.id.num_3 to "3", R.id.num_4 to "4", R.id.num_5 to "5",
            R.id.num_6 to "6", R.id.num_7 to "7", R.id.num_8 to "8",
            R.id.num_9 to "9"
        )

        buttons.forEach { (id, value) ->
            findViewById<MaterialButton>(id).setOnClickListener {
                if (currentOtp.length < 6) {
                    currentOtp += value
                    updateOtpDisplay()
                    updateButtonStyle(currentOtp.length)
                }
            }
        }

        findViewById<MaterialButton>(R.id.num_del).setOnClickListener {
            if (currentOtp.isNotEmpty()) {
                currentOtp = currentOtp.substring(0, currentOtp.length - 1)
                updateOtpDisplay()
                updateButtonStyle(currentOtp.length)
            }
        }
        
        // Initial state
        updateButtonStyle(0)
    }

    private fun updateOtpDisplay() {
        for (i in 0 until 6) {
            if (i < currentOtp.length) {
                otpTextViews[i]?.text = currentOtp[i].toString()
                otpTextViews[i]?.setBackgroundResource(R.drawable.option_item_selector)
            } else {
                otpTextViews[i]?.text = ""
                otpTextViews[i]?.setBackgroundResource(R.drawable.input_field_bg)
            }
        }
    }

    private fun updateButtonStyle(length: Int) {
        if (length == 6) {
            btnVerify.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            btnVerify.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        } else {
            // Semi-transparent or lighter green to show "inactive" state
            btnVerify.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#81C784"))
            btnVerify.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        }
    }

    private fun verifyCode(code: String) {
        if (verificationId == null) {
            Toast.makeText(this, "Verification error. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: com.google.firebase.auth.PhoneAuthCredential) {
        btnVerify.text = "Verifying..."
        btnVerify.isEnabled = false
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                    finish()
                } else {
                    btnVerify.text = "VERIFY"
                    btnVerify.isEnabled = true
                    updateButtonStyle(currentOtp.length)
                    Toast.makeText(this, "Invalid OTP: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startResendTimer() {
        val tvResend = findViewById<TextView>(R.id.tv_resend)
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResend.text = "Resend code in ${millisUntilFinished / 1000}s"
                tvResend.isEnabled = false
            }

            override fun onFinish() {
                tvResend.text = "Resend OTP"
                tvResend.isEnabled = true
                tvResend.setOnClickListener {
                    Toast.makeText(this@OTPVerificationActivity, "Resending OTP...", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}