package com.zoobiapps.admobpreloader.interstitialAds.storage

/**
 * Tracks adUnitId -> preload status, and mapping from InterAdKey -> AdInfo for config.
 * This is kept minimal — Preloader (GMA) owns the actual ad objects; we track availability status via InterstitialAdPreloader APIs.
 */
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.interstitialAds.model.AdInfo
import java.util.concurrent.ConcurrentHashMap

internal class AdRegistry {
    private val infoMap = ConcurrentHashMap<InterAdKey, AdInfo>()
    private val preloadActive = ConcurrentHashMap<String, Boolean>() // adUnitId -> isPreloading(true/false)
    private val adShown = ConcurrentHashMap<String, Boolean>() // adUnitId -> wasShown (impression)

    fun putInfo(key: InterAdKey, info: AdInfo) = infoMap.put(key, info)
    fun getInfo(key: InterAdKey) = infoMap[key]
    fun removeInfo(key: InterAdKey) { infoMap.remove(key) }

    fun markPreloadActive(adUnitId: String, isActive: Boolean) { preloadActive[adUnitId] = isActive }
    fun isPreloadActive(adUnitId: String) = preloadActive[adUnitId] == true
    fun removePreload(adUnitId: String) { preloadActive.remove(adUnitId) }

    fun markAdShown(adUnitId: String) { adShown[adUnitId] = true }
    fun wasAdShown(adUnitId: String) = adShown[adUnitId] == true
    fun removeAdShown(adUnitId: String) { adShown.remove(adUnitId) }

    fun clearAll() {
        infoMap.clear()
        preloadActive.clear()
        adShown.clear()
    }

    fun findAdKeyByUnit(adUnitId: String): InterAdKey? =
        infoMap.entries.firstOrNull { it.value.adUnitId == adUnitId }?.key
}