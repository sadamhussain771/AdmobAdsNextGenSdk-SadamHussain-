package com.zoobiapps.admobpreloader.bannerAds.engine

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.zoobiapps.admobpreloader.bannerAds.callbacks.BannerOnLoadCallback
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.bannerAds.model.AdInfo
import com.zoobiapps.admobpreloader.bannerAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Low-level preload engine for banner ads.
 *
 * Mirrors native/interstitial preload engines, but uses BannerAdPreloader/BannerAdRequest.
 */
internal class PreloadEngine(
    private val registry: AdRegistry
) {

    /**
     * Start preloading a banner ad for a given AdInfo.
     *
     * If bufferSize is null -> we still start preloader with default buffering (1),
     * and will stop preloading after impression (see ShowEngine + registry.markAdShown).
     *
     * If an adUnit is already preloading and an ad is available, we do not start a duplicate preloader.
     */
    fun startPreload(key: BannerAdKey, adInfo: AdInfo, listener: BannerOnLoadCallback?) {
        val adUnitId = adInfo.adUnitId

        // avoid duplicate start
        if (registry.isPreloadActive(adUnitId) && BannerAdPreloader.isAdAvailable(adUnitId)) {
            AdLogger.logDebug(key.value, "loadBannerAd", "Ad already available for this ad unit: $adUnitId")
            MainDispatcher.run { listener?.onResponse(true) }
            return
        }

        registry.markPreloadActive(adUnitId, true)
        val buffer = adInfo.bufferSize ?: 1

        // Use provided AdSize (e.g., adaptive / MREC) when available, otherwise fall back to standard BANNER.
        val size = adInfo.adSize ?: AdSize.BANNER
        val request = BannerAdRequest.Builder(adUnitId, size).apply {
            adInfo.extras?.let { setGoogleExtrasBundle(it) }
        }.build()
        val config = PreloadConfiguration(request, buffer)

        try {
            val started = BannerAdPreloader.start(adUnitId, config, object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    AdLogger.logInfo(key.value, "loadBannerAd", "onAdPreloaded: $preloadId")
                    registry.markPreloadActive(adUnitId, true)
                    MainDispatcher.run { listener?.onResponse(true) }
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    AdLogger.logError(key.value, "loadBannerAd", "onAdFailedToPreload: adUnitId: $preloadId, adMessage: ${adError.message}")
                    registry.markPreloadActive(adUnitId, false)
                    if (adInfo.bufferSize == null) {
                        registry.removePreload(adUnitId)
                    }
                    MainDispatcher.run { listener?.onResponse(false) }
                }

                override fun onAdsExhausted(preloadId: String) {
                    //AdLogger.logDebug(key.value, "loadBannerAd", "onAdsExhausted: $preloadId")
                }
            })

            if (!started) {
                AdLogger.logDebug(key.value, "loadBannerAd", "AdUnitId is already in use")
                registry.markPreloadActive(adUnitId, true)
                MainDispatcher.run { listener?.onResponse(BannerAdPreloader.isAdAvailable(adUnitId)) }
            }
        } catch (e: Exception) {
            registry.markPreloadActive(adUnitId, false)
            AdLogger.logError(key.value, "loadBannerAd", "Exception: ${e.message}")
            MainDispatcher.run { listener?.onResponse(false) }
        }
    }

    /**
     * Stop preloading/destroy preloader for a given unit id.
     */
    fun stopPreload(key: BannerAdKey, adUnitId: String) {
        try {
            BannerAdPreloader.destroy(adUnitId)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "stopPreloading (banner)", "Exception: ${e.message}")
        } finally {
            registry.removePreload(adUnitId)
        }
    }

    fun stopAll() {
        // Destroy all active preloads before clearing registry
        // We iterate over registry entries to get all adUnitIds
        val allAdUnitIds = registry.getAllAdUnitIds()
        allAdUnitIds.forEach { adUnitId ->
            try {
                BannerAdPreloader.destroy(adUnitId)
            } catch (e: Exception) {
                AdLogger.logError("", "stopAll (banner)", "Exception destroying $adUnitId: ${e.message}")
            }
        }
        registry.clearAll()
    }
}