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
import androidx.lifecycle.repeatOnLifecycle
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.BuildConfig
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.services.SupabaseAuthService
import com.example.reloop_technologies_mobile_app.backend.services.SupabaseClientProvider
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authService = SupabaseAuthService()
    private var hasNavigatedToHome = false

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

        if (authService.currentUser != null) {
            openHome()
            return
        }
        UserManager.logout(this)

        setContentView(R.layout.activity_main)

        if (SupabaseClientProvider.isConfigured) {
            lifecycleScope.launch {
                repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    SupabaseClientProvider.client.auth.sessionStatus.collect { status ->
                        if (status is SessionStatus.Authenticated && !isFinishing) {
                            openHome()
                        }
                    }
                }
            }
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleButton = findViewById<View>(R.id.googleSignInButton)
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

        googleButton?.setSingleClickListener {
            performGoogleSSO()
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

            setLoadingState(true)

            authService.signIn(email, password) { state ->
                when (state) {
                    is ResultState.Success -> {
                        val user = state.data
                        val previous = UserManager.getUserProfile(this, user.uid)
                        UserManager.saveUserProfile(this, previous.copy(
                            name = previous.name.ifEmpty { user.displayName.orEmpty() },
                            email = user.email ?: email
                        ))
                        setLoadingState(false)
                        openHome()
                    }
                    is ResultState.Error -> {
                        setLoadingState(false)
                        val message = state.message
                        if (message.contains("invalid login", true) || message.contains("invalid credentials", true)) {
                            passwordInput?.error = "Check your email and password."
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    authenticateWithGoogleToken(googleIdTokenCredential.idToken)
                } else {
                    fallbackGoogleSignIn(webClientId)
                }
            } catch (e: GetCredentialCancellationException) {
                setLoadingState(false)
                Toast.makeText(this@MainActivity, "Sign-in was canceled.", Toast.LENGTH_SHORT).show()
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
                    val previous = UserManager.getUserProfile(this, user.uid)
                    val userData = previous.copy(
                        name = previous.name.ifEmpty { displayName },
                        email = previous.email.ifEmpty { email }
                    )
                    UserManager.saveUserProfile(this, userData)

                    val nameParts = displayName.split(Regex("\\s+"), limit = 2)
                    val userProfile = UserModel(
                        userId = user.uid,
                        firstName = nameParts.firstOrNull().orEmpty(),
                        lastName = nameParts.getOrElse(1) { "" },
                        email = email
                    )
                    RestApiClient.instance.updateUserProfile(userProfile) { }

                    setLoadingState(false)
                    Toast.makeText(this, "Welcome, ${displayName.ifEmpty { "User" }}!", Toast.LENGTH_SHORT).show()
                    openHome()
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
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleButton = findViewById<View>(R.id.googleSignInButton)

        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton?.isEnabled = !isLoading
        googleButton?.isEnabled = !isLoading
    }

    private fun openHome() {
        if (hasNavigatedToHome || isFinishing) return
        hasNavigatedToHome = true
        startActivity(Intent(this, HomeActivity::class.java))
        finishAffinity()
    }
}
