package com.zoobiapps.admobpreloader.interstitialAds.engine


import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialLoadListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.interstitialAds.model.AdInfo
import com.zoobiapps.admobpreloader.interstitialAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Uses Next-Gen SDK's InterstitialAdPreloader and hands back load events.
 * Buffering is delegated to the SDK via PreloadConfiguration.bufferSize.
 */
internal class PreloadEngine(private val registry: AdRegistry) {

    /**
     * Start preloading an ad for a given AdInfo.
     * If bufferSize is null -> we still start preloader with size=1, but we will stop preloading
     * after the impression (registry.markAdShown -> manager will call stop).
     *
     * If an adUnit is already preloading, we will not start duplicate preloader.
     */
    fun startPreload(key: InterAdKey, adInfo: AdInfo, listener: InterstitialLoadListener?) {
        val adUnitId = adInfo.adUnitId

        // avoid duplicate start
        if (registry.isPreloadActive(adUnitId)) {
            AdLogger.logDebug(key.value, "loadInterstitialAd", "Ad is already loading for this ad unit: $adUnitId")
            MainDispatcher.run { listener?.onLoaded(adUnitId) } // if SDK already loaded, reply true
            return
        }

        registry.markPreloadActive(adUnitId, true)
        val buffer = adInfo.bufferSize ?: 1 // pass 1 to SDK if null; we'll stop after impression manually

        val request = AdRequest.Builder(adUnitId).build()
        val config = PreloadConfiguration(request, buffer)

        try {
            val started = InterstitialAdPreloader.start(adUnitId, config, object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    AdLogger.logInfo(key.value, "loadInterstitialAd", "onAdLoaded: adUnitId: $preloadId")
                    registry.markPreloadActive(adUnitId, true)
                    MainDispatcher.run { listener?.onLoaded(adUnitId) }
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    AdLogger.logError(key.value, "loadInterstitialAd", "onAdFailedToLoad: adUnitId: $preloadId, adMessage: ${adError.message}")
                    registry.markPreloadActive(adUnitId, false)
                    // if adInfo.bufferSize == null we might want to remove the preload (already not active)
                    MainDispatcher.run { listener?.onFailed(adUnitId, adError.message) }
                }

                override fun onAdsExhausted(preloadId: String) {
                    // SDK-level events; we don't act specifically here, but could notify metrics
                    //AdLogger.logVerbose(key.value, "loadInterstitialAd", "onAdsExhausted: $preloadId")
                }
            })

            if (!started) {
                // Another preloader for same id is already in place. Mark as active and notify.
                AdLogger.logDebug(key.value, "loadInterstitialAd", "AdUnitId is already in use")
                registry.markPreloadActive(adUnitId, true)
                MainDispatcher.run { listener?.onLoaded(adUnitId) }
            } else {
                registry.markPreloadActive(adUnitId, true)
            }
        } catch (e: Exception) {
            registry.markPreloadActive(adUnitId, false)
            AdLogger.logError(key.value, "loadInterstitialAd", "Exception: ${e.message}")
            MainDispatcher.run { listener?.onFailed(adUnitId, e.message ?: "Exception") }
        }
    }

    /**
     * Stop preloading/destroy preloader for a given unit id.
     */
    fun stopPreload(key: InterAdKey, adUnitId: String) {
        try {
            InterstitialAdPreloader.destroy(adUnitId)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "stopPreloading", "Exception: ${e.message}")
        } finally {
            registry.removePreload(adUnitId)
        }
    }

    fun stopAll() {
        // Not a direct SDK call for listing preloads; we iterate known registry entries.
        // Note: This is called from destroyAllAds, which already logs
        registry.clearAll()
    }
}