package com.example.reloop_technologies_mobile_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class RecyclingGuidanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycling_guidance)

        findViewById<View>(R.id.backButton)?.setOnClickListener {
            finish()
        }
    }
}
