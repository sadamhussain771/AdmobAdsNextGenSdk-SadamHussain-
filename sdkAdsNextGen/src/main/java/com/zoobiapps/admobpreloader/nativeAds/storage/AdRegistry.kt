package com.zoobiapps.admobpreloader.nativeAds.storage

import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey
import com.zoobiapps.admobpreloader.nativeAds.model.AdInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * AdRegistry for native ads.
 *
 * Tracks:
 *  - NativeAdKey -> AdInfo configuration
 *  - adUnitId -> preload status (is preloader active)
 *  - adUnitId -> whether an ad was already shown (impression)
 */
internal class AdRegistry {

    private val infoMap = ConcurrentHashMap<NativeAdKey, AdInfo>()
    private val preloadActive = ConcurrentHashMap<String, Boolean>() // adUnitId -> isPreloading(true/false)
    private val adShown = ConcurrentHashMap<String, Boolean>() // adUnitId -> wasShown (impression)

    fun putInfo(key: NativeAdKey, info: AdInfo) = infoMap.put(key, info)
    fun getInfo(key: NativeAdKey) = infoMap[key]
    fun removeInfo(key: NativeAdKey) {
        infoMap.remove(key)
    }

    fun markPreloadActive(adUnitId: String, isActive: Boolean) {
        preloadActive[adUnitId] = isActive
    }

    fun isPreloadActive(adUnitId: String) = preloadActive[adUnitId] == true

    fun removePreload(adUnitId: String) {
        preloadActive.remove(adUnitId)
    }

    fun markAdShown(adUnitId: String) {
        adShown[adUnitId] = true
    }

    fun wasAdShown(adUnitId: String) = adShown[adUnitId] == true

    fun removeAdShown(adUnitId: String) {
        adShown.remove(adUnitId)
    }

    fun clearAll() {
        infoMap.clear()
        preloadActive.clear()
        adShown.clear()
    }

    fun findAdKeyByUnit(adUnitId: String): NativeAdKey? =
        infoMap.entries.firstOrNull { it.value.adUnitId == adUnitId }?.key
}