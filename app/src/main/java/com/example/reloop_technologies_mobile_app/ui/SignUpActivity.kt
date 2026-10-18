package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val progressBar = findViewById<ProgressBar>(R.id.signUpProgressBar)
        val loginText = findViewById<TextView>(R.id.loginText)

        loginText?.text = Html.fromHtml(
            "Already have an account? <font color='#237A3B'><b>Log in</b></font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        loginText?.setOnClickListener {
            finish()
        }

        signUpButton?.setSingleClickListener {
            val name = nameInput?.text?.toString()?.trim() ?: ""
            val email = emailInput?.text?.toString()?.trim() ?: ""
            val phone = phoneInput?.text?.toString()?.trim() ?: ""
            val address = addressInput?.text?.toString()?.trim() ?: ""
            val password = passwordInput?.text?.toString() ?: ""

            // Validation
            var isValid = true

            if (name.isEmpty()) {
                nameInput?.error = "Full name is required"
                isValid = false
            }

            if (email.isEmpty()) {
                emailInput?.error = "Email address is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput?.error = "Please enter a valid email address (e.g. name@domain.com)"
                isValid = false
            }

            if (phone.isEmpty()) {
                phoneInput?.error = "Phone number is required"
                isValid = false
            } else if (phone.length < 7) {
                phoneInput?.error = "Please enter a valid phone number (min 7 digits)"
                isValid = false
            }

            if (address.isEmpty()) {
                addressInput?.error = "Residential address is required"
                isValid = false
            }

            if (password.isEmpty()) {
                passwordInput?.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                passwordInput?.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (!isValid) {
                Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show()
                return@setSingleClickListener
            }

            // Start Registration UI State
            progressBar?.visibility = View.VISIBLE
            signUpButton.isEnabled = false

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        val uid = firebaseUser?.uid
                        if (uid.isNullOrEmpty()) {
                            progressBar?.visibility = View.GONE
                            signUpButton.isEnabled = true
                            Toast.makeText(this, "Account created, but the session is unavailable. Please log in again.", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        // Update Display Name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        firebaseUser?.updateProfile(profileUpdates)

                        // Save User Data Model
                        val userData = UserData(
                            uid = uid,
                            name = name,
                            email = email,
                            phone = phone,
                            address = address
                        )

                        // Save to SharedPreferences locally
                        UserManager.saveUserProfile(this, userData)

                        // Save to Firestore
                        val nameParts = name.split(Regex("\\s+"), limit = 2)
                        val userProfile = UserModel(
                            userId = uid,
                            firstName = nameParts.first(),
                            lastName = nameParts.getOrElse(1) { "" },
                            email = email,
                            phoneNumber = phone,
                            address = address
                        )

                        firestore.collection("users").document(uid).set(userProfile)

                        progressBar?.visibility = View.GONE
                        Toast.makeText(this, "Account created successfully! Welcome $name.", Toast.LENGTH_LONG).show()

                        startActivity(Intent(this, HomeActivity::class.java))
                        finishAffinity()
                    } else {
                        progressBar?.visibility = View.GONE
                        signUpButton.isEnabled = true

                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            emailInput?.error = "This email address is already registered."
                            Toast.makeText(this, "An account with this email already exists. Please log in.", Toast.LENGTH_LONG).show()
                        } else if (exception is FirebaseAuthWeakPasswordException) {
                            passwordInput?.error = "Password is too weak. Choose a stronger password."
                            Toast.makeText(this, "Weak password: " + exception.reason, Toast.LENGTH_LONG).show()
                        } else {
                            val msg = exception?.localizedMessage ?: "Registration failed. Please try again."
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }
}
