package com.zoobiapps.admobpreloader.interstitialAds

import android.app.Activity
import android.content.res.Resources
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.zoobiapps.admobpreloader.R
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialLoadListener
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.engine.PreloadEngine
import com.zoobiapps.admobpreloader.interstitialAds.engine.ShowEngine
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.interstitialAds.model.AdConfig
import com.zoobiapps.admobpreloader.interstitialAds.model.AdInfo
import com.zoobiapps.admobpreloader.interstitialAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.core.network.InternetManager
import com.zoobiapps.core.storage.SharedPreferencesDataSource

/**
 * The top-level manager you should use in Fragments / Activities.
 * Responsibilities:
 * - Validate (premium, internet, remote flag, adUnit empty)
 * - Map InterAdKey -> AdConfig
 * - Enforce marketing policies: canShare / canReuse / single-shot vs buffer
 * - Delegate to PreloadEngine / ShowEngine
 *
 * Public API:
 *  @see loadInterstitialAd(key, listener)
 *  @see showInterstitialAd(activity, key, listener)
 *  @see destroyInterstitialAd(key)
 *  @see destroyAllInterstitials()
 *  @see isAdLoaded()
 */
class InterstitialAdsManager internal constructor(
    private val resources: Resources,
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine,
    private val showEngine: ShowEngine,
    private val internetManager: InternetManager,
    private val sharedPrefs: SharedPreferencesDataSource
) {

    private val adConfigMap: Map<InterAdKey, AdConfig> by lazy {
        mapOf(
            InterAdKey.ENTRANCE to AdConfig(
                adUnitId = resources.getString(R.string.admob_inter_entrance_id),
                isRemoteEnabled = sharedPrefs.rcInterEntrance != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            InterAdKey.ON_BOARDING to AdConfig(
                adUnitId = resources.getString(R.string.admob_inter_on_boarding_id),
                isRemoteEnabled = sharedPrefs.rcInterOnBoarding != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            InterAdKey.DASHBOARD to AdConfig(
                adUnitId = resources.getString(R.string.admob_inter_dashboard_id),
                isRemoteEnabled = sharedPrefs.rcInterDashboard != 0,
                bufferSize = 1,
                canShare = true,
                canReuse = true
            ),
            InterAdKey.SAVED_VIDEOS to AdConfig(
                adUnitId = resources.getString(R.string.inter_save_admob_id),
                isRemoteEnabled = sharedPrefs.rcInterSavedVideos != 0,
                bufferSize = 1,
                canShare = true,
                canReuse = true
            ),
            InterAdKey.DOWNLOADER_VIDEOS to AdConfig(
                adUnitId = resources.getString(R.string.inter_videos_admob_id),
                isRemoteEnabled = sharedPrefs.rcInterDownloaderVideos != 0,
                bufferSize = 1,
                canShare = true,
                canReuse = true
            ),
            InterAdKey.BOOKMARK_VIDEOS to AdConfig(
                adUnitId = resources.getString(R.string.inter_bookmark_admob_id),
                isRemoteEnabled = sharedPrefs.rcInterBookmarkedVideos != 0,
                bufferSize = 1,
                canShare = true,
                canReuse = true
            ),
            InterAdKey.DP_MAKER_VIDEOS to AdConfig(
                adUnitId = resources.getString(R.string.inter_dpmaker_admob_id),
                isRemoteEnabled = sharedPrefs.rcInterDpMakerVideos != 0,
                bufferSize = 1,
                canShare = true,
                canReuse = true
            ),
        )
    }

    // Public API - minimal & friendly
    fun loadInterstitialAd(key: InterAdKey, listener: InterstitialLoadListener? = null) {
        val config = adConfigMap[key] ?: run {
            AdLogger.logError(key.value, "loadInterstitialAd", "Unknown key")
            listener?.onFailed(key.value, "Unknown key")
            return
        }

        // Validations
        when {
            !isRemoteEnabled(key) -> {
                AdLogger.logError(key.value, "loadInterstitialAd", "Remote config disabled")
                listener?.onFailed(key.value, "Remote config disabled")
                return
            }

            sharedPrefs.isAppPurchased -> {
                AdLogger.logDebug(key.value, "loadInterstitialAd", "Premium user")
                listener?.onFailed(key.value, "Premium user")
                return
            }

            config.adUnitId.trim().isEmpty() -> {
                AdLogger.logError(key.value, "loadInterstitialAd", "AdUnit id empty")
                listener?.onFailed(key.value, "AdUnit id empty")
                return
            }

            !internetManager.isInternetConnected -> {
                AdLogger.logError(key.value, "loadInterstitialAd", "No internet")
                listener?.onFailed(key.value, "No internet")
                return
            }
        }

        // register config for lookups
        registry.putInfo(
            key,
            AdInfo(config.adUnitId, config.canShare, config.canReuse, config.bufferSize)
        )

        // Policy: If *any* ad (which is shareable) already loaded and available with no impression, prefer reuse
        // Find any available ad that can be used instead of loading a new ad, but only if allowed by canShare/canReuse.
        val existingReusableKey = findReusableAdFor(key)
        if (existingReusableKey != null) {
            // We won't start a new preload if there's an available ad: prefer to increase show-rate
            AdLogger.logDebug(
                key.value,
                "loadInterstitialAd",
                "Reusing available ad from ${existingReusableKey.value}"
            )
            listener?.onLoaded(
                adConfigMap[existingReusableKey]?.adUnitId ?: registry.getInfo(
                    existingReusableKey
                )!!.adUnitId
            )
            return
        }

        // else start preload for this key's ad unit
        AdLogger.logDebug(key.value, "loadInterstitialAd", "Requesting admob server for ad...")
        preloadEngine.startPreload(
            key,
            AdInfo(config.adUnitId, config.canShare, config.canReuse, config.bufferSize),
            listener
        )
    }

    fun showInterstitialAd(
        activity: Activity?,
        key: InterAdKey,
        listener: InterstitialShowListener? = null
    ) {
        val config = adConfigMap[key] ?: run {
            AdLogger.logError(key.value, "showInterstitialAd", "Unknown key")
            listener?.onAdFailedToShow(key.value, "Unknown key")
            return
        }

        when {
            activity == null -> {
                AdLogger.logError(key.value, "showInterstitialAd", "Activity reference is null")
                listener?.onAdFailedToShow(key.value, "Activity Ref is null")
                return
            }

            !isRemoteEnabled(key) -> {
                AdLogger.logError(key.value, "showInterstitialAd", "Remote config disabled")
                listener?.onAdFailedToShow(key.value, "Remote config disabled")
                return
            }

            sharedPrefs.isAppPurchased -> {
                AdLogger.logDebug(key.value, "showInterstitialAd", "Premium user")
                listener?.onAdFailedToShow(key.value, "Premium user")
                return
            }

            config.adUnitId.trim().isEmpty() -> {
                AdLogger.logError(key.value, "showInterstitialAd", "Ad id is empty")
                listener?.onAdFailedToShow(key.value, "AdUnit id empty")
                return
            }

            activity.isFinishing || activity.isDestroyed -> {
                AdLogger.logError(
                    key.value,
                    "showInterstitialAd",
                    "Activity is finishing or destroyed"
                )
                listener?.onAdFailedToShow(key.value, "Activity invalid")
                return
            }
        }

        // If this key canReuse==true or the key's own ad is available, prefer own ad.
        // If own ad isn't available but other shareable ad is (and this key allows reuse), use that.
        val ownInfo = registry.getInfo(key)
        val ownUnit = ownInfo?.adUnitId

        if (ownUnit != null && InterstitialAdPreloader.isAdAvailable(ownUnit)) {
            AdLogger.logDebug(key.value, "showInterstitialAd", "Showing own ad")
            showEngine.showAd(key, activity, ownUnit, listener)
            return
        }

        // If own is not available: try to find reusable ad (other key) that canShare == true and available
        val reusableKey = findReusableAdFor(key)
        if (reusableKey != null) {
            val unit = registry.getInfo(reusableKey)?.adUnitId
            if (unit != null && InterstitialAdPreloader.isAdAvailable(unit)) {
                AdLogger.logDebug(
                    key.value,
                    "showInterstitialAd",
                    "Reusing available ad from ${reusableKey.value}"
                )
                showEngine.showAd(key, activity, unit, listener)
                return
            }
        }

        // No ad available — report failure
        AdLogger.logError(key.value, "showInterstitialAd", "Interstitial is not available yet")
        listener?.onAdFailedToShow(key.value, "No available ad to show")
    }

    fun destroyInterstitialAd(key: InterAdKey) {
        registry.getInfo(key)?.adUnitId?.let {
            AdLogger.logDebug(key.value, "destroyInterstitialAd", "Destroying ad")
            preloadEngine.stopPreload(key, it)
        }
        registry.removeInfo(key)
    }

    fun destroyAllInterstitials() {
        AdLogger.logDebug("", "destroyAllInterstitials", "Destroying all ads")
        // stop all based on registry info
        registry.clearAll()
        preloadEngine.stopAll()
    }

    fun isAdLoaded(key: InterAdKey): Boolean {
        val info = registry.getInfo(key) ?: return false
        return InterstitialAdPreloader.isAdAvailable(info.adUnitId)
    }

    // Helper: find any reusable ad key for the requested key
    private fun findReusableAdFor(requested: InterAdKey): InterAdKey? {

        // Prefer same adUnitId if present and not the same key
        val requestedUnit = registry.getInfo(requested)?.adUnitId
        if (requestedUnit != null) {
            val sameUnit = registry.findAdKeyByUnit(requestedUnit)
            if (
                sameUnit != null &&
                sameUnit != requested &&
                registry.getInfo(sameUnit)?.canShare == true &&
                !registry.wasAdShown(requestedUnit) &&
                InterstitialAdPreloader.isAdAvailable(requestedUnit)
            ) {
                return sameUnit
            }
        }

        // Fallback: any shareable, loaded, not-shown, active ad
        val found = registryEntriesFind { (key, info) ->
            key != requested &&
                    info.canShare &&
                    !registry.wasAdShown(info.adUnitId) &&
                    registry.isPreloadActive(info.adUnitId) &&
                    InterstitialAdPreloader.isAdAvailable(info.adUnitId)
        }?.first   // since Pair<InterAdKey, AdInfo>

        return found
    }

    // --------------------------------------------
    // Pair-based matcher
    // --------------------------------------------
    private fun registryEntriesFind(predicate: (Pair<InterAdKey, AdInfo>) -> Boolean): Pair<InterAdKey, AdInfo>? {
        return registrySnapshot().firstOrNull(predicate)
    }

    // --------------------------------------------
    // Snapshot as List<Pair<InterAdKey, AdInfo>>
    // --------------------------------------------
    private fun registrySnapshot(): List<Pair<InterAdKey, AdInfo>> {
        return adConfigMap.keys.mapNotNull { key ->
            registry.getInfo(key)?.let { info -> key to info }
        }
    }

    /** Always reads the current SharedPrefs value so RC changes take effect without restart. */
    private fun isRemoteEnabled(key: InterAdKey): Boolean = when (key) {
        InterAdKey.ENTRANCE -> sharedPrefs.rcInterEntrance != 0
        InterAdKey.ON_BOARDING -> sharedPrefs.rcInterOnBoarding != 0
        InterAdKey.DASHBOARD -> sharedPrefs.rcInterDashboard != 0
        InterAdKey.SAVED_VIDEOS -> sharedPrefs.rcInterSavedVideos != 0
        InterAdKey.DOWNLOADER_VIDEOS -> sharedPrefs.rcInterDownloaderVideos != 0
        InterAdKey.BOOKMARK_VIDEOS -> sharedPrefs.rcInterBookmarkedVideos != 0
        InterAdKey.DP_MAKER_VIDEOS -> sharedPrefs.rcInterDpMakerVideos != 0
//        InterAdKey.BOTTOM_NAVIGATION -> sharedPrefs.rcInterBottomNavigation != 0
//        InterAdKey.BACK_PRESS -> sharedPrefs.rcInterBackpress != 0
//        InterAdKey.EXIT -> sharedPrefs.rcInterExit != 0
    }
}