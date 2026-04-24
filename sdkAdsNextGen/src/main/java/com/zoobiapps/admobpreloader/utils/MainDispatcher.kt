package com.zoobiapps.admobpreloader.utils

import android.os.Handler
import android.os.Looper

internal object MainDispatcher {
    private val handler = Handler(Looper.getMainLooper())
    fun run(delayMs: Long = 0L, action: () -> Unit) {
        if (delayMs <= 0L) handler.post(action) else handler.postDelayed(action, delayMs)
    }
}