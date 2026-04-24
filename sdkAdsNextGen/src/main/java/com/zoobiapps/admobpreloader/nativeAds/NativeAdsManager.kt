package com.zoobiapps.admobpreloader.nativeAds

import android.content.res.Resources
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.zoobiapps.admobpreloader.R
import com.zoobiapps.admobpreloader.nativeAds.callbacks.NativeOnLoadCallback
import com.zoobiapps.admobpreloader.nativeAds.callbacks.NativeOnShowCallback
import com.zoobiapps.admobpreloader.nativeAds.engine.PreloadEngine
import com.zoobiapps.admobpreloader.nativeAds.engine.ShowEngine
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey
import com.zoobiapps.admobpreloader.nativeAds.model.AdConfig
import com.zoobiapps.admobpreloader.nativeAds.model.AdInfo
import com.zoobiapps.admobpreloader.nativeAds.storage.AdRegistry
import com.zoobiapps.admobpreloader.utils.AdLogger
import com.zoobiapps.core.network.InternetManager
import com.zoobiapps.core.storage.SharedPreferencesDataSource

/**
 * Top-level manager for Native Ads, mirroring InterstitialAdsManager.
 *
 * Responsibilities:
 *  - Validate (premium, internet, remote flag, adUnit empty)
 *  - Map NativeAdKey -> AdConfig
 *  - Enforce marketing policies: canShare / canReuse / single-shot vs buffer
 *  - Delegate to PreloadEngine / ShowEngine
 *
 * Public API:
 *  @see loadNativeAd(key, listener)
 *  @see pollNativeAd(key, showCallback)
 *  @see clearNativeAd(key)
 */
