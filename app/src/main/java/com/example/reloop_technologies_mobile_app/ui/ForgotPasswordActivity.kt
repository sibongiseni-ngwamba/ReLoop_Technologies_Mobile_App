package com.example.reloop_technologies_mobile_app.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        firebaseAuth = FirebaseAuth.getInstance()

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val progressBar = findViewById<ProgressBar>(R.id.resetProgressBar)
        val sendResetButton = findViewById<Button>(R.id.sendResetButton)

        sendResetButton?.setSingleClickListener {
            val email = emailInput?.text?.toString()?.trim() ?: ""

            if (email.isEmpty()) {
                emailInput?.error = "Please enter your email address"
                return@setSingleClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput?.error = "Please enter a valid email address"
                return@setSingleClickListener
            }

            progressBar?.visibility = View.VISIBLE
            sendResetButton.isEnabled = false

            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBar?.visibility = View.GONE
                    sendResetButton.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset link sent to $email. Please check your inbox.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Failed to send reset email. Please try again."
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
