package com.zoobiapps.admobpreloader.nativeAds.engine

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.zoobiapps.admobpreloader.nativeAds.callbacks.NativeOnShowCallback
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey
import com.zoobiapps.admobpreloader.nativeAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Responsible for polling a preloaded NativeAd and wiring NativeAdEventCallback.
 */
internal class ShowEngine(
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine
) {

    /**
     * Polls an ad from NativeAdPreloader and attaches callbacks.
     *
     * Returns the NativeAd so that the caller can bind it into a NativeAdView.
     */
    fun pollAd(key: NativeAdKey, adUnitId: String, listener: NativeOnShowCallback?): NativeAd? {
        val result: NativeAdLoadResult? = try {
            NativeAdPreloader.pollAd(adUnitId)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "pollNativeAd", "Exception polling ad: ${e.message}")
            null
        }

        val nativeAd = (result as? NativeAdLoadResult.NativeAdSuccess)?.ad
        if (nativeAd == null) {
            AdLogger.logError(key.value, "pollNativeAd", "no ad available")
            MainDispatcher.run { listener?.onAdFailedToShow() }
            return null
        }

        AdLogger.logDebug(key.value, "pollNativeAd", "got ad, responseInfo=${nativeAd.getResponseInfo().responseId}")

        nativeAd.adEventCallback = object : NativeAdEventCallback {
            override fun onAdImpression() {
                AdLogger.logVerbose(key.value, "pollNativeAd", "onAdImpression")
                MainDispatcher.run { listener?.onAdImpression() }
                MainDispatcher.run(300) { listener?.onAdImpressionDelayed() }

                // For non-buffered ads, clear preload after first impression
                registry.findAdKeyByUnit(adUnitId)?.let { adKey ->
                    val info = registry.getInfo(adKey)
                    if (info?.bufferSize == null) {
                        preloadEngine.stopPreload(adKey, adUnitId)
                    }
                    // For buffered ads: do NOT mark as shown — the preloader keeps
                    // refilling the buffer and the ad unit should remain reusable.
                    // Marking it shown permanently blocks findReusableAdFor() even
                    // after the buffer is refilled, causing "Ad info not found" errors.
                    else {
                        registry.removeAdShown(adUnitId)
                    }
                }
            }

            override fun onAdClicked() {
                //AdLogger.logDebug(key.value, "pollNativeAd", "onAdClicked")
                MainDispatcher.run { listener?.onAdClicked() }
            }

            override fun onAdPaid(value: AdValue) {
                //AdLogger.logDebug(key.value, "pollNativeAd", "onPaid ${value.valueMicros} ${value.currencyCode}")
            }
        }

        return nativeAd
    }
}