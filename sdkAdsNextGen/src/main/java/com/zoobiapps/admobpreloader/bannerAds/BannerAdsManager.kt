package com.zoobiapps.admobpreloader.bannerAds

import android.content.Context
import android.os.Bundle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.zoobiapps.admobpreloader.R
import com.zoobiapps.admobpreloader.bannerAds.callbacks.BannerOnLoadCallback
import com.zoobiapps.admobpreloader.bannerAds.callbacks.BannerOnShowCallback
import com.zoobiapps.admobpreloader.bannerAds.engine.PreloadEngine
import com.zoobiapps.admobpreloader.bannerAds.engine.ShowEngine
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdType
import com.zoobiapps.admobpreloader.bannerAds.model.AdConfig
import com.zoobiapps.admobpreloader.bannerAds.model.AdInfo
import com.zoobiapps.admobpreloader.bannerAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.core.network.InternetManager
import com.zoobiapps.core.storage.SharedPreferencesDataSource

/**
 * Top-level manager for Banner Ads, mirroring NativeAdsManager / InterstitialAdsManager.
 *
 * Responsibilities:
 *  - Validate (premium, internet, remote flag, adUnit empty)
 *  - Map BannerAdKey -> AdConfig
 *  - (Optionally) enforce marketing policies: canShare / canReuse / single-shot vs buffer
 *  - Delegate to PreloadEngine / ShowEngine
 *
 * Public API:
 *  @see loadBannerAd(key, listener)
 *  @see pollBannerAd(key, showCallback)
 *  @see clearBannerAd(key)
 *  @see clearAllBannerAds()
 */
