package com.example.admobnextgensdk.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentSplashBinding
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Splash screen:
 *  1. Shows an adaptive banner ad immediately (BannerAdKey.SPLASH).
 *  2. Once banner is shown, starts the 2-second timer.
 *  3. Starts loading the ON_BOARDING interstitial in parallel.
 *  4. Starts preloading ON_BOARDING native ad so Onboarding1 has it ready.
 *  5. After banner shown + 2s elapsed + inter ready → show inter then navigate.
 *     If inter not ready after 2s → navigate directly (never block user).
 */
class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    // Tracks whether banner has been shown
    private var bannerShown = false
    // Tracks whether the minimum display time has elapsed
    private var minDelayElapsed = false
    // Tracks whether the interstitial load attempt has completed
    private var interLoadDone = false
    // Prevents double-navigation
    private var navigated = false

    private var bannerAd: BannerAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Load & show adaptive banner immediately
        loadSplashBanner()

        // 2. Preload ON_BOARDING interstitial (used after onboarding3)
        adManager.loadInterstitial(InterAdKey.ON_BOARDING)

        // 3. Preload ON_BOARDING native so Onboarding1 has it ready on arrival
        adManager.loadNative(NativeAdKey.ON_BOARDING)
    }

    // ---- Banner ----

    private fun loadSplashBanner() {
        adManager.getOrLoadBanner(BannerAdKey.SPLASH) { ad ->
            if (_binding == null) return@getOrLoadBanner
            if (ad == null) {
                // No banner available — start timer immediately
                bannerShown = true
                startTimerAndPoll()
                return@getOrLoadBanner
            }
            bannerAd = ad
            val activity = activity ?: return@getOrLoadBanner
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(ad.getView(activity))
            binding.bannerContainer.visibility = View.VISIBLE
            
            // Banner shown — now start the 2s timer + inter poll
            bannerShown = true
            startTimerAndPoll()
        }
    }

    private fun startTimerAndPoll() {
        // 4. Minimum 2-second splash display (starts after banner is shown)
        binding.root.postDelayed({
            minDelayElapsed = true
            maybeNavigate()
        }, 2000L)

        // 5. Poll for interstitial readiness — check every 300 ms for up to 4 seconds
        pollForInterstitial(attemptsLeft = 13)
    }

    // ---- Interstitial polling ----

    private fun pollForInterstitial(attemptsLeft: Int) {
        if (navigated || _binding == null) return

        if (adManager.isInterstitialLoaded(InterAdKey.ON_BOARDING)) {
            interLoadDone = true
            maybeNavigate()
            return
        }

        if (attemptsLeft <= 0) {
            // Give up waiting — proceed without interstitial
            interLoadDone = true
            maybeNavigate()
            return
        }

        binding.root.postDelayed({
            pollForInterstitial(attemptsLeft - 1)
        }, 300L)
    }

    // ---- Navigation gate ----

    private fun maybeNavigate() {
        if (navigated) return
        if (!bannerShown || !minDelayElapsed || !interLoadDone) return
        navigated = true

        if (adManager.isInterstitialLoaded(InterAdKey.ON_BOARDING)) {
            adManager.showInterstitial(
                activity,
                InterAdKey.ON_BOARDING,
                object : InterstitialShowListener {
                    override fun onAdDismissed(key: String) = goToOnboarding()
                    override fun onAdFailedToShow(key: String, reason: String) = goToOnboarding()
                }
            )
        } else {
            goToOnboarding()
        }
    }

    private fun goToOnboarding() {
        if (isAdded && !isDetached) {
            findNavController().navigate(R.id.action_splash_to_onboarding1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerAd = null
        _binding = null
    }
}
