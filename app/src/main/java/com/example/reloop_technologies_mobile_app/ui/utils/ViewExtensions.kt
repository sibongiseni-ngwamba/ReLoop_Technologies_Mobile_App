package com.example.reloop_technologies_mobile_app.ui.utils

import android.os.SystemClock
import android.view.View

/**
 * Custom click listener extension that prevents rapid repeated clicks / double taps (debouncing).
 * @param debounceIntervalMs Minimum time interval in milliseconds between valid click events (default 800ms).
 * @param onSingleClick Action executed when a valid single click occurs.
 */
fun View.setSingleClickListener(
    debounceIntervalMs: Long = 800L,
    onSingleClick: (View) -> Unit
) {
    var lastClickTime = 0L

    setOnClickListener { v ->
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceIntervalMs) {
            lastClickTime = currentTime
            onSingleClick(v)
        }
    }
}