class BannerAdsManager internal constructor(
    private val context: Context,
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine,
    private val showEngine: ShowEngine,
    private val internetManager: InternetManager,
    private val sharedPrefs: SharedPreferencesDataSource
) {

    private val adWidth: Int
        get() {
            val displayMetrics = context.resources.displayMetrics
            val adWidthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            return (adWidthPixels / density).toInt()
        }

    private val adConfigMap: Map<BannerAdKey, AdConfig> by lazy {
        mapOf(
            BannerAdKey.ENTRANCE to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_entrance_id),
                isRemoteEnabled = sharedPrefs.rcBannerEntrance != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_BOTTOM,
                bufferSize = null,
                canShare = true,
                canReuse = true
            ),
            BannerAdKey.SPLASH to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_splash_id),
                isRemoteEnabled = sharedPrefs.rcBannerSplash != 0,
                bannerAdType = BannerAdType.ADAPTIVE,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            /*BannerAdKey.ON_BOARDING to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_on_boarding_id),
                isRemoteEnabled = sharedPrefs.rcBannerOnBoarding != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_TOP,
                bufferSize = null,
                canShare = false,
                canReuse = true
            ),
            BannerAdKey.DASHBOARD to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_dashboard_id),
                isRemoteEnabled = sharedPrefs.rcBannerDashboard != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_TOP,
                bufferSize = null,
                canShare = true,
                canReuse = false
            ),
            BannerAdKey.FEATURE_ONE_A to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_feature_one_a_id),
                isRemoteEnabled = sharedPrefs.rcBannerFeatureOneA != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_TOP,
                bufferSize = null,
                canShare = false,
                canReuse = true
            ),
            BannerAdKey.FEATURE_ONE_B to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_feature_one_b_id),
                isRemoteEnabled = sharedPrefs.rcBannerFeatureOneB != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_BOTTOM,
                bufferSize = null,
                canShare = false,
                canReuse = true
            ),
            BannerAdKey.FEATURE_TWO_A to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_feature_two_a_id),
                isRemoteEnabled = sharedPrefs.rcBannerFeatureTwoA != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_TOP,
                bufferSize = null,
                canShare = false,
                canReuse = true
            ),
            BannerAdKey.FEATURE_TWO_B to AdConfig(
                adUnitId = context.getString(R.string.admob_banner_feature_two_b_id),
                isRemoteEnabled = sharedPrefs.rcBannerFeatureTwoB != 0,
                bannerAdType = BannerAdType.COLLAPSIBLE_BOTTOM,
                bufferSize = null,
                canShare = false,
                canReuse = true
            )*/
        )
    }

    /**
     * Preload a banner ad for the given placement key.
     *
     * The actual AdSize / behavior is derived from the placement's BannerAdType:
     *  - ADAPTIVE          -> anchored adaptive banner based on device width
     *  - MEDIUM_RECTANGLE  -> AdSize.MEDIUM_RECTANGLE
     *  - COLLAPSIBLE_TOP   -> anchored adaptive + "collapsible=top" extras
     *  - COLLAPSIBLE_BOTTOM-> anchored adaptive + "collapsible=bottom" extras
     */
    fun loadBannerAd(key: BannerAdKey, listener: BannerOnLoadCallback? = null) {
        val config = adConfigMap[key] ?: run {
            AdLogger.logError(key.value, "loadBannerAd", "Unknown key")
            listener?.onResponse(false)
            return
        }

        // Validations
        when {
            !isRemoteEnabled(key) -> {
                AdLogger.logError(key.value, "loadBannerAd", "Remote config disabled")
                listener?.onResponse(false)
                return
            }

            sharedPrefs.isAppPurchased -> {
                AdLogger.logDebug(key.value, "loadBannerAd", "Premium user")
                listener?.onResponse(false)
                return
            }

            config.adUnitId.trim().isEmpty() -> {
                AdLogger.logError(key.value, "loadBannerAd", "AdUnit id empty")
                listener?.onResponse(false)
                return
            }

            !internetManager.isInternetConnected -> {
                AdLogger.logError(key.value, "loadBannerAd", "No internet")
                listener?.onResponse(false)
                return
            }
        }

        // Derive AdSize + extras based on BannerAdType
        val (adSize, extras) = when (config.bannerAdType) {
            BannerAdType.ADAPTIVE -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                adWidth
            ) to null

            BannerAdType.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE to null
            BannerAdType.COLLAPSIBLE_TOP -> {
                val size =
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                val bundle = Bundle().apply { putString("collapsible", "top") }
                size to bundle
            }

            BannerAdType.COLLAPSIBLE_BOTTOM -> {
                val size =
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                val bundle = Bundle().apply { putString("collapsible", "bottom") }
                size to bundle
            }
        }

        // register config for lookups
        registry.putInfo(
            key,
            AdInfo(
                config.adUnitId,
                config.canShare,
                config.canReuse,
                config.bufferSize,
                adSize,
                extras
            )
        )

        // Reuse policy (type-aware):
        //  - MEDIUM_RECTANGLE: only reused by other MREC requests
        //  - COLLAPSIBLE_TOP/BOTTOM: prefer same type; if none, may reuse ADAPTIVE
        //  - ADAPTIVE: only reused by other ADAPTIVE requests
        val existingReusableKey = findReusableAdFor(key)
        if (existingReusableKey != null) {
            AdLogger.logDebug(
                key.value,
                "loadBannerAd",
                "Reusing available banner from ${existingReusableKey.value}"
            )
            listener?.onResponse(true)
            return
        }

        AdLogger.logDebug(key.value, "loadBannerAd", "Requesting server for banner ad...")
        preloadEngine.startPreload(
            key,
            AdInfo(
                config.adUnitId,
                config.canShare,
                config.canReuse,
                config.bufferSize,
                adSize,
                extras
            ),
            listener
        )
    }

    /**
     * Polls a preloaded banner ad for the given placement key.
     *
     * The caller is responsible for attaching the returned BannerAd view into a container.
     */
    fun pollBannerAd(
        key: BannerAdKey,
        showCallback: BannerOnShowCallback? = null
    ): BannerAd? {
        val info = registry.getInfo(key)

        // First, try to show this key's own ad if available.
        if (info != null && BannerAdPreloader.isAdAvailable(info.adUnitId)) {
            return showEngine.pollAd(key, info.adUnitId, showCallback)
        }

        // If own ad is not available but this placement canReuse, try to find a reusable ad from another key.
        val reusableKey = findReusableAdFor(key)
        if (reusableKey != null) {
            val reusableInfo = registry.getInfo(reusableKey)
            val unit = reusableInfo?.adUnitId
            if (unit != null && BannerAdPreloader.isAdAvailable(unit)) {
                AdLogger.logDebug(
                    key.value,
                    "pollBannerAd",
                    "Reusing available banner from ${reusableKey.value}"
                )
                return showEngine.pollAd(key, unit, showCallback)
            }
        }

        AdLogger.logError(
            key.value,
            "pollBannerAd",
            "Ad info not found or no banner available for this key"
        )
        showCallback?.onAdFailedToShow()
        return null
    }

    /**
     * Clear a specific placement's banner ad and stop preloading if needed.
     */
    fun clearBannerAd(key: BannerAdKey) {
        val adUnitId = registry.getInfo(key)?.adUnitId ?: return
        AdLogger.logDebug(key.value, "clearBannerAd", "Clearing banner ad")
        preloadEngine.stopPreload(key, adUnitId)
        registry.removeInfo(key)
    }

    /**
     * Clear all banner placement state.
     */
    fun clearAllBannerAds() {
        AdLogger.logDebug("", "clearAllBannerAds", "Clearing all banner ads")
        registry.clearAll()
        preloadEngine.stopAll()
    }

    /** Always reads the current SharedPrefs value so RC changes take effect without restart. */
    private fun isRemoteEnabled(key: BannerAdKey): Boolean = when (key) {
        BannerAdKey.ENTRANCE -> sharedPrefs.rcBannerEntrance != 0
        BannerAdKey.SPLASH -> sharedPrefs.rcBannerSplash != 0
//        BannerAdKey.ON_BOARDING -> sharedPrefs.rcBannerOnBoarding != 0
//        BannerAdKey.DASHBOARD -> sharedPrefs.rcBannerDashboard != 0
//        BannerAdKey.FEATURE_ONE_A -> sharedPrefs.rcBannerFeatureOneA != 0
//        BannerAdKey.FEATURE_ONE_B -> sharedPrefs.rcBannerFeatureOneB != 0
//        BannerAdKey.FEATURE_TWO_A -> sharedPrefs.rcBannerFeatureTwoA != 0
//        BannerAdKey.FEATURE_TWO_B -> sharedPrefs.rcBannerFeatureTwoB != 0
    }

    /**
     * Helper: find any reusable banner key for the requested key, obeying canShare/canReuse flags
     * and BannerAdType compatibility rules.
     */
    private fun findReusableAdFor(requested: BannerAdKey): BannerAdKey? {
        val requestedInfo = registry.getInfo(requested) ?: return null

        // If this placement is not allowed to reuse others, bail out.
        if (!requestedInfo.canReuse) return null

        val requestedType = adConfigMap[requested]?.bannerAdType ?: return null

        // Snapshot of current registry entries
        val entries: List<Pair<BannerAdKey, AdInfo>> = adConfigMap.keys.mapNotNull { key ->
            registry.getInfo(key)?.let { info -> key to info }
        }

        // Helper to filter candidates by generic availability/share flags
        fun baseFilter(key: BannerAdKey, info: AdInfo): Boolean {
            return key != requested &&
                    info.canShare &&
                    !registry.wasAdShown(info.adUnitId) &&
                    registry.isPreloadActive(info.adUnitId) &&
                    BannerAdPreloader.isAdAvailable(info.adUnitId)
        }

        // 1) Prefer same-type reuse first
        val sameTypeCandidate = entries.firstOrNull { (key, info) ->
            if (!baseFilter(key, info)) return@firstOrNull false
            val candidateType = adConfigMap[key]?.bannerAdType ?: return@firstOrNull false
            candidateType == requestedType
        }?.first

        if (sameTypeCandidate != null) return sameTypeCandidate

        // 2) For collapsible placements, allow fallback to ADAPTIVE banners
        if (requestedType == BannerAdType.COLLAPSIBLE_TOP || requestedType == BannerAdType.COLLAPSIBLE_BOTTOM) {
            val adaptiveCandidate = entries.firstOrNull { (key, info) ->
                if (!baseFilter(key, info)) return@firstOrNull false
                val candidateType = adConfigMap[key]?.bannerAdType ?: return@firstOrNull false
                candidateType == BannerAdType.ADAPTIVE
            }?.first

            if (adaptiveCandidate != null) return adaptiveCandidate
        }

        // 3) No compatible reusable banner found
        return null
    }
}