package com.zoobiapps.admobpreloader.bannerAds.callbacks

/**
 * Callbacks for banner ad show / lifecycle.
 *
 * The view binding lives in the app layer; this is just for events.
 */
interface BannerOnShowCallback {
    fun onAdImpression() {}
    fun onAdImpressionDelayed() {}
    fun onAdClicked() {}
    fun onAdFailedToShow() {}
}