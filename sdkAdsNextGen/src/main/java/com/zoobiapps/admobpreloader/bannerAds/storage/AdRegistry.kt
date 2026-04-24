package com.zoobiapps.admobpreloader.bannerAds.storage

import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.bannerAds.model.AdInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * AdRegistry for banner ads.
 *
 * Tracks:
 *  - BannerAdKey -> AdInfo configuration
 *  - adUnitId -> preload status (is preloader active)
 *  - adUnitId -> whether an ad was already shown (impression)
 */
internal class AdRegistry {

    private val infoMap = ConcurrentHashMap<BannerAdKey, AdInfo>()
    private val preloadActive = ConcurrentHashMap<String, Boolean>() // adUnitId -> isPreloading(true/false)
    private val adShown = ConcurrentHashMap<String, Boolean>() // adUnitId -> wasShown (impression)

    fun putInfo(key: BannerAdKey, info: AdInfo) = infoMap.put(key, info)
    fun getInfo(key: BannerAdKey) = infoMap[key]
    fun removeInfo(key: BannerAdKey) {
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

    fun findAdKeyByUnit(adUnitId: String): BannerAdKey? =
        infoMap.entries.firstOrNull { it.value.adUnitId == adUnitId }?.key

    fun getAllAdUnitIds(): Set<String> =
        infoMap.values.map { it.adUnitId }.toSet()
}