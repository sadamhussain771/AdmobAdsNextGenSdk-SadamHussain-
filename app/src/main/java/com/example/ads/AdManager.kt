package com.example.ads

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import com.airbnb.lottie.LottieAnimationView
import com.customlauncher.app.di.DIComponent
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey
import com.zoobiapps.core.storage.SharedPreferencesDataSource

/**
 * Central ad manager that gates every load/show call behind Remote Config flags.
 *
 * VIOLATION PREVENTION:
 * Google policy forbids showing an interstitial while an app open ad is showing and vice versa.
 * [isFullScreenAdShowing] is the single source of truth — both showInterstitial and showAppOpen
 * check it before showing and set/clear it around the ad lifecycle.
 */
class AdManager(
    private val sharedPrefs: SharedPreferencesDataSource,
    private val di: DIComponent
) {
    private val tag = "AdManager"

    // ---- Global full-screen conflict guard ----
    // True while ANY full-screen ad (app open OR interstitial) is on screen.
    // Prevents the interstitial + app open overlap violation.
    var isFullScreenAdShowing: Boolean = false
        private set

    // ---- Native ad in-flight guard ----
    // Tracks pending callbacks waiting for a native ad load to complete.
    // Key insight: NativeAdPreloader is a persistent buffer — once started it
    // keeps refilling automatically. We must NOT call loadNativeAd repeatedly
    // for restock; that fights the SDK and causes "AdUnitId is already in use"
    // responses with isAdAvailable=false, making every poll return null.
    private val nativePendingCallbacks =
        mutableMapOf<NativeAdKey, MutableList<(NativeAd?) -> Unit>>()
    private val nativeLoadInFlight = mutableSetOf<NativeAdKey>()

    // ---- Global interstitial frequency cap ----
    // Single counter shared across ALL keys except ENTRANCE and ON_BOARDING.
    // Ad shows on every 3rd click regardless of which key/fragment triggered it.
    private var globalInterClickCount = 0
    private val INTER_SHOW_EVERY_N = 3
    //private val uncappedKeys = setOf(InterAdKey.CLAP_SPLASH, InterAdKey.INTRUDER_SPLASH, InterAdKey.ON_BOARDING)

    private fun shouldShowInterstitial(key: InterAdKey): Boolean {
       // if (key in uncappedKeys) return true
        globalInterClickCount++
        return globalInterClickCount % INTER_SHOW_EVERY_N == 0
    }

    private fun resetGlobalCounter() {
        globalInterClickCount = 0
    }

    // ---- Premium guard ----
    private val isPremium get() = di.premiumManager.isPremium

    private fun isEnabled(value: Int) = value != 0

    // ---- RC flag helpers ----

    /** Returns true if the interstitial for [key] is loaded and ready to show. */
    fun isInterstitialLoaded(key: InterAdKey): Boolean =
        !isPremium && isInterEnabled(key) && di.interstitialAdsManager.isAdLoaded(key)

    fun isInterEnabled(key: InterAdKey): Boolean {
        val rcKey = when (key) {
            InterAdKey.DASHBOARD -> sharedPrefs.rcInterDashboard
            InterAdKey.ON_BOARDING -> sharedPrefs.rcInterOnBoarding
            InterAdKey.SAVED_VIDEOS -> sharedPrefs.rcInterSavedVideos
            InterAdKey.BOOKMARK_VIDEOS -> sharedPrefs.rcInterBookmarkedVideos
            InterAdKey.DP_MAKER_VIDEOS -> sharedPrefs.rcInterDpMakerVideos
            else -> return false
        }
        return isEnabled(rcKey)
    }

    fun isNativeEnabled(key: NativeAdKey): Boolean {
        val rcKey = when (key) {
            NativeAdKey.DASHBOARD -> sharedPrefs.rcNativeHome
            NativeAdKey.ON_BOARDING -> sharedPrefs.rcNativeOnBoarding
            NativeAdKey.ON_BOARDING2 -> sharedPrefs.rcNativeOnBoarding2
            NativeAdKey.ON_BOARDING3 -> sharedPrefs.rcNativeOnBoarding3
            NativeAdKey.GET_STARTED -> sharedPrefs.rcNativeOnGetStarted
            NativeAdKey.LANGUAGE -> sharedPrefs.rcNativeLanguage
            NativeAdKey.LANGUAGE2 -> sharedPrefs.rcNativeLanguage2
            //  NativeAdKey.EXIT        -> sharedPrefs.rcNativeExit
            NativeAdKey.FULL_SCREEN -> sharedPrefs.rcNativeFullScreen
            NativeAdKey.FULL_SCREEN2 -> sharedPrefs.rcNativeFullScreen2
            else -> 1
        }
        return isEnabled(rcKey)
    }

    fun isBannerEnabled(key: BannerAdKey): Boolean {
        val rcKey = when (key) {
            //  BannerAdKey.DASHBOARD   -> sharedPrefs.rcBannerDashboard
            //  BannerAdKey.ON_BOARDING -> sharedPrefs.rcBannerOnBoarding
            BannerAdKey.ENTRANCE -> sharedPrefs.rcBannerEntrance
            else -> 1
        }
        return isEnabled(rcKey)
    }

    fun shouldShowNative(key: NativeAdKey): Boolean = !isPremium && isNativeEnabled(key)

    // ---- Load calls (RC-gated) ----

    fun loadInterstitial(key: InterAdKey) {
        if (isPremium || !isInterEnabled(key)) return
        di.interstitialAdsManager.loadInterstitialAd(key)
    }

    fun loadNative(key: NativeAdKey, onResult: (loaded: Boolean) -> Unit = {}) {
        if (isPremium || !isNativeEnabled(key)) {
            onResult(false); return
        }
        // Single call — the SDK's persistent preloader handles retries internally.
        // Multiple rapid calls cause "AdUnitId is already in use" spam.
        di.nativeAdsManager.loadNativeAd(key) { onResult(it) }
    }

    /**
     * Single entry point for fragments/adapters that need a native ad.
     *
     * SDK behavior (NativeAdPreloader with bufferSize=2 for FEATURE):
     *  - Once started, the preloader runs persistently and refills the buffer automatically.
     *  - NativeAdPreloader.start() returns false ("already in use") if already running.
     *    In that case onResponse fires with isAdAvailable — true if buffer has an ad, false if empty.
     *  - When the buffer is empty and refilling, the ONLY reliable signal is the next
     *    onAdPreloaded callback — which fires via loadNativeAd's listener.
     *
     * Strategy: call loadNativeAd() and wait for its callback. That callback fires either:
     *   - Immediately with true  → ad is in buffer right now, poll it
     *   - Immediately with false → buffer empty, preloader running — wait for next onAdPreloaded
     *   - After network round-trip with true/false → first-time start
     *
     * For the false case we call loadNativeAd() again after a short delay — this re-registers
     * our listener with the already-running preloader so we get notified on next onAdPreloaded.
     * We limit retries to avoid infinite loops when there's genuinely no fill.
     *
     * Always delivers result on the main thread.
     */
    fun getOrLoadNative(key: NativeAdKey, onReady: (NativeAd?) -> Unit) {
        if (isPremium || !isNativeEnabled(key)) {
            Handler(Looper.getMainLooper()).post { onReady(null) }
            return
        }
        val mainHandler = Handler(Looper.getMainLooper())

        // Poll immediately — buffer may already have an ad
        val cached = di.nativeAdsManager.pollNativeAd(key, null)
        if (cached != null) {
            mainHandler.post { onReady(cached as NativeAd?) }
            return
        }

        // Queue callback. If a chain is already running for this key, just wait.
        nativePendingCallbacks.getOrPut(key) { mutableListOf() }.add(onReady)
        if (nativeLoadInFlight.contains(key)) {
            Log.d(tag, "Native $key already in-flight — queuing")
            return
        }

        // Start the persistent chain — it runs until it delivers or gives up
        nativeLoadInFlight.add(key)
        runNativeChain(key, mainHandler, attemptsLeft = 8)
    }

    /**
     * Persistent chain that calls loadNativeAd() and waits for its callback.
     *
     * The chain stays alive (nativeLoadInFlight remains set) until it either:
     *  - Delivers an ad to all pending callbacks, then terminates
     *  - Exhausts all attempts, delivers null, then terminates
     *
     * New getOrLoadNative() callers that arrive while the chain is running
     * simply queue into nativePendingCallbacks and get served when the chain delivers.
     *
     * This prevents multiple parallel chains from fighting each other with
     * loadNativeAd() spam.
     */
    private fun runNativeChain(key: NativeAdKey, mainHandler: Handler, attemptsLeft: Int) {
        di.nativeAdsManager.loadNativeAd(key) { loaded ->
            // Check if there are still pending callers — if not, terminate the chain
            val hasPending = nativePendingCallbacks[key]?.isNotEmpty() == true
            if (!hasPending) {
                nativeLoadInFlight.remove(key)
                return@loadNativeAd
            }

            if (loaded) {
                val ad = di.nativeAdsManager.pollNativeAd(key, null)
                if (ad != null) {
                    // Deliver to all waiting callbacks and terminate the chain
                    nativeLoadInFlight.remove(key)
                    val pending = nativePendingCallbacks.remove(key) ?: return@loadNativeAd
                    pending.forEach { cb -> mainHandler.post { cb(ad as NativeAd?) } }
                } else if (attemptsLeft > 0) {
                    // SDK timing race — retry quickly
                    mainHandler.postDelayed(
                        { runNativeChain(key, mainHandler, attemptsLeft - 1) },
                        300L
                    )
                } else {
                    nativeLoadInFlight.remove(key)
                    val pending = nativePendingCallbacks.remove(key) ?: return@loadNativeAd
                    pending.forEach { cb -> mainHandler.post { cb(null) } }
                }
            } else {
                // Buffer empty (preloader running) or load failed — retry after delay
                if (attemptsLeft > 0) {
                    mainHandler.postDelayed(
                        { runNativeChain(key, mainHandler, attemptsLeft - 1) },
                        800L
                    )
                } else {
                    nativeLoadInFlight.remove(key)
                    val pending = nativePendingCallbacks.remove(key) ?: return@loadNativeAd
                    pending.forEach { cb -> mainHandler.post { cb(null) } }
                }
            }
        }
    }

    fun loadBanner(key: BannerAdKey) {
        if (isPremium || !isBannerEnabled(key)) return
        di.bannerAdsManager.loadBannerAd(key)
    }

    /**
     * Single entry point for fragments that need a banner ad.
     * Checks if the ad is already available (preloaded by preloadAll), polls it immediately.
     * If not ready yet, loads and delivers via [onReady] once available.
     * This fixes the race where preloadAll() started loading before the fragment registered
     * its callback, causing the callback to fire with isAdAvailable=false.
     */
    fun getOrLoadBanner(key: BannerAdKey, onReady: (BannerAd?) -> Unit) {
        if (isPremium || !isBannerEnabled(key)) {
            onReady(null); return
        }
        val preloaded = di.bannerAdsManager.pollBannerAd(key)
        if (preloaded != null) {
            onReady(preloaded as BannerAd?)
            if (key == BannerAdKey.ENTRANCE) di.bannerAdsManager.loadBannerAd(key)
            return
        }
        // Not ready yet — start loading and wait for callback.
        // If the SDK reports "already in use" (success=false), it means preloadAll() already
        // kicked off a load for this key. In that case we retry polling after a short delay
        // rather than giving up — the ad will be in the buffer once the in-flight load finishes.
        loadBannerWithRetry(key, onReady, retriesLeft = 6)
    }

    private fun loadBannerWithRetry(
        key: BannerAdKey,
        onReady: (BannerAd?) -> Unit,
        retriesLeft: Int
    ) {
        di.bannerAdsManager.loadBannerAd(key) { success ->
            if (success) {
                val ad = di.bannerAdsManager.pollBannerAd(key)
                onReady(ad)
                if (ad != null && key == BannerAdKey.ENTRANCE) di.bannerAdsManager.loadBannerAd(key)
            } else {
                // "Already in use" — preloadAll() is loading it. Poll after delay.
                if (retriesLeft > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val ad = di.bannerAdsManager.pollBannerAd(key)
                        if (ad != null) {
                            onReady(ad as BannerAd?)
                            if (key == BannerAdKey.ENTRANCE) di.bannerAdsManager.loadBannerAd(key)
                        } else {
                            loadBannerWithRetry(key, onReady, retriesLeft - 1)
                        }
                    }, 1000L)
                } else {
                    onReady(null)
                }
            }
        }
    }

    // ---- Show interstitial — blocks if app open is on screen ----
    fun showInterstitial(activity: Activity?, key: InterAdKey, listener: InterstitialShowListener) {
        if (isPremium || !isInterEnabled(key)) {
            listener.onAdFailedToShow(key.name, "Skipped")
            return
        }
        // Policy: never show interstitial while app open ad is visible
        if (isFullScreenAdShowing) {
            Log.d(tag, "Interstitial $key blocked — full-screen ad already showing")
            listener.onAdFailedToShow(key.name, "Full-screen ad already showing")
            return
        }
        isFullScreenAdShowing = true

        // Safety watchdog: if the SDK never calls back within 10s, release the flag
        // so other fragments are not permanently blocked.
        var callbackFired = false
        val watchdogHandler = Handler(Looper.getMainLooper())
        val watchdog = Runnable {
            if (!callbackFired) {
                Log.w(tag, "Interstitial $key watchdog fired — releasing isFullScreenAdShowing")
                isFullScreenAdShowing = false
                listener.onAdFailedToShow(key.name, "Watchdog timeout")
            }
        }
        watchdogHandler.postDelayed(watchdog, 10_000L)

        di.interstitialAdsManager.showInterstitialAd(
            activity,
            key,
            object : InterstitialShowListener {
                override fun onAdShown(adUnitId: String) = listener.onAdShown(adUnitId)
                override fun onAdImpression(adUnitId: String) = listener.onAdImpression(adUnitId)
                override fun onAdImpressionDelayed(adUnitId: String) =
                    listener.onAdImpressionDelayed(adUnitId)

                override fun onAdClicked(adUnitId: String) = listener.onAdClicked(adUnitId)
                override fun onAdDismissed(adUnitId: String) {
                    callbackFired = true
                    watchdogHandler.removeCallbacks(watchdog)
                    isFullScreenAdShowing = false
                    listener.onAdDismissed(adUnitId)
                }

                override fun onAdFailedToShow(adUnitId: String, reason: String) {
                    callbackFired = true
                    watchdogHandler.removeCallbacks(watchdog)
                    isFullScreenAdShowing = false
                    listener.onAdFailedToShow(adUnitId, reason)
                }
            })
    }

    fun pollNative(key: NativeAdKey): NativeAd? {
        if (isPremium || !isNativeEnabled(key)) return null
        return di.nativeAdsManager.pollNativeAd(key, null)
    }

    /**
     * Non-consuming check — returns true if a native ad for [key] is loaded and ready.
     * Does NOT remove the ad from the buffer. Safe to call before deciding to navigate.
     */
    fun isNativeReady(key: NativeAdKey): Boolean {
        if (isPremium || !isNativeEnabled(key)) return false
        return di.nativeAdsManager.isAdAvailable(key)
    }

    // ---- App Open — blocks if interstitial is on screen ----

    fun isAppOpenEnabled(): Boolean = sharedPrefs.rcAppOpen != 0 && !isPremium

    fun loadAppOpen() {
        if (isPremium || !isAppOpenEnabled()) return
        di.appOpenAdsManager.loadAd()
    }

    fun showAppOpen(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (isPremium || !isAppOpenEnabled()) {
            onComplete?.invoke(); return
        }
        if (isFullScreenAdShowing) {
            Log.d(tag, "AppOpen blocked — full-screen ad already showing")
            onComplete?.invoke()
            return
        }
        isFullScreenAdShowing = true

        // Safety watchdog: release flag if SDK never calls back within 15s
        var callbackFired = false
        val watchdogHandler = Handler(Looper.getMainLooper())
        val watchdog = Runnable {
            if (!callbackFired) {
                Log.w(tag, "AppOpen watchdog fired — releasing isFullScreenAdShowing")
                isFullScreenAdShowing = false
                onComplete?.invoke()
            }
        }
        watchdogHandler.postDelayed(watchdog, 15_000L)

        di.appOpenAdsManager.showAdIfAvailable(activity) {
            callbackFired = true
            watchdogHandler.removeCallbacks(watchdog)
            isFullScreenAdShowing = false
            onComplete?.invoke()
        }
    }

    fun onSplashComplete() {
        di.appOpenAdsManager.isSplashComplete = true
    }

    /**
     * Shows a rewarded ad, respecting the full-screen conflict guard.
     * [onLoadStart] — called on main thread when load begins (show your loading UI)
     * [onReadyToShow] — called on main thread when ad is loaded and ready; caller must call [show] to display it
     * [onFailedToLoad] — called on main thread when load fails; proceed without ad
     * [onEarnedReward] — called on main thread when user earns reward
     * [onDismissed] — called on main thread when ad is dismissed (after reward)
     * [onFailedToShow] — called on main thread when show fails; proceed without ad
     */
    fun loadAndShowRewarded(
        activity: Activity?,
        adUnitId: String,
        onLoadStart: () -> Unit,
        onReadyToShow: (show: () -> Unit) -> Unit,
        onFailedToLoad: () -> Unit,
        onEarnedReward: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: () -> Unit = onFailedToLoad
    ) {
        if (sharedPrefs.rcRewardedAiFeature == 0) {
            Log.d(tag, "Rewarded skipped — RC disabled (rcRewardedAiFeature=0)")
            onEarnedReward(); return
        }
        Log.d(
            tag,
            "Rewarded loading — adUnitId=$adUnitId rcRewardedAiFeature=${sharedPrefs.rcRewardedAiFeature}"
        )
        Handler(Looper.getMainLooper()).post { onLoadStart() }

        // RewardedAd.load() must be called on the main thread
        Handler(Looper.getMainLooper()).post {
            RewardedAd.load(
                AdRequest.Builder(adUnitId)
                    .build(),
                object :
                    AdLoadCallback<RewardedAd> {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Handler(Looper.getMainLooper()).post {
                            val act = activity
                            if (act == null || act.isFinishing || act.isDestroyed) {
                                onFailedToLoad(); return@post
                            }
                            // Give the caller a chance to dismiss any dialogs BEFORE show() is called
                            onReadyToShow {
                                doShowRewarded(ad, act, onEarnedReward, onDismissed, onFailedToShow)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(tag, "Rewarded failed to load: ${error.message}")
                        Handler(Looper.getMainLooper())
                            .post { onFailedToLoad() }
                    }
                }
            )
        }
    }

    fun loadAndShowRewardedApiVideoDownload(
        activity: Activity?,
        adUnitId: String,
        onLoadStart: () -> Unit,
        onReadyToShow: (show: () -> Unit) -> Unit,
        onFailedToLoad: () -> Unit,
        onEarnedReward: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: () -> Unit = onFailedToLoad
    ) {
        if (sharedPrefs.rcRewardedApiVideoDownload == 0) {
            Log.d(tag, "Rewarded skipped — RC disabled (rcRewardedAiFeature=0)")
            onEarnedReward(); return
        }
        Log.d(
            tag,
            "Rewarded loading — adUnitId=$adUnitId rcRewardedAiFeature=${sharedPrefs.rcRewardedApiVideoDownload}"
        )
        Handler(Looper.getMainLooper()).post { onLoadStart() }

        // RewardedAd.load() must be called on the main thread
        Handler(Looper.getMainLooper()).post {
            RewardedAd.load(
                AdRequest.Builder(adUnitId)
                    .build(),
                object :
                    AdLoadCallback<RewardedAd> {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Handler(Looper.getMainLooper()).post {
                            val act = activity
                            if (act == null || act.isFinishing || act.isDestroyed) {
                                onFailedToLoad(); return@post
                            }
                            // Give the caller a chance to dismiss any dialogs BEFORE show() is called
                            onReadyToShow {
                                doShowRewarded(ad, act, onEarnedReward, onDismissed, onFailedToShow)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(tag, "Rewarded failed to load: ${error.message}")
                        Handler(Looper.getMainLooper())
                            .post { onFailedToLoad() }
                    }
                }
            )
        }
    }

    private fun doShowRewarded(
        ad: RewardedAd,
        activity: Activity,
        onEarnedReward: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (isFullScreenAdShowing) {
            // Wait up to 3s for any current full-screen ad to clear
            var waited = 0
            val handler = Handler(Looper.getMainLooper())
            fun tryShow() {
                if (!isFullScreenAdShowing) {
                    actuallyShow(ad, activity, onEarnedReward, onDismissed, onFailed)
                } else if (waited < 3000) {
                    waited += 300
                    handler.postDelayed(::tryShow, 300)
                } else {
                    Log.w(tag, "Rewarded wait timeout — forcing show")
                    isFullScreenAdShowing = false
                    actuallyShow(ad, activity, onEarnedReward, onDismissed, onFailed)
                }
            }
            tryShow()
        } else {
            actuallyShow(ad, activity, onEarnedReward, onDismissed, onFailed)
        }
    }

    private fun actuallyShow(
        ad: RewardedAd,
        activity: Activity,
        onEarnedReward: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit
    ) {
        isFullScreenAdShowing = true
        ad.adEventCallback =
            object : RewardedAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    isFullScreenAdShowing = false
                    Handler(Looper.getMainLooper()).post { onDismissed() }
                }

                override fun onAdFailedToShowFullScreenContent(
                    error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
                ) {
                    isFullScreenAdShowing = false
                    Log.e(tag, "Rewarded failed to show: ${error.message}")
                    Handler(Looper.getMainLooper()).post { onFailed() }
                }
            }
        ad.show(activity) {
            Handler(Looper.getMainLooper()).post { onEarnedReward() }
        }
    }

    /**
     * Show an interstitial with a loading dialog.
     * Uses the same dialog_loading layout and flow as rewarded ads.
     * Dialog shows immediately; interstitial is triggered after 2 seconds minimum.
     * If no ad is available, skips the dialog entirely and calls onAdFailedToShow immediately.
     */
    fun showInterstitialWithDialog(
        activity: Activity?,
        key: InterAdKey,
        listener: InterstitialShowListener
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            listener.onAdFailedToShow(key.name, "Activity not available")
            return
        }
        if (isPremium || !isInterEnabled(key)) {
            listener.onAdFailedToShow(key.name, "Skipped")
            return
        }
        if (isFullScreenAdShowing) {
            Log.d(tag, "Interstitial $key blocked - full-screen ad already showing")
            listener.onAdFailedToShow(key.name, "Full-screen ad already showing")
            return
        }

        // Frequency cap: only show on every Nth click
        if (!shouldShowInterstitial(key)) {
            Log.d(
                tag,
                "Interstitial $key skipped — frequency cap (global click $globalInterClickCount/$INTER_SHOW_EVERY_N)"
            )
            listener.onAdFailedToShow(key.name, "Frequency cap")
            return
        }

        // If no ad is loaded, skip the dialog entirely — no point showing a spinner
        if (!di.interstitialAdsManager.isAdLoaded(key)) {
            Log.d(tag, "Interstitial $key not loaded — skipping dialog")
            // Don't count this as a shown slot — undo the increment
            //if (key !in uncappedKeys) globalInterClickCount--
            listener.onAdFailedToShow(key.name, "Ad not available")
            return
        }

        val mainHandler = Handler(Looper.getMainLooper())
