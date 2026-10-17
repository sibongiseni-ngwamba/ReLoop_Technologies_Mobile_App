package com.example.reloop_technologies_mobile_app

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.signUpText)?.text = Html.fromHtml(
            "Don't have an account? <font color='#237A3B'><b>Sign up</b></font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        findViewById<TextView>(R.id.signUpText)?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<Button>(R.id.loginButton)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}
