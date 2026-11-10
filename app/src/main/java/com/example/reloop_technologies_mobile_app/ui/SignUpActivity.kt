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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.BuildConfig
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.services.SupabaseAuthService
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val authService = SupabaseAuthService()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                authenticateWithGoogleToken(idToken)
            } else {
                setLoadingState(false)
                Toast.makeText(this, "Failed to retrieve Google credentials. Please try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            setLoadingState(false)
            val message = when (e.statusCode) {
                12501 -> "Sign-in attempt was canceled."
                7 -> "Network error occurred during sign-in."
                else -> e.localizedMessage ?: "Google Sign-In failed (Code: ${e.statusCode})."
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)


        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val googleSignUpButton = findViewById<View>(R.id.googleSignUpButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        loginText?.text = Html.fromHtml(
            "Already have an account? <font color='#237A3B'><b>Log in</b></font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        loginText?.setOnClickListener {
            finish()
        }

        googleSignUpButton?.setSingleClickListener {
            performGoogleSSO()
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

            setLoadingState(true)

            authService.signUp(email, password, name, phone, address) { state ->
                when (state) {
                    is ResultState.Success -> {
                        val user = state.data
                        if (user == null) {
                            setLoadingState(false)
                            Toast.makeText(this, "Check your email to confirm the account, then sign in.", Toast.LENGTH_LONG).show()
                            finish()
                            return@signUp
                        }
                        val uid = user.uid
                        UserManager.saveUserProfile(this, UserData(uid = uid, name = name, email = email, phone = phone, address = address))
                        val nameParts = name.split(Regex("\\s+"), limit = 2)
                        val profile = UserModel(userId = uid, firstName = nameParts.firstOrNull().orEmpty(), lastName = nameParts.getOrElse(1) { "" }, email = email, phoneNumber = phone, address = address)
                        RestApiClient.instance.updateUserProfile(profile) { }
                        setLoadingState(false)
                        Toast.makeText(this, "Account created successfully! Welcome $name.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finishAffinity()
                    }
                    is ResultState.Error -> {
                        setLoadingState(false)
                        if (state.message.contains("already", true)) emailInput?.error = "This email address may already be registered."
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                    ResultState.Loading -> setLoadingState(true)
                }
            }
        }
    }

    private fun performGoogleSSO() {
        setLoadingState(true)
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@SignUpActivity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    authenticateWithGoogleToken(googleIdTokenCredential.idToken)
                } else {
                    fallbackGoogleSignIn(webClientId)
                }
            } catch (e: GetCredentialCancellationException) {
                setLoadingState(false)
                Toast.makeText(this@SignUpActivity, "Sign-in was canceled.", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                fallbackGoogleSignIn(webClientId)
            } catch (e: Exception) {
                fallbackGoogleSignIn(webClientId)
            }
        }
    }

    private fun fallbackGoogleSignIn(webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun authenticateWithGoogleToken(idToken: String) {
        authService.signInWithGoogleIdToken(idToken) { result ->
            when (result) {
                is ResultState.Success -> {
                    val user = result.data
                    val displayName = user.displayName.orEmpty()
                    val email = user.email.orEmpty()

                    val nameInput = findViewById<EditText>(R.id.nameInput)
                    val phoneInput = findViewById<EditText>(R.id.phoneInput)
                    val addressInput = findViewById<EditText>(R.id.addressInput)

                    val name = nameInput?.text?.toString()?.trim()?.ifEmpty { displayName } ?: displayName
                    val phone = phoneInput?.text?.toString()?.trim().orEmpty()
                    val address = addressInput?.text?.toString()?.trim().orEmpty()

                    val userData = UserData(
                        uid = user.uid,
                        name = name,
                        email = email,
                        phone = phone,
                        address = address
                    )
                    UserManager.saveUserProfile(this, userData)

                    val nameParts = name.split(Regex("\\s+"), limit = 2)
                    val userProfile = UserModel(
                        userId = user.uid,
                        firstName = nameParts.firstOrNull().orEmpty(),
                        lastName = nameParts.getOrElse(1) { "" },
                        email = email,
                        phoneNumber = phone,
                        address = address
                    )
                    RestApiClient.instance.updateUserProfile(userProfile) { }

                    setLoadingState(false)
                    Toast.makeText(this, "Welcome, ${name.ifEmpty { "User" }}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity()
                }
                is ResultState.Error -> {
                    setLoadingState(false)
                    val msg = result.message ?: "Google SSO sign-in failed. Please try again."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
                is ResultState.Loading -> {
                    setLoadingState(true)
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        val progressBar = findViewById<ProgressBar>(R.id.signUpProgressBar)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val googleSignUpButton = findViewById<View>(R.id.googleSignUpButton)

        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUpButton?.isEnabled = !isLoading
        googleSignUpButton?.isEnabled = !isLoading
    }
}
