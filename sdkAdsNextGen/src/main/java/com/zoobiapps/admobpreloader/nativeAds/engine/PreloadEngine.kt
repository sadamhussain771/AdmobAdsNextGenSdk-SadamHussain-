package com.zoobiapps.admobpreloader.nativeAds.engine

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.zoobiapps.admobpreloader.nativeAds.callbacks.NativeOnLoadCallback
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey
import com.zoobiapps.admobpreloader.nativeAds.model.AdInfo
import com.zoobiapps.admobpreloader.nativeAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Low-level preload engine for native ads.
 */
internal class PreloadEngine(
    private val registry: AdRegistry
) {

    /**
     * Start preloading a native ad for a given AdInfo.
     *
     * If bufferSize is null -> we still start preloader with size=1, but we will stop preloading
     * after the impression (see ShowEngine + registry.markAdShown).
     *
     * If an adUnit is already preloading, we will not start duplicate preloader.
     */
    fun startPreload(key: NativeAdKey, adInfo: AdInfo, listener: NativeOnLoadCallback?) {
        val adUnitId = adInfo.adUnitId

        // avoid duplicate start
        if (registry.isPreloadActive(adUnitId) && NativeAdPreloader.isAdAvailable(adUnitId)) {
            AdLogger.logDebug(key.value, "loadNativeAd", "Ad already available for this ad unit: $adUnitId")
            MainDispatcher.run { listener?.onResponse(true) }
            return
        }

        registry.markPreloadActive(adUnitId, true)
        val buffer = adInfo.bufferSize ?: 1 // pass 1 to SDK if null; we'll stop after impression manually

        val request = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()
        val config = PreloadConfiguration(request, buffer)

        try {
            val started = NativeAdPreloader.start(adUnitId, config, object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    AdLogger.logInfo(key.value, "loadNativeAd", "onAdPreloaded: $preloadId")
                    registry.markPreloadActive(adUnitId, true)
                    // Reset adShown so the refilled buffer ad is eligible for reuse again
                    if (adInfo.bufferSize != null) {
                        registry.removeAdShown(adUnitId)
                    }
                    MainDispatcher.run { listener?.onResponse(true) }
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    AdLogger.logError(
                        key.value,
                        "loadNativeAd",
                        "onAdFailedToPreload: adUnitId: $preloadId, adMessage: ${adError.message}"
                    )
                    registry.markPreloadActive(adUnitId, false)
                    // For non-buffered ads, clear preload on failure (same behavior as interstitials)
                    if (adInfo.bufferSize == null) {
                        registry.removePreload(adUnitId)
                    }
                    MainDispatcher.run { listener?.onResponse(false) }
                }

                override fun onAdsExhausted(preloadId: String) {
                    //AdLogger.logDebug(key.value, "loadNativeAd", "onAdsExhausted: $preloadId")
                }
            })

            if (!started) {
                // Another preloader for same id is already in place. Mark as active and notify.
                AdLogger.logDebug(key.value, "loadNativeAd", "AdUnitId is already in use")
                registry.markPreloadActive(adUnitId, true)
                MainDispatcher.run { listener?.onResponse(NativeAdPreloader.isAdAvailable(adUnitId)) }
            }
        } catch (e: Exception) {
            registry.markPreloadActive(adUnitId, false)
            AdLogger.logError(key.value, "loadNativeAd", "Exception: ${e.message}")
            MainDispatcher.run { listener?.onResponse(false) }
        }
    }

    /**
     * Stop preloading/destroy preloader for a given unit id.
     */
    fun stopPreload(key: NativeAdKey, adUnitId: String) {
        try {
            NativeAdPreloader.destroy(adUnitId)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "stopPreloading (native)", "Exception: ${e.message}")
        } finally {
            registry.removePreload(adUnitId)
        }
    }

    fun stopAll() {
        // Not a direct SDK call for listing preloads; we just clear our state.
        registry.clearAll()
    }
}