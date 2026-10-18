package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.repositories.UserRepository
import com.example.reloop_technologies_mobile_app.backend.services.FirebaseAuthService
import com.example.reloop_technologies_mobile_app.database.UserManager

class ProfileActivity : AppCompatActivity() {
    private val userRepository = UserRepository()
    private val authService = FirebaseAuthService()

    private var currentUserModel: UserModel = UserModel()

    private lateinit var avatarText: TextView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView

    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var profileImageUrlInput: EditText
    private lateinit var saveProfileButton: Button
    private lateinit var saveProgressBar: ProgressBar

    private lateinit var changePasswordOption: View
    private lateinit var resetPasswordOption: View
    private lateinit var guidanceOption: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bindViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun bindViews() {
        avatarText = findViewById(R.id.avatarText)
        nameText = findViewById(R.id.nameText)
        emailText = findViewById(R.id.emailText)

        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        phoneInput = findViewById(R.id.phoneInput)
        addressInput = findViewById(R.id.addressInput)
        profileImageUrlInput = findViewById(R.id.profileImageUrlInput)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        saveProgressBar = findViewById(R.id.saveProgressBar)

        changePasswordOption = findViewById(R.id.changePasswordOption)
        resetPasswordOption = findViewById(R.id.resetPasswordOption)
        guidanceOption = findViewById(R.id.guidanceOption)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        saveProfileButton.setOnClickListener {
            validateAndSaveProfile()
        }

        changePasswordOption.setOnClickListener {
            showChangePasswordDialog()
        }

        resetPasswordOption.setOnClickListener {
            sendPasswordReset()
        }

        guidanceOption.setOnClickListener {
            startActivity(Intent(this, RecyclingGuidanceActivity::class.java))
        }

        findViewById<Button>(R.id.logoutButton)?.setOnClickListener {
            authService.signOut()
            UserManager.logout(this)
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // Bottom Nav
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.navScan)?.setOnClickListener {
            startActivity(Intent(this, ScanWasteActivity::class.java))
        }
        findViewById<View>(R.id.navPickups)?.setOnClickListener {
            startActivity(Intent(this, MyPickupsActivity::class.java))
        }
        findViewById<View>(R.id.navRewards)?.setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }
    }

    private fun getUserId(): String {
        val currentUser = authService.currentUser
        val uid = currentUser?.uid ?: UserManager.getUserProfile(this).uid
        return uid.ifEmpty { "default_user" }
    }

    private fun loadUserProfile() {
        val userId = getUserId()
        val localProfile = UserManager.getUserProfile(this)

        // Local profile preview first
        if (localProfile.name.isNotEmpty()) {
            nameText.text = localProfile.name
            avatarText.text = localProfile.name.first().uppercase()
            emailText.text = localProfile.email
        }

        userRepository.fetchUserProfile(userId, this) { result ->
            when (result) {
                is ResultState.Success -> {
                    currentUserModel = result.data
                    populateUi(currentUserModel)
                }
                else -> {}
            }
        }
    }

    private fun populateUi(user: UserModel) {
        val firebaseUser = authService.currentUser
        val email = user.email.ifEmpty { firebaseUser?.email ?: "alex.morgan@example.com" }

        nameText.text = user.fullName
        emailText.text = email

        if (user.fullName.isNotEmpty()) {
            avatarText.text = user.fullName.first().uppercase()
        }

        if (firstNameInput.text.isEmpty() || (firstNameInput.text.toString() != user.firstName)) {
            firstNameInput.setText(user.firstName)
        }
        if (lastNameInput.text.isEmpty() || (lastNameInput.text.toString() != user.lastName)) {
            lastNameInput.setText(user.lastName)
        }
        if (phoneInput.text.isEmpty() || (phoneInput.text.toString() != user.phoneNumber)) {
            phoneInput.setText(user.phoneNumber)
        }
        if (addressInput.text.isEmpty() || (addressInput.text.toString() != user.address)) {
            addressInput.setText(user.address)
        }
        if (profileImageUrlInput.text.isEmpty()) {
            profileImageUrlInput.setText(user.profileImageUrl ?: "")
        }
    }

    private fun validateAndSaveProfile() {
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val address = addressInput.text.toString().trim()
        val imageUrl = profileImageUrlInput.text.toString().trim()

        if (firstName.isEmpty()) {
            firstNameInput.error = "First name is required"
            firstNameInput.requestFocus()
            return
        }

        if (phone.isEmpty() || phone.length < 7) {
            phoneInput.error = "Please enter a valid phone number"
            phoneInput.requestFocus()
            return
        }

        if (address.isEmpty()) {
            addressInput.error = "Home address is required"
            addressInput.requestFocus()
            return
        }

        val userId = getUserId()
        val updatedUser = currentUserModel.copy(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phone,
            address = address,
            profileImageUrl = imageUrl.ifEmpty { null },
            updatedAt = System.currentTimeMillis(),
        )

        saveProgressBar.visibility = View.VISIBLE
        saveProfileButton.isEnabled = false

        userRepository.saveUserProfile(updatedUser, this) { result ->
            saveProgressBar.visibility = View.GONE
            saveProfileButton.isEnabled = true

            when (result) {
                is ResultState.Success -> {
                    currentUserModel = updatedUser
                    populateUi(currentUserModel)
                    Toast.makeText(this, "Profile changes saved successfully!", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
                is ResultState.Loading -> {}
            }
        }
    }

    private fun showChangePasswordDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val passInput = EditText(context).apply {
            hint = "New Password (min 6 chars)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val confirmInput = EditText(context).apply {
            hint = "Confirm New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(passInput)
        layout.addView(confirmInput)

        AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update Password") { dialog, _ ->
                val pass = passInput.text.toString().trim()
                val confirm = confirmInput.text.toString().trim()

                if (pass.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (pass != confirm) {
                    Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                authService.updatePassword(pass) { result ->
                    when (result) {
                        is ResultState.Success -> {
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        is ResultState.Error -> {
                            AlertDialog.Builder(context)
                                .setTitle("Password Change Note")
                                .setMessage("${result.message}\n\nWould you like us to send a Password Reset email instead?")
                                .setPositiveButton("Send Reset Email") { _, _ ->
                                    sendPasswordReset()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        else -> {}
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordReset() {
        val firebaseUser = authService.currentUser
        val email = currentUserModel.email.ifEmpty { firebaseUser?.email ?: "" }

        if (email.isEmpty()) {
            Toast.makeText(this, "No email found for this account.", Toast.LENGTH_SHORT).show()
            return
        }

        authService.sendPasswordResetEmail(email) { result ->
            when (result) {
                is ResultState.Success -> {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                }
                is ResultState.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