class NativeAdsManager internal constructor(
    private val resources: Resources,
    private val registry: AdRegistry,
    private val preloadEngine: PreloadEngine,
    private val showEngine: ShowEngine,
    private val internetManager: InternetManager,
    private val sharedPrefs: SharedPreferencesDataSource
) {

    private val adConfigMap: Map<NativeAdKey, AdConfig> by lazy {
        mapOf(
            NativeAdKey.LANGUAGE to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_language_id),
                isRemoteEnabled = sharedPrefs.rcNativeLanguage != 0,
                bufferSize = null,   // single ad at a time
                canShare = true,
                canReuse = true
            ),
            NativeAdKey.LANGUAGE2 to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_language2_id),
                isRemoteEnabled = sharedPrefs.rcNativeLanguage2 != 0,
                bufferSize = null,   // single ad at a time
                canShare = true,
                canReuse = true
            ),
            NativeAdKey.ON_BOARDING to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_on_boarding_id),
                isRemoteEnabled = sharedPrefs.rcNativeOnBoarding != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            NativeAdKey.ON_BOARDING2 to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_on_boarding2_id),
                isRemoteEnabled = sharedPrefs.rcNativeOnBoarding2 != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            NativeAdKey.ON_BOARDING3 to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_on_boarding3_id),
                isRemoteEnabled = sharedPrefs.rcNativeOnBoarding3 != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            NativeAdKey.GET_STARTED to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_get_started_id),
                isRemoteEnabled = sharedPrefs.rcNativeOnGetStarted != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            NativeAdKey.DASHBOARD to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_home_id),
                isRemoteEnabled = sharedPrefs.rcNativeHome != 0,
                bufferSize = null,
                canShare = false,
                canReuse = true
            ),
            NativeAdKey.FEATURE to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_feature_id),
                isRemoteEnabled = sharedPrefs.rcNativeFeature != 0,
                bufferSize = 2,   // buffer 2 so multiple tabs can each get one
                canShare = true,
                canReuse = true
            ),
            /*NativeAdKey.EXIT to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_exit_id),
                isRemoteEnabled = sharedPrefs.rcNativeExit != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),*/
            NativeAdKey.FULL_SCREEN to AdConfig(
                adUnitId = resources.getString(R.string.admob_native_full_screen_id),
                isRemoteEnabled = sharedPrefs.rcNativeFullScreen != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            ),
            NativeAdKey.FULL_SCREEN2 to AdConfig(
                adUnitId = resources.getString(R.string.native_full_screen2),
                isRemoteEnabled = sharedPrefs.rcNativeFullScreen2 != 0,
                bufferSize = null,
                canShare = false,
                canReuse = false
            )
        )
    }

    /**
     * Preload a native ad for the given placement key.
     */
    fun loadNativeAd(key: NativeAdKey, listener: NativeOnLoadCallback? = null) {
        val config = adConfigMap[key] ?: run {
            AdLogger.logError(key.value, "loadNativeAd", "Unknown key")
            listener?.onResponse(false)
            return
        }

        // Validations
        when {
            !isRemoteEnabled(key) -> {
                AdLogger.logError(key.value, "loadNativeAd", "Remote config disabled")
                listener?.onResponse(false)
                return
            }

            sharedPrefs.isAppPurchased -> {
                AdLogger.logDebug(key.value, "loadNativeAd", "Premium user")
                listener?.onResponse(false)
                return
            }

            config.adUnitId.trim().isEmpty() -> {
                AdLogger.logError(key.value, "loadNativeAd", "AdUnit id empty")
                listener?.onResponse(false)
                return
            }

            !internetManager.isInternetConnected -> {
                AdLogger.logError(key.value, "loadNativeAd", "No internet")
                listener?.onResponse(false)
                return
            }
        }

        // register config for lookups
        registry.putInfo(
            key,
            AdInfo(config.adUnitId, config.canShare, config.canReuse, config.bufferSize)
        )

        // Policy: If *any* ad (which is shareable) already loaded and available with no impression, prefer reuse.
        // This mirrors the behaviour of InterstitialAdsManager.
        val existingReusableKey = findReusableAdFor(key)
        if (existingReusableKey != null) {
            AdLogger.logDebug(
                key.value,
                "loadNativeAd",
                "Reusing available ad from ${existingReusableKey.value}"
            )
            // We already have an ad available, so we don't start another preload. Just signal success.
            listener?.onResponse(true)
            return
        }

        AdLogger.logDebug(key.value, "loadNativeAd", "Requesting server for native ad...")
        preloadEngine.startPreload(
            key,
            AdInfo(config.adUnitId, config.canShare, config.canReuse, config.bufferSize),
            listener
        )
    }

    /**
     * Polls a preloaded native ad for the given placement key.
     *
     * The caller is responsible for binding the returned NativeAd into a NativeAdView.
     */
    fun pollNativeAd(
        key: NativeAdKey,
        showCallback: NativeOnShowCallback? = null
    ): NativeAd? {
        val info = registry.getInfo(key)

        // First, try to show this key's own ad if available.
        if (info != null && NativeAdPreloader.isAdAvailable(info.adUnitId)) {
            return showEngine.pollAd(key, info.adUnitId, showCallback)
        }

        // If own ad is not available but this placement canReuse, try to find a reusable ad from another key.
        val reusableKey = findReusableAdFor(key)
        if (reusableKey != null) {
            val reusableInfo = registry.getInfo(reusableKey)
            val unit = reusableInfo?.adUnitId
            if (unit != null && NativeAdPreloader.isAdAvailable(unit)) {
                AdLogger.logDebug(
                    key.value,
                    "pollNativeAd",
                    "Reusing available ad from ${reusableKey.value}"
                )
                return showEngine.pollAd(key, unit, showCallback)
            }
        }

        AdLogger.logError(
            key.value,
            "pollNativeAd",
            "Ad info not found or no ad available for this key"
        )
        showCallback?.onAdFailedToShow()
        return null
    }

    /**
     * Clear a specific placement's native ad and stop preloading if needed.
     */
    fun clearNativeAd(key: NativeAdKey) {
        val adUnitId = registry.getInfo(key)?.adUnitId ?: return
        AdLogger.logDebug(key.value, "clearNativeAd", "Clearing native ad")
        preloadEngine.stopPreload(key, adUnitId)
        registry.removeInfo(key)
    }

    /**
     * Clear all native placement state. This does not destroy UI views,
     * only the cached/preloaded state.
     */
    fun clearAllNativeAds() {
        AdLogger.logDebug("", "clearAllNativeAds", "Clearing all native ads")
        registry.clearAll()
        preloadEngine.stopAll()
    }

    /**
     * Non-consuming check — returns true if an ad is available for [key].
     * Does NOT poll/consume the ad from the buffer.
     */
    fun isAdAvailable(key: NativeAdKey): Boolean {
        val info = registry.getInfo(key) ?: return false
        return NativeAdPreloader.isAdAvailable(info.adUnitId)
    }

    /** Always reads the current SharedPrefs value so RC changes take effect without restart. */
    private fun isRemoteEnabled(key: NativeAdKey): Boolean = when (key) {
        NativeAdKey.LANGUAGE -> sharedPrefs.rcNativeLanguage != 0
        NativeAdKey.LANGUAGE2 -> sharedPrefs.rcNativeLanguage2 != 0
        NativeAdKey.ON_BOARDING -> sharedPrefs.rcNativeOnBoarding != 0
        NativeAdKey.ON_BOARDING2 -> sharedPrefs.rcNativeOnBoarding2 != 0
        NativeAdKey.ON_BOARDING3 -> sharedPrefs.rcNativeOnBoarding3 != 0
        NativeAdKey.DASHBOARD -> sharedPrefs.rcNativeHome != 0
        NativeAdKey.FEATURE -> sharedPrefs.rcNativeFeature != 0
        NativeAdKey.GET_STARTED -> sharedPrefs.rcNativeOnGetStarted != 0
        //  NativeAdKey.EXIT        -> sharedPrefs.rcNativeExit != 0
        NativeAdKey.FULL_SCREEN -> sharedPrefs.rcNativeFullScreen != 0
        NativeAdKey.FULL_SCREEN2 -> sharedPrefs.rcNativeFullScreen2 != 0
    }

    /**
     * Helper: find any reusable ad key for the requested key, obeying canShare/canReuse flags.
     */
    private fun findReusableAdFor(requested: NativeAdKey): NativeAdKey? {
        val requestedInfo = registry.getInfo(requested) ?: return null

        // If this placement is not allowed to reuse others, bail out.
        if (!requestedInfo.canReuse) return null

        // Prefer same adUnitId if present and not the same key.
        val requestedUnit = requestedInfo.adUnitId
        val sameUnit = registry.findAdKeyByUnit(requestedUnit)
        if (
            sameUnit != null &&
            sameUnit != requested
        ) {
            val sameInfo = registry.getInfo(sameUnit)
            if (
                sameInfo?.canShare == true &&
                !registry.wasAdShown(requestedUnit) &&
                registry.isPreloadActive(requestedUnit) &&
                NativeAdPreloader.isAdAvailable(requestedUnit)
            ) {
                return sameUnit
            }
        }

        // Fallback: any shareable, loaded, not-shown, active ad.
        val found = adConfigMap.keys.mapNotNull { key ->
            registry.getInfo(key)?.let { info -> key to info }
        }.firstOrNull { (key, info) ->
            key != requested &&
                    info.canShare &&
                    !registry.wasAdShown(info.adUnitId) &&
                    registry.isPreloadActive(info.adUnitId) &&
                    NativeAdPreloader.isAdAvailable(info.adUnitId)
        }?.first

        return found
    }
}