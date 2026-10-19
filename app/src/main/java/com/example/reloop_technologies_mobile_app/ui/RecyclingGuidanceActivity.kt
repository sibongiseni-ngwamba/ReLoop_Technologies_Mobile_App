package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.repositories.WasteScanRepository

class RecyclingGuidanceActivity : AppCompatActivity() {

    private val wasteScanRepository = WasteScanRepository()

    private lateinit var guidanceImageView: ImageView
    private lateinit var guidanceTitleText: TextView
    private lateinit var suitabilityBadgeText: TextView
    private lateinit var instructionsCard: View
    private lateinit var instructionsBodyText: TextView
    private lateinit var nonRecyclableWarningCard: View
    private lateinit var nonRecyclableNoticeText: TextView
    private lateinit var fallbackCard: View
    private lateinit var btnBackToScan: Button
    private lateinit var btnGoToDashboard: Button

    private var categoryId: String? = null
    private var categoryName: String? = null
    private var isRecyclable: Boolean = true
    private var imageUriStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycling_guidance)

        guidanceImageView = findViewById(R.id.guidanceImageView)
        guidanceTitleText = findViewById(R.id.guidanceTitleText)
        suitabilityBadgeText = findViewById(R.id.suitabilityBadgeText)
        instructionsCard = findViewById(R.id.instructionsCard)
        instructionsBodyText = findViewById(R.id.instructionsBodyText)
        nonRecyclableWarningCard = findViewById(R.id.nonRecyclableWarningCard)
        nonRecyclableNoticeText = findViewById(R.id.nonRecyclableNoticeText)
        fallbackCard = findViewById(R.id.fallbackCard)
        btnBackToScan = findViewById(R.id.btnBackToScan)
        btnGoToDashboard = findViewById(R.id.btnGoToDashboard)

        categoryId = intent.getStringExtra("CATEGORY_ID")
        categoryName = intent.getStringExtra("CATEGORY_NAME")
        isRecyclable = intent.getBooleanExtra("IS_RECYCLABLE", true)
        imageUriStr = intent.getStringExtra("IMAGE_URI")

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        btnBackToScan.setOnClickListener { finish() }

        btnGoToDashboard.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        loadCategoryGuidance()
    }

    private fun loadCategoryGuidance() {
        wasteScanRepository.fetchGuidanceContent { result ->
            when (result) {
                is ResultState.Success -> {
                    val matchingGuidance = findMatchingGuidance(result.data)
                    if (matchingGuidance != null) {
                        displayGuidanceContent(matchingGuidance)
                    } else {
                        displayFallbackCategoryGuidance()
                    }
                }
                is ResultState.Error -> {
                    Toast.makeText(this, "Operating in cached/offline guidance mode.", Toast.LENGTH_SHORT).show()
                    displayFallbackCategoryGuidance()
                }
                else -> {
                    displayFallbackCategoryGuidance()
                }
            }
        }
    }

    private fun findMatchingGuidance(guidanceList: List<GuidanceContent>): GuidanceContent? {
        val targetId = categoryId ?: ""
        val targetName = categoryName ?: ""

        return guidanceList.firstOrNull { guidance ->
            (targetId.isNotEmpty() && guidance.wasteCategoryId.equals(targetId, ignoreCase = true)) ||
                    (targetName.isNotEmpty() && guidance.title.contains(targetName, ignoreCase = true))
        }
    }

    private fun displayGuidanceContent(guidance: GuidanceContent) {
        guidanceTitleText.text = guidance.title.ifEmpty { categoryName ?: "Waste Guidance" }
        instructionsBodyText.text = guidance.content

        fallbackCard.visibility = View.GONE
        instructionsCard.visibility = View.VISIBLE

        if (isRecyclable) {
            suitabilityBadgeText.text = "Recyclable via Platform"
            suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_completed)
            suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.primary_green_dark))
            nonRecyclableWarningCard.visibility = View.GONE
        } else {
            suitabilityBadgeText.text = "Cannot be Recycled on Platform"
            suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_cancelled)
            suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.badge_cancelled_text))
            nonRecyclableWarningCard.visibility = View.VISIBLE
        }

        loadImageOrFallback()
    }

    private fun displayFallbackCategoryGuidance() {
        val name = categoryName ?: "Waste Item"

        loadImageOrFallback()

        when {
            name.contains("Plastic", ignoreCase = true) -> {
                guidanceTitleText.text = "Plastics (PET, HDPE)"
                suitabilityBadgeText.text = "Recyclable via Platform"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_completed)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.primary_green_dark))

                instructionsBodyText.text = "• Rinse containers to remove liquid or food residue.\n• Remove bottle caps and recycle separately if required.\n• Avoid soft film plastics or plastic bags in curb bins."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = View.GONE
                fallbackCard.visibility = View.GONE
            }
            name.contains("Paper", ignoreCase = true) || name.contains("Cardboard", ignoreCase = true) -> {
                guidanceTitleText.text = "Paper & Cardboard"
                suitabilityBadgeText.text = "Recyclable via Platform"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_completed)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.primary_green_dark))

                instructionsBodyText.text = "• Flatten cardboard boxes to save space.\n• Keep paper clean and dry (greasy paper cannot be recycled).\n• Shredded paper should be placed in paper bags."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = View.GONE
                fallbackCard.visibility = View.GONE
            }
            name.contains("Glass", ignoreCase = true) -> {
                guidanceTitleText.text = "Glass Bottles & Jars"
                suitabilityBadgeText.text = "Recyclable via Platform"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_completed)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.primary_green_dark))

                instructionsBodyText.text = "• Wash out glass jars and bottles thoroughly.\n• Do not include broken drinking glasses, light bulbs, or mirrors."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = View.GONE
                fallbackCard.visibility = View.GONE
            }
            name.contains("Electronic", ignoreCase = true) || name.contains("E-Waste", ignoreCase = true) -> {
                guidanceTitleText.text = "Electronics & E-Waste"
                suitabilityBadgeText.text = "Special Collection Required"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_scheduled)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.badge_scheduled_text))

                instructionsBodyText.text = "• Schedule a dedicated e-waste pickup request.\n• Tape battery contact terminals before pickup to prevent short circuits."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = View.GONE
                fallbackCard.visibility = View.GONE
            }
            !isRecyclable || name.contains("Non-Recyclable", ignoreCase = true) || name.contains("Hazardous", ignoreCase = true) -> {
                guidanceTitleText.text = name
                suitabilityBadgeText.text = "Cannot be Recycled on Platform"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_cancelled)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.badge_cancelled_text))

                instructionsBodyText.text = "• Store safely away from household waste.\n• Check local municipal rules for designated drop-off sites or hazardous waste disposal."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = View.VISIBLE
                nonRecyclableNoticeText.text = "This waste category cannot be accepted for platform pickups. Please take this item to a certified local collection center."
                fallbackCard.visibility = View.GONE
            }
            name.contains("Unknown", ignoreCase = true) || categoryId.isNullOrEmpty() -> {
                guidanceTitleText.text = "Unknown Classification"
                suitabilityBadgeText.text = "Classification Unverified"
                suitabilityBadgeText.setBackgroundResource(R.drawable.bg_badge_cancelled)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, R.color.badge_cancelled_text))

                instructionsCard.visibility = View.GONE
                nonRecyclableWarningCard.visibility = View.GONE
                fallbackCard.visibility = View.VISIBLE
            }
            else -> {
                guidanceTitleText.text = name
                suitabilityBadgeText.text = if (isRecyclable) "Recyclable via Platform" else "Cannot be Recycled on Platform"
                suitabilityBadgeText.setBackgroundResource(if (isRecyclable) R.drawable.bg_badge_completed else R.drawable.bg_badge_cancelled)
                suitabilityBadgeText.setTextColor(ContextCompat.getColor(this, if (isRecyclable) R.color.primary_green_dark else R.color.badge_cancelled_text))

                instructionsBodyText.text = "• Ensure item is clean, dry, and free of organic contamination.\n• Separate mixed materials into appropriate recycling bins."
                instructionsCard.visibility = View.VISIBLE
                nonRecyclableWarningCard.visibility = if (isRecyclable) View.GONE else View.VISIBLE
                fallbackCard.visibility = View.GONE
            }
        }
    }

    private fun loadImageOrFallback() {
        if (!imageUriStr.isNullOrEmpty()) {
            try {
                guidanceImageView.setImageURI(Uri.parse(imageUriStr))
            } catch (_: Exception) {
                guidanceImageView.setImageResource(R.drawable.ic_bottle)
            }
        } else {
            guidanceImageView.setImageResource(R.drawable.ic_bottle)
        }
    }
}
