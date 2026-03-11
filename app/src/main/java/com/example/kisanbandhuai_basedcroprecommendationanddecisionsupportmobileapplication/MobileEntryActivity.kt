package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
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
                sendVerificationCode("+91$phoneNumber")
            } else {
                Toast.makeText(this, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun sendVerificationCode(number: String) {
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
            // Auto-retrieval or instant verification
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            btnSendOtp.isEnabled = true
            btnSendOtp.text = getString(R.string.send_otp)
            
            // Show actual error to help debugging (e.g., missing SHA keys or disabled provider)
            Toast.makeText(this@MobileEntryActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
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
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                    finish()
                } else {
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
                }
            }
        }

        findViewById<MaterialButton>(R.id.num_del).setOnClickListener {
            val text = etMobile.text.toString()
            if (text.isNotEmpty()) {
                etMobile.setText(text.substring(0, text.length - 1))
            }
        }
    }
}