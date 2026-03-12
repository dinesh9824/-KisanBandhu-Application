package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MobileEntryActivity : BaseActivity() {
    
    private lateinit var etMobile: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var btnSendOtp: MaterialButton
    
    // DEBUG BYPASS: Set this to your test phone number
    private val BYPASS_NUMBER = "9876543210" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_entry)

        auth = FirebaseAuth.getInstance()
        etMobile = findViewById(R.id.et_mobile)
        btnSendOtp = findViewById(R.id.btn_send_otp)
        
        setupNumpad()

        btnSendOtp.setOnClickListener {
            val phoneNumber = etMobile.text.toString()
            if (phoneNumber.length == 10) {
                if (phoneNumber == BYPASS_NUMBER) {
                    Log.d("OTP_DEBUG", "Bypass number detected. Navigating to OTP screen without Firebase request.")
                    val intent = Intent(this, OTPVerificationActivity::class.java)
                    intent.putExtra("verificationId", "MOCK_VERIFICATION_ID")
                    intent.putExtra("phone", phoneNumber)
                    startActivity(intent)
                } else {
                    sendVerificationCode("+91$phoneNumber")
                }
            } else {
                Toast.makeText(this, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun sendVerificationCode(number: String) {
        Log.d("OTP_DEBUG", "Initiating verification for: $number")
        btnSendOtp.isEnabled = false
        btnSendOtp.text = "Sending..."

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
            Log.d("OTP_DEBUG", "onVerificationCompleted: Instant verification")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            btnSendOtp.isEnabled = true
            btnSendOtp.text = "SEND OTP"
            updateButtonStyle(etMobile.text.length)
            Log.e("OTP_DEBUG", "Verification Failed: ${e.message}")
            
            if (e.message?.contains("BILLING_NOT_ENABLED") == true) {
                Toast.makeText(this@MobileEntryActivity, 
                    "SMS failed. Use test number $BYPASS_NUMBER to bypass billing check.", 
                    Toast.LENGTH_LONG).show()
            } else if (e.message?.contains("unusual activity") == true) {
                Toast.makeText(this@MobileEntryActivity, 
                    "Blocked due to unusual activity. Use test number $BYPASS_NUMBER to bypass.", 
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MobileEntryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d("OTP_DEBUG", "onCodeSent: Code successfully requested. ID: $verificationId")
            btnSendOtp.isEnabled = true
            btnSendOtp.text = "SEND OTP"
            updateButtonStyle(etMobile.text.length)
            
            val intent = Intent(this@MobileEntryActivity, OTPVerificationActivity::class.java)
            intent.putExtra("verificationId", verificationId)
            intent.putExtra("phone", etMobile.text.toString())
            startActivity(intent)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: com.google.firebase.auth.PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("OTP_DEBUG", "Sign in successful")
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                    finish()
                } else {
                    Log.e("OTP_DEBUG", "Sign in failed: ${task.exception?.message}")
                    Toast.makeText(this, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
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
                if (etMobile.text.length < 10) {
                    etMobile.append(value)
                    updateButtonStyle(etMobile.text.length)
                }
            }
        }

        findViewById<MaterialButton>(R.id.num_del).setOnClickListener {
            val text = etMobile.text.toString()
            if (text.isNotEmpty()) {
                etMobile.setText(text.substring(0, text.length - 1))
                updateButtonStyle(etMobile.text.length)
            }
        }
        
        // Initialize state
        updateButtonStyle(0)
    }

    private fun updateButtonStyle(length: Int) {
        if (length == 10) {
            btnSendOtp.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            btnSendOtp.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        } else {
            // Default inactive-looking color (using current backgroundTint from XML as reference)
            btnSendOtp.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#81C784"))
            btnSendOtp.setTextColor(ContextCompat.getColor(this, R.color.brand_green_dark))
        }
    }
}