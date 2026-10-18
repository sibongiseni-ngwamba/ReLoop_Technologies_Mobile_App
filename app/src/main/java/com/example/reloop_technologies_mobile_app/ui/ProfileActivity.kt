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
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.UserRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.WasteScanRepository
import com.example.reloop_technologies_mobile_app.backend.services.FirebaseAuthService
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private val userRepository = UserRepository()
    private val pickupRepository = PickupRepository()
    private val wasteScanRepository = WasteScanRepository()
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
        if (authService.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_profile)

        bindViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || authService.currentUser == null) return
        loadUserProfile()
        loadImpact()
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

        findViewById<Button>(R.id.logoutButton)?.setSingleClickListener {
            authService.signOut(this)
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
        return authService.currentUser?.uid.orEmpty()
    }

    private fun loadUserProfile() {
        val userId = getUserId()
        if (userId.isEmpty()) return
        val localProfile = UserManager.getUserProfile(this, userId)

        val localName = localProfile.name.ifEmpty { authService.currentUser?.displayName.orEmpty() }
        val nameParts = localName.trim().split(Regex("\\s+"), limit = 2)
        currentUserModel = UserModel(
            userId = userId,
            firstName = nameParts.firstOrNull().orEmpty(),
            lastName = nameParts.getOrElse(1) { "" },
            email = authService.currentUser?.email ?: localProfile.email,
            phoneNumber = localProfile.phone,
            address = localProfile.address
        )
        populateUi(currentUserModel)

        userRepository.fetchUserProfile(userId, this) { result ->
            if (getUserId() != userId || isFinishing) return@fetchUserProfile
            when (result) {
                is ResultState.Success -> {
                    currentUserModel = result.data
                    populateUi(currentUserModel)
                }
                else -> {}
            }
        }
    }

    private fun loadImpact() {
        val userId = getUserId()
        if (userId.isEmpty()) return
        findViewById<TextView>(R.id.plasticSavedText).text = "0 kg"
        findViewById<TextView>(R.id.itemsScannedText).text = "0"
        findViewById<TextView>(R.id.pickupsDoneText).text = "0"
        wasteScanRepository.fetchWasteScansForUser(userId) { result ->
            if (getUserId() != userId || isFinishing) return@fetchWasteScansForUser
            if (result is ResultState.Success) {
                val scans = result.data
                val plasticKg = scans.filter {
                    it.wasteCategoryId.contains("plastic", true) || it.modelLabel.orEmpty().contains("plastic", true)
                }.sumOf { it.weightKg }
                findViewById<TextView>(R.id.plasticSavedText).text = String.format(Locale.getDefault(), "%.1f kg", plasticKg)
                findViewById<TextView>(R.id.itemsScannedText).text = scans.size.toString()
            }
        }
        pickupRepository.fetchPickupsForUser(userId) { result ->
            if (getUserId() != userId || isFinishing) return@fetchPickupsForUser
            if (result is ResultState.Success) {
                findViewById<TextView>(R.id.pickupsDoneText).text =
                    result.data.count { it.status.equals("Completed", true) }.toString()
            }
        }
    }

    private fun populateUi(user: UserModel) {
        val firebaseUser = authService.currentUser
        val email = firebaseUser?.email ?: user.email

        nameText.text = user.fullName
        emailText.text = email

        if (user.fullName.isNotEmpty()) {
            avatarText.text = user.fullName.first().uppercase()
        }

        if (!firstNameInput.hasFocus()) firstNameInput.setText(user.firstName)
        if (!lastNameInput.hasFocus()) lastNameInput.setText(user.lastName)
        if (!phoneInput.hasFocus()) phoneInput.setText(user.phoneNumber)
        if (!addressInput.hasFocus()) addressInput.setText(user.address)
        if (!profileImageUrlInput.hasFocus()) profileImageUrlInput.setText(user.profileImageUrl ?: "")
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
        if (userId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        val updatedUser = currentUserModel.copy(
            userId = userId,
            email = authService.currentUser?.email ?: currentUserModel.email,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phone,
            address = address,
            profileImageUrl = imageUrl.ifEmpty { null },
            updatedAt = System.currentTimeMillis()
        )

        saveProgressBar.visibility = View.VISIBLE
        saveProfileButton.isEnabled = false

        userRepository.saveUserProfile(updatedUser, this) { result ->
            when (result) {
                is ResultState.Success -> {
                    saveProgressBar.visibility = View.GONE
                    saveProfileButton.isEnabled = true
                    currentUserModel = updatedUser
                    populateUi(currentUserModel)
                    Toast.makeText(this, "Profile changes saved successfully!", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Error -> {
                    saveProgressBar.visibility = View.GONE
                    saveProfileButton.isEnabled = true
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
        val email = firebaseUser?.email ?: currentUserModel.email

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
