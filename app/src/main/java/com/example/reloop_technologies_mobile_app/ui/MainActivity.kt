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
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        // Firebase Auth is the source of truth for the session. Preferences only cache its profile.
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        UserManager.logout(this)

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

        loginButton?.setSingleClickListener {
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

            if (!isValid) return@setSingleClickListener

            progressBar?.visibility = View.VISIBLE
            loginButton.isEnabled = false

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user == null) {
                            progressBar?.visibility = View.GONE
                            loginButton.isEnabled = true
                            Toast.makeText(this, "Login session unavailable. Please try again.", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }
                        val previous = UserManager.getUserProfile(this, user.uid)
                        UserManager.saveUserProfile(this, previous.copy(
                            name = previous.name.ifEmpty { user.displayName.orEmpty() },
                            email = user.email ?: email
                        ))
                        progressBar?.visibility = View.GONE
                        startActivity(Intent(this, HomeActivity::class.java))
                        finishAffinity()
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
