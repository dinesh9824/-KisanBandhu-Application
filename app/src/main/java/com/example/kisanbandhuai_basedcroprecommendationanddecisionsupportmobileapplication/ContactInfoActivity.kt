package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactInfoActivity : AppCompatActivity() {

    private lateinit var tvPrimaryMobile: TextView
    private lateinit var etAltMobile: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: MaterialButton
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_info)

        // Initialize Views
        tvPrimaryMobile = findViewById(R.id.tv_primary_mobile)
        etAltMobile = findViewById(R.id.et_alt_mobile)
        etEmail = findViewById(R.id.et_email)
        etAddress = findViewById(R.id.et_address)
        btnSave = findViewById(R.id.btn_save_contact)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveContactInfo()
        }

        // Delay loading slightly to let the window gain focus and avoid ANR
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                loadContactInfo()
            }
        }, 300)
    }

    private fun loadContactInfo() {
        val user = auth.currentUser ?: return
        
        // Set Auth phone number immediately
        tvPrimaryMobile.text = user.phoneNumber ?: "Not Available"

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (isFinishing) return@addOnSuccessListener
                
                if (document.exists()) {
                    try {
                        val profile = document.toObject(UserProfile::class.java)
                        etAltMobile.setText(profile?.alternateMobile ?: "")
                        etEmail.setText(profile?.email ?: "")
                        etAddress.setText(profile?.address ?: "")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener {
                if (!isFinishing) {
                    Toast.makeText(this, "Failed to load info", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveContactInfo() {
        val uid = auth.currentUser?.uid ?: return
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val updates = hashMapOf<String, Any>(
            "alternateMobile" to etAltMobile.text.toString(),
            "email" to etEmail.text.toString(),
            "address" to etAddress.text.toString()
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                if (!isFinishing) {
                    Toast.makeText(this, "Contact Info Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                if (!isFinishing) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE CONTACT INFO / सहेजें"
                }
            }
    }
}