package com.zoobiapps.admobpreloader.bannerAds.callbacks

/**
 * Simple callback for banner preload results.
 */
fun interface BannerOnLoadCallback {
    fun onResponse(isLoaded: Boolean)
}