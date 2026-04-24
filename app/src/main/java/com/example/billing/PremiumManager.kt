package com.customlauncher.app.billing

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.customlauncher.app.di.DIComponent

/**
 * Persists premium status locally. Source of truth is Play Billing restore.
 * Also syncs with the SDK's own isAppPurchased flag so all ad managers
 * automatically respect premium without extra wiring.
 */
class PremiumManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)

    val isPremium: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM, false)

    fun setPremium(value: Boolean) {
        prefs.edit { putBoolean(KEY_IS_PREMIUM, value) }
        // Sync with the SDK's isAppPurchased so InterstitialAdsManager /
        // NativeAdsManager / BannerAdsManager all skip ads for premium users
        runCatching { DIComponent().sharedPrefs.isAppPurchased = value }
    }

    companion object {
        private const val KEY_IS_PREMIUM = "is_premium"
    }
}
