package com.zoobiapps.admobpreloader.interstitialAds.engine

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.interstitialAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Responsible for showing an available preloaded ad (via InterstitialAdPreloader.pollAd).
 * When ad is shown/dismissed/failed we notify listener and update registry state.
 */
internal class ShowEngine(
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine
) {

    fun showAd(key: InterAdKey, activity: Activity, adUnitId: String, listener: InterstitialShowListener?) {
        val ad: InterstitialAd? = try {
            InterstitialAdPreloader.pollAd(adUnitId)
        } catch (e: Exception) {
            null
        }

        if (ad == null) {
            AdLogger.logError(key.value, "showInterstitialAd", "Failed to poll ad")
            MainDispatcher.run { listener?.onAdFailedToShow(adUnitId, "Ad not available") }
            return
        }

        AdLogger.logDebug(key.value, "showInterstitialAd", "showing ad")
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                MainDispatcher.run { listener?.onAdShown(adUnitId) }
            }

            override fun onAdImpression() {
                // mark impression and (if bufferSize == null) stop preloading for that unit
                AdLogger.logVerbose(key.value, "showInterstitialAd", "onAdImpression: called")
                registry.markAdShown(adUnitId)
                MainDispatcher.run { listener?.onAdImpression(adUnitId) }
                MainDispatcher.run(300) { listener?.onAdImpressionDelayed(adUnitId) }
                // if bufferSize is null, we should stop automatic reload
                registry.findAdKeyByUnit(adUnitId)?.let { adKey ->
                    val info = registry.getInfo(adKey)
                    if (info?.bufferSize == null) {
                        // stop preloader so SDK doesn't keep buffering automatically
                        preloadEngine.stopPreload(adKey, adUnitId)
                    }
                }
            }

            override fun onAdClicked() {
                //AdLogger.logDebug(key.value, "showInterstitialAd", "onAdClicked: called")
                MainDispatcher.run { listener?.onAdClicked(adUnitId) }
            }

            override fun onAdDismissedFullScreenContent() {
                // Ad consumed; remove mapping if it was single-shot (bufferSize==null)
                AdLogger.logDebug(key.value, "showInterstitialAd", "onAdDismissedFullScreenContent: called")
                registry.findAdKeyByUnit(adUnitId)?.let { adKey ->
                    val info = registry.getInfo(adKey)
                    if (info?.bufferSize == null) {
                        registry.removePreload(adUnitId)
                    }
                }
                MainDispatcher.run { listener?.onAdDismissed(adUnitId) }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                AdLogger.logError(key.value, "showInterstitialAd", "onAdFailedToShowFullScreenContent: ${fullScreenContentError.code} -- ${fullScreenContentError.message}")
                MainDispatcher.run { listener?.onAdFailedToShow(adUnitId, "code=${fullScreenContentError.code} msg=${fullScreenContentError.message}") }
                // On fail, if bufferSize == null, stop preloading
                registry.findAdKeyByUnit(adUnitId)?.let { adKey ->
                    val info = registry.getInfo(adKey)
                    if (info?.bufferSize == null) preloadEngine.stopPreload(adKey, adUnitId)
                }
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "showInterstitialAd", "Exception showing ad: ${e.message}")
            MainDispatcher.run { listener?.onAdFailedToShow(adUnitId, e.message ?: "Exception showing ad") }
        }
    }
}