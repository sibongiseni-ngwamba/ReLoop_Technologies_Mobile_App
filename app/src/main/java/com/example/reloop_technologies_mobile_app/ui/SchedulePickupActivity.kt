package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.services.NotificationHelper
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth

class SchedulePickupActivity : AppCompatActivity() {

    private val pickupRepository = PickupRepository()
    private val notificationRepository = NotificationRepository()
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth

class SchedulePickupActivity : AppCompatActivity() {
    private val pickupRepository = PickupRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_pickup)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        val addressInput = findViewById<EditText>(R.id.addressInput)
        val wasteTypeInput = findViewById<EditText>(R.id.wasteTypeInput)
        val notesInput = findViewById<EditText>(R.id.notesInput)
        val submitButton = findViewById<Button>(R.id.submitButton)
        val progressBar = findViewById<ProgressBar>(R.id.scheduleProgressBar)

        val userProfile = UserManager.getUserProfile(this)
        if (userProfile.address.isNotEmpty()) {
            addressInput?.setText(userProfile.address)
        }

        submitButton?.setSingleClickListener {
            val address = addressInput?.text?.toString()?.trim()
            val wasteType = wasteTypeInput?.text?.toString()?.trim() ?: ""
            val notes = notesInput?.text?.toString()?.trim() ?: ""

            if (address.isNullOrEmpty()) {
                addressInput?.error = "Please enter a pickup address"
                Toast.makeText(this, "Please enter a pickup address", Toast.LENGTH_SHORT).show()
            } else {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: userProfile.uid

                val newPickup = PickupModel(
                    userId = currentUid,
                    address = address,
                    status = "Scheduled",
                    scheduledDate = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

                pickupRepository.schedulePickup(newPickup) { result ->
                    // On pickup saved, create and notify confirmation
                    val pickupId = if (result is com.example.reloop_technologies_mobile_app.backend.ResultState.Success) result.data else ""
                    val notification = NotificationModel(
                        userId = currentUid,
                        title = "Pickup Confirmed!",
                        message = "Your recycling pickup request for address '$address' has been confirmed.",
                        type = NotificationModel.TYPE_PICKUP_CONFIRMATION,
                        relatedPickupId = pickupId,
                        sentAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )

                    notificationRepository.addNotification(notification)

                    // Post push notification to system notification bar
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Pickup Confirmed!",
                        "Your recycling pickup request for address '$address' has been confirmed."
                    )
                }

                Toast.makeText(this, "Pickup Request Submitted Successfully!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MyPickupsActivity::class.java))
                return@setSingleClickListener
            }

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                ?: userProfile.uid

            if (currentUid.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@setSingleClickListener
            }

            // Lock UI against duplicate bookings
            submitButton.isEnabled = false
            progressBar?.visibility = View.VISIBLE

            val newPickup = PickupModel(
                userId = currentUid,
                address = address,
                wasteType = wasteType,
                notes = notes,
                status = "Scheduled",
                scheduledDate = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            pickupRepository.schedulePickup(newPickup) { result ->
                when (result) {
                    is ResultState.Loading -> {
                        progressBar?.visibility = View.VISIBLE
                        submitButton.isEnabled = false
                    }
                    is ResultState.Success -> {
                        progressBar?.visibility = View.GONE
                        val pickupId = result.data

                        // Add confirmation notification
                        val notification = NotificationModel(
                            userId = currentUid,
                            title = "Pickup Confirmed!",
                            message = "Your recycling pickup request for '$address' has been confirmed.",
                            type = NotificationModel.TYPE_PICKUP_CONFIRMATION,
                            relatedPickupId = pickupId,
                            sentAt = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis(),
                            isRead = false
                        )
                        notificationRepository.addNotification(notification)

                        // Post local push notification
                        NotificationHelper.showNotification(
                            applicationContext,
                            "Pickup Confirmed!",
                            "Your recycling pickup request for '$address' has been confirmed."
                        )

                        Toast.makeText(this, "Pickup Request Submitted Successfully!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, MyPickupsActivity::class.java))
                        finish()
                    }
                    is ResultState.Error -> {
                        progressBar?.visibility = View.GONE
                        submitButton.isEnabled = true
                        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    }
            } else {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: userProfile.uid.ifEmpty { "default_user" }

                val newPickup = PickupModel(
                    userId = userId,
                    address = address,
                    wasteType = "Recyclables (Plastic, Paper, Glass)",
                    wasteTypes = listOf("Plastic", "Paper", "Glass"),
                    status = "Scheduled",
                    scheduledDate = System.currentTimeMillis() + (2 * 86400000L)
                )

                pickupRepository.schedulePickup(newPickup) {
                    Toast.makeText(this, "Pickup Request Submitted Successfully!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, MyPickupsActivity::class.java))
                    finish()
                }
            }
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
        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
