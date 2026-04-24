package com.zoobiapps.admobpreloader.interstitialAds.callbacks

interface InterstitialShowListener {
    fun onAdShown(key: String) {}
    fun onAdImpression(key: String) {}
    fun onAdImpressionDelayed(key: String) {}
    fun onAdClicked(key: String) {}
    fun onAdDismissed(key: String) {}
    fun onAdFailedToShow(key: String, reason: String) {}
}