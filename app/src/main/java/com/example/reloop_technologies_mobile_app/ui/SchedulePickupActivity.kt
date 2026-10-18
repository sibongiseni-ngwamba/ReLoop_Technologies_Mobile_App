package com.example.reloop_technologies_mobile_app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SchedulePickupActivity : AppCompatActivity() {

    private val pickupRepository = PickupRepository()
    private val notificationRepository = NotificationRepository()

    private lateinit var addressInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var wasteTypeInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private val calendar: Calendar = Calendar.getInstance()
    private var isDateSelected = false
    private var isTimeSelected = false
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_pickup)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        addressInput = findViewById(R.id.addressInput)
        dateInput = findViewById(R.id.dateInput)
        timeInput = findViewById(R.id.timeInput)
        wasteTypeInput = findViewById(R.id.wasteTypeInput)
        notesInput = findViewById(R.id.notesInput)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.scheduleProgressBar)

        val userProfile = UserManager.getUserProfile(this)
        if (userProfile.address.isNotEmpty()) {
            addressInput.setText(userProfile.address)
        }

        setupDateAndPickers()

        submitButton.setSingleClickListener {
            if (isSubmitting) return@setSingleClickListener
            validateAndSubmitPickup()
        }

        setupBottomNav()
    }

    private fun setupDateAndPickers() {
        dateInput.isFocusable = false
        dateInput.isClickable = true
        timeInput.isFocusable = false
        timeInput.isClickable = true

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dateInput.setText(sdf.format(calendar.time))
            dateInput.error = null
            isDateSelected = true
        }

        dateInput.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)

            val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            timeInput.setText(timeSdf.format(calendar.time))
            timeInput.error = null
            isTimeSelected = true
        }

        timeInput.setOnClickListener {
            TimePickerDialog(
                this,
                timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun validateAndSubmitPickup() {
        val address = addressInput.text?.toString()?.trim() ?: ""
        val dateText = dateInput.text?.toString()?.trim() ?: ""
        val timeText = timeInput.text?.toString()?.trim() ?: ""
        val wasteType = wasteTypeInput.text?.toString()?.trim() ?: ""
        val notes = notesInput.text?.toString()?.trim() ?: ""

        var isValid = true

        if (address.isEmpty()) {
            addressInput.error = "Please enter a pickup address"
            isValid = false
        }
        if (dateText.isEmpty() || !isDateSelected) {
            dateInput.error = "Please select a preferred date"
            isValid = false
        }
        if (timeText.isEmpty() || !isTimeSelected) {
            timeInput.error = "Please select a preferred time"
            isValid = false
        }
        if (wasteType.isEmpty()) {
            wasteTypeInput.error = "Please enter waste type(s)"
            isValid = false
        }

        if (!isValid) return

        if (calendar.timeInMillis < System.currentTimeMillis() - 60000) {
            Toast.makeText(this, "Selected pickup slot is in the past. Please choose a future slot.", Toast.LENGTH_LONG).show()
            dateInput.error = "Slot unavailable"
            return
        }

        val userProfile = UserManager.getUserProfile(this)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: userProfile.uid

        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        isSubmitting = true
        submitButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val newPickup = PickupModel(
            userId = currentUid,
            address = address,
            wasteType = wasteType,
            wasteTypes = wasteType.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            notes = notes,
            status = "Scheduled",
            scheduledDate = calendar.timeInMillis,
            createdAt = System.currentTimeMillis()
        )

        pickupRepository.schedulePickup(newPickup) { result ->
            when (result) {
                is ResultState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    submitButton.isEnabled = false
                }
                is ResultState.Success -> {
                    progressBar.visibility = View.GONE
                    isSubmitting = false
                    val pickupId = result.data

                    val notification = NotificationModel(
                        userId = currentUid,
                        title = "Pickup Confirmed!",
                        message = "Your recycling pickup request (#${pickupId.take(8)}) for address '$address' on $dateText at $timeText has been confirmed.",
                        type = NotificationModel.TYPE_PICKUP_CONFIRMATION,
                        relatedPickupId = pickupId,
                        sentAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    notificationRepository.addNotification(notification)

                    NotificationHelper.showNotification(
                        applicationContext,
                        "Pickup Confirmed!",
                        "Your pickup request (#${pickupId.take(8)}) has been scheduled successfully."
                    )

                    Toast.makeText(this, "Pickup Request Submitted! Reference: #${pickupId.take(8)}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, MyPickupsActivity::class.java))
                    finish()
                }
                is ResultState.Error -> {
                    progressBar.visibility = View.GONE
                    isSubmitting = false
                    submitButton.isEnabled = true
                    Toast.makeText(this, "Failed to schedule pickup: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupBottomNav() {
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
