package com.example.reloop_technologies_mobile_app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanWasteActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var capturedImageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var actionButtonsLayout: View
    private lateinit var retakeButton: Button
    private lateinit var submitScanButton: Button
    private lateinit var progressBar: ProgressBar

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageUri: Uri? = null
    private var isSubmitting = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to scan waste.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_waste)

        previewView = findViewById(R.id.previewView)
        capturedImageView = findViewById(R.id.capturedImageView)
        captureButton = findViewById(R.id.captureButton)
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout)
        retakeButton = findViewById(R.id.retakeButton)
        submitScanButton = findViewById(R.id.submitScanButton)
        progressBar = findViewById(R.id.progressBar)

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }
        findViewById<View>(R.id.tipsCard)?.setOnClickListener {
            startActivity(Intent(this, RecyclingGuidanceActivity::class.java))
        }

        captureButton.setSingleClickListener {
            takePhoto()
        }

        retakeButton.setSingleClickListener {
            resetCameraView()
        }

        submitScanButton.setSingleClickListener {
            if (isSubmitting) return@setSingleClickListener
            submitScan()
        }

        checkCameraPermissionAndStart()
        setupBottomNav()
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera: ${exc.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull() ?: filesDir,
            "ReLoop_Scan_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@ScanWasteActivity, "Capture failed: ${exc.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImageUri = output.savedUri ?: Uri.fromFile(photoFile)
                    showCapturedPreview()
                }
            }
        )
    }

    private fun showCapturedPreview() {
        capturedImageView.setImageURI(capturedImageUri)
        capturedImageView.visibility = View.VISIBLE
        previewView.visibility = View.GONE

        captureButton.visibility = View.GONE
        actionButtonsLayout.visibility = View.VISIBLE
    }

    private fun resetCameraView() {
        capturedImageUri = null
        capturedImageView.setImageURI(null)
        capturedImageView.visibility = View.GONE
        previewView.visibility = View.VISIBLE

        actionButtonsLayout.visibility = View.GONE
        captureButton.visibility = View.VISIBLE
        isSubmitting = false
        submitScanButton.isEnabled = true
        retakeButton.isEnabled = true
    }

    private fun submitScan() {
        if (capturedImageUri == null) {
            Toast.makeText(this, "Please capture an image first.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        submitScanButton.isEnabled = false
        retakeButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        Toast.makeText(this, "Analyzing and classifying waste...", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("IMAGE_URI", capturedImageUri.toString())
            }
            startActivity(intent)
            finish()
        }, 1200)
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.navScan)?.setOnClickListener { /* Already on Scan */ }
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
