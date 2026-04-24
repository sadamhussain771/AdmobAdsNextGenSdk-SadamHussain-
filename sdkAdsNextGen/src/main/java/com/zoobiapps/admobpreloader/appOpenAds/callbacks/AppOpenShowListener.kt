package com.zoobiapps.admobpreloader.appOpenAds.callbacks

interface AppOpenShowListener {
    fun onAdShown(adUnitId: String) {}
    fun onAdDismissed(adUnitId: String) {}
    fun onAdFailedToShow(adUnitId: String, reason: String) {}
    fun onAdImpression(adUnitId: String) {}
    fun onAdClicked(adUnitId: String) {}
}
