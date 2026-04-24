package com.zoobiapps.admobpreloader.appOpenAds.callbacks

interface AppOpenLoadListener {
    fun onLoaded(adUnitId: String) {}
    fun onFailed(adUnitId: String, reason: String) {}
}