/*        val dialogView = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_loading, null)
        val lottie = dialogView.findViewById<LottieAnimationView>(
            R.id.lottieLoading
        )
        lottie?.playAnimation()

        val loadingDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            .also {
                it.window?.setBackgroundDrawableResource(R.color.transparent)
                it.show()
                it.window?.setLayout(
                    activity.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._120sdp),
                    activity.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._110sdp)
                )
            }
        loadingDialog.window?.setBackgroundDrawableResource(R.color.transparent)

        fun dismissDialog() {
            mainHandler.post {
                runCatching {
                    lottie?.cancelAnimation()
                    if (loadingDialog.isShowing) loadingDialog.dismiss()
                }
            }
        }*/

        val wrappedListener = object : InterstitialShowListener {
            override fun onAdShown(adUnitId: String) {
                resetGlobalCounter()
            //    dismissDialog()
                listener.onAdShown(adUnitId)
            }

            override fun onAdImpression(adUnitId: String) = listener.onAdImpression(adUnitId)
            override fun onAdImpressionDelayed(adUnitId: String) {
                resetGlobalCounter()
               // dismissDialog()
                listener.onAdImpressionDelayed(adUnitId)
            }

            override fun onAdClicked(adUnitId: String) = listener.onAdClicked(adUnitId)
            override fun onAdDismissed(adUnitId: String) {
               // dismissDialog()
                listener.onAdDismissed(adUnitId)
            }

            override fun onAdFailedToShow(adUnitId: String, reason: String) {
             //   dismissDialog()
                listener.onAdFailedToShow(adUnitId, reason)
            }
        }

        // Wait at least 2 seconds so the dialog is always visible before the ad shows
        mainHandler.postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) {
                showInterstitial(activity, key, wrappedListener)
            } else {
                //dismissDialog()
                listener.onAdFailedToShow(key.name, "Activity destroyed")
            }
        }, 2000L)
    }

    /**
     * Same as [showInterstitialWithDialog] but skips the frequency cap.
     * Use when the ad must show on every eligible action (e.g. bookmark add).
     */
    fun showInterstitialWithDialogUncapped(
        activity: Activity?,
        key: InterAdKey,
        listener: InterstitialShowListener
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            listener.onAdFailedToShow(key.name, "Activity not available")
            return
        }
        if (isPremium || !isInterEnabled(key)) {
            listener.onAdFailedToShow(key.name, "Skipped")
            return
        }
        if (isFullScreenAdShowing) {
            Log.d(tag, "Interstitial $key blocked - full-screen ad already showing")
            listener.onAdFailedToShow(key.name, "Full-screen ad already showing")
            return
        }
        if (!di.interstitialAdsManager.isAdLoaded(key)) {
            Log.d(tag, "Interstitial $key not loaded — skipping dialog")
            listener.onAdFailedToShow(key.name, "Ad not available")
            return
        }

        val mainHandler = Handler(Looper.getMainLooper())
       /* val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)
        val lottie = dialogView.findViewById<LottieAnimationView>(R.id.lottieLoading)
        lottie?.playAnimation()

        val loadingDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            .also {
                it.window?.setBackgroundDrawableResource(R.color.transparent)
                it.show()
                it.window?.setLayout(
                    activity.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._120sdp),
                    activity.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._110sdp)
                )
            }
        loadingDialog.window?.setBackgroundDrawableResource(R.color.transparent)

        fun dismissDialog() {
            mainHandler.post {
                runCatching {
                    lottie?.cancelAnimation()
                    if (loadingDialog.isShowing) loadingDialog.dismiss()
                }
            }
        }*/

        val wrappedListener = object : InterstitialShowListener {
            override fun onAdShown(adUnitId: String) {
                //dismissDialog();
                listener.onAdShown(adUnitId)
            }

            override fun onAdImpression(adUnitId: String) = listener.onAdImpression(adUnitId)
            override fun onAdImpressionDelayed(adUnitId: String) {
               // dismissDialog();
                listener.onAdImpressionDelayed(adUnitId)
            }

            override fun onAdClicked(adUnitId: String) = listener.onAdClicked(adUnitId)
            override fun onAdDismissed(adUnitId: String) {
                //dismissDialog();
                listener.onAdDismissed(adUnitId)
            }

            override fun onAdFailedToShow(adUnitId: String, reason: String) {
                //dismissDialog();
                listener.onAdFailedToShow(adUnitId, reason)
            }
        }

        mainHandler.postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) {
                showInterstitial(activity, key, wrappedListener)
            } else {
                //dismissDialog()
                listener.onAdFailedToShow(key.name, "Activity destroyed")
            }
        }, 2000L)
    }

    // ---- Preload all (called once after RC + MobileAds are ready) ----
    // [isFirstLaunch] — true on first ever launch (onboarding will be shown).
    //
    // Strategy: only preload ads for screens the user is GUARANTEED to see next.
    // Post-GetStarted screens each load on-demand via getOrLoadNative() — this avoids
    // wasting a request on a screen the user may never visit, which hurts fill rate.
    // Also avoids the "AdUnitId is already in use" + isAdAvailable=false race that
    // occurs when we call loadNativeAd() while the SDK's persistent preloader is
    // already running and the buffer is momentarily empty.
    fun preloadAll(isFirstLaunch: Boolean = false) {
        Log.d(tag, "Preloading guaranteed-path ads — isFirstLaunch=$isFirstLaunch")
        loadAppOpen()

        if (isFirstLaunch) {
            // Onboarding path: seed LANGUAGE native for the first screen the user sees.
            // FEATURE will be seeded by GetStartedFragment before navigating to home.
            //loadNative(NativeAdKey.LANGUAGE)
        } else {
            // Returning user goes straight to home — warm up FEATURE preloader now
            // so the buffer is filled before any fragment opens and polls it.
            //loadNative(NativeAdKey.FEATURE)
        }
    }
}
