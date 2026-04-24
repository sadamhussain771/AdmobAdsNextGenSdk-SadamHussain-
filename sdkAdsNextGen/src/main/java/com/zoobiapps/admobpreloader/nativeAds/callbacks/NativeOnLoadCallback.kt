package com.zoobiapps.admobpreloader.nativeAds.callbacks

fun interface NativeOnLoadCallback {
    fun onResponse(isLoaded: Boolean)
}