package com.zoobiapps.admobpreloader.utils

import android.util.Log

/**
 * Centralized logging utility for inter package.
 * Follows the same clean logging strategy as interstitialAds package.
 */
internal object AdLogger {
    const val TAG_ADS = "TAG_ADS"

    fun logError(adType: String, method: String, message: String) {
        Log.e(TAG_ADS, "$adType -> $method: $message")
    }

    fun logDebug(adType: String, method: String, message: String) {
        Log.d(TAG_ADS, "$adType -> $method: $message")
    }

    fun logInfo(adType: String, method: String, message: String) {
        Log.i(TAG_ADS, "$adType -> $method: $message")
    }

    fun logVerbose(adType: String, method: String, message: String) {
        Log.v(TAG_ADS, "$adType -> $method: $message")
    }
}