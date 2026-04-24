package com.zoobiapps.admobpreloader.bannerAds.engine

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.zoobiapps.admobpreloader.bannerAds.callbacks.BannerOnShowCallback
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.bannerAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.admobpreloader.utils.MainDispatcher

/**
 * Responsible for polling a preloaded BannerAd and wiring BannerAdEventCallback.
 *
 * The caller is responsible for attaching the returned BannerAd view into a container.
 */
internal class ShowEngine(
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine
) {

    /**
     * Polls an ad from BannerAdPreloader and attaches callbacks.
     *
     * Returns the BannerAd so that the caller can add its view to the layout.
     */
    fun pollAd(key: BannerAdKey, adUnitId: String, listener: BannerOnShowCallback?): BannerAd? {
        val bannerAd: BannerAd? = try {
            BannerAdPreloader.pollAd(adUnitId)
        } catch (e: Exception) {
            AdLogger.logError(key.value, "pollBannerAd", "Exception polling ad: ${e.message}")
            null
        }

        if (bannerAd == null) {
            AdLogger.logError(key.value, "pollBannerAd", "no ad available")
            MainDispatcher.run { listener?.onAdFailedToShow() }
            return null
        }

        AdLogger.logDebug(key.value, "pollBannerAd", "got ad")

        bannerAd.adEventCallback = object : BannerAdEventCallback {
            override fun onAdImpression() {
                AdLogger.logVerbose(key.value, "pollBannerAd", "onAdImpression")
                registry.markAdShown(adUnitId)
                MainDispatcher.run { listener?.onAdImpression() }
                MainDispatcher.run(300) { listener?.onAdImpressionDelayed() }

                // For non-buffered ads, clear preload after first impression
                registry.findAdKeyByUnit(adUnitId)?.let { adKey ->
                    val info = registry.getInfo(adKey)
                    if (info?.bufferSize == null) {
                        preloadEngine.stopPreload(adKey, adUnitId)
                    }
                }
            }

            override fun onAdClicked() {
                MainDispatcher.run { listener?.onAdClicked() }
            }

            override fun onAdPaid(value: AdValue) {
                //MainDispatcher.run { listener?.onAdPaid(value) }
            }
        }

        return bannerAd
    }
}
