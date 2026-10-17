package com.example.reloop_technologies_mobile_app

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        findViewById<TextView>(R.id.loginText)?.text = Html.fromHtml(
            "Already have an account? <font color='#237A3B'><b>Log in</b></font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        findViewById<TextView>(R.id.loginText)?.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.signUpButton)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
    }
}
