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
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Session Restoration: Check if user is already signed in
        if (firebaseAuth.currentUser != null || UserManager.isLoggedIn(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)
        val signUpText = findViewById<TextView>(R.id.signUpText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        signUpText?.text = Html.fromHtml(
            "Don't have an account? <font color='#237A3B'><b>Sign up</b></font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        signUpText?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPasswordText?.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginButton?.setOnClickListener {
            val email = emailInput?.text?.toString()?.trim() ?: ""
            val password = passwordInput?.text?.toString() ?: ""

            var isValid = true

            if (email.isEmpty()) {
                emailInput?.error = "Please enter your email address"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput?.error = "Please enter a valid email address"
                isValid = false
            }

            if (password.isEmpty()) {
                passwordInput?.error = "Please enter your password"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            progressBar?.visibility = View.VISIBLE
            loginButton.isEnabled = false

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = firebaseAuth.currentUser?.uid ?: ""

                        // Retrieve profile from Firestore to populate UserManager
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userData = UserData(
                                        uid = uid,
                                        name = document.getString("name") ?: "",
                                        email = document.getString("email") ?: email,
                                        phone = document.getString("phone") ?: "",
                                        address = document.getString("address") ?: ""
                                    )
                                    UserManager.saveUserProfile(this, userData)
                                } else {
                                    val userData = UserData(
                                        uid = uid,
                                        name = firebaseAuth.currentUser?.displayName ?: "Resident",
                                        email = email
                                    )
                                    UserManager.saveUserProfile(this, userData)
                                }
                                progressBar?.visibility = View.GONE
                                Toast.makeText(this, "Login successful! Welcome back.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finishAffinity()
                            }
                            .addOnFailureListener {
                                val userData = UserData(
                                    uid = uid,
                                    name = firebaseAuth.currentUser?.displayName ?: "Resident",
                                    email = email
                                )
                                UserManager.saveUserProfile(this, userData)
                                progressBar?.visibility = View.GONE
                                Toast.makeText(this, "Login successful! Welcome back.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finishAffinity()
                            }
                    } else {
                        progressBar?.visibility = View.GONE
                        loginButton.isEnabled = true

                        val exception = task.exception
                        if (exception is FirebaseAuthInvalidUserException) {
                            emailInput?.error = "No account found with this email. Please sign up."
                            Toast.makeText(this, "Account does not exist. Please create an account.", Toast.LENGTH_LONG).show()
                        } else if (exception is FirebaseAuthInvalidCredentialsException) {
                            passwordInput?.error = "Incorrect password. Please try again."
                            Toast.makeText(this, "Invalid credentials. Please check your password.", Toast.LENGTH_LONG).show()
                        } else {
                            val msg = exception?.localizedMessage ?: "Authentication failed. Please check network connection."
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }
}
