package com.zoobiapps.admobpreloader.appOpenAds

import android.app.Activity
import android.content.res.Resources
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.zoobiapps.core.storage.SharedPreferencesDataSource
import com.zoobiapps.admobpreloader.R

/**
 * Manages App Open ads following the official NextGen SDK pattern.
 *
 * RC flag: "appOpen" — 1 = enabled, 0 = disabled
 *
 * Violations prevented:
 *  - Never shows during splash (isSplashComplete guard)
 *  - Never shows for premium users
 *  - Never shows if another full-screen ad is already on screen (isShowingAd)
 *  - Ads expire after 4 hours (Google policy)
 *  - Reloads automatically after each show so next foreground has an ad ready
 */
class AppOpenAdsManager(
    private val resources: Resources,
    private val sharedPrefs: SharedPreferencesDataSource
) {
    private val tag = "AppOpenAdsManager"
    private val adUnitId: String by lazy { resources.getString(R.string.admob_app_open_id) }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0

    var isShowingAd = false
        private set

    // Set to true once splash finishes — gates all foregrounding ads
    var isSplashComplete = false

    private val isAppOpenEnabled get() = sharedPrefs.rcAppOpen != 0
    private val isPremium get() = sharedPrefs.isAppPurchased

    private fun isAdAvailable(): Boolean {
        if (appOpenAd == null) return false
        val elapsed = System.currentTimeMillis() - loadTime
        return elapsed < 4 * 60 * 60 * 1000L // 4 hours
    }

    /** Load an ad. No-op if already loading or a valid ad is cached. */
    fun loadAd() {
        if (isPremium || !isAppOpenEnabled) {
            Log.d(tag, "loadAd skipped — premium=$isPremium rcEnabled=$isAppOpenEnabled")
            return
        }
        if (isLoadingAd || isAdAvailable()) {
            Log.d(tag, "loadAd skipped — already loading or ad available")
            return
        }

        isLoadingAd = true
        Log.d(tag, "loadAd — requesting ad for $adUnitId")

        AppOpenAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(tag, "onAdLoaded")
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(tag, "onAdFailedToLoad: ${error.message}")
                    isLoadingAd = false
                }
            }
        )
    }

    /**
     * Show the ad if one is available and conditions are met.
     * Called from App.onStart() when the app comes to foreground.
     */
    fun showAdIfAvailable(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (!isSplashComplete) {
            Log.d(tag, "showAdIfAvailable skipped — splash not complete")
            onComplete?.invoke()
            return
        }
        if (isPremium) {
            Log.d(tag, "showAdIfAvailable skipped — premium user")
            onComplete?.invoke()
            return
        }
        if (!isAppOpenEnabled) {
            Log.d(tag, "showAdIfAvailable skipped — RC disabled")
            onComplete?.invoke()
            return
        }
        if (isShowingAd) {
            Log.d(tag, "showAdIfAvailable skipped — already showing")
            onComplete?.invoke()
            return
        }
        if (!isAdAvailable()) {
            Log.d(tag, "showAdIfAvailable — no ad ready, loading for next time")
            onComplete?.invoke()
            loadAd()
            return
        }

        val ad = appOpenAd ?: run { onComplete?.invoke(); loadAd(); return }

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(tag, "onAdShowedFullScreenContent")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(tag, "onAdDismissedFullScreenContent")
                appOpenAd = null
                isShowingAd = false
                onComplete?.invoke()
                loadAd() // preload next
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.w(tag, "onAdFailedToShowFullScreenContent: ${error.message}")
                appOpenAd = null
                isShowingAd = false
                onComplete?.invoke()
                loadAd() // preload next
            }

            override fun onAdImpression() {
                Log.d(tag, "onAdImpression")
            }

            override fun onAdClicked() {
                Log.d(tag, "onAdClicked")
            }
        }

        Log.d(tag, "showing ad")
        isShowingAd = true
        ad.show(activity)
    }
}