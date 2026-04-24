package com.example.admobnextgensdk.ui.boarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentOnboarding3Binding
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Onboarding screen 3:
 *  - Shows shimmer immediately while ON_BOARDING3 native loads.
 *  - Hides container if ad unavailable.
 *  - On "Get Started": shows ON_BOARDING interstitial then navigates to Home.
 */
class Onboarding3Fragment : BaseFragment() {

    private var _binding: FragmentOnboarding3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show shimmer immediately — container is visible by default in XML
        showNativeAd()

        binding.btnGetStarted.setOnClickListener {
            binding.btnGetStarted.isEnabled = false
            showInterAndNavigate()
        }
    }

    private fun showNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.ON_BOARDING3)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.ON_BOARDING3) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            // Stop shimmer, show the actual ad view
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
            binding.nativeAdView.visibility = View.VISIBLE
            binding.nativeAdView.setNativeAd(nativeAd)
        }
    }

    private fun showInterAndNavigate() {
        if (adManager.isInterstitialLoaded(InterAdKey.ON_BOARDING)) {
            adManager.showInterstitial(
                activity,
                InterAdKey.ON_BOARDING,
                object : InterstitialShowListener {
                    override fun onAdDismissed(key: String) = goToHome()
                    override fun onAdFailedToShow(key: String, reason: String) = goToHome()
                }
            )
        } else {
            goToHome()
        }
    }

    private fun goToHome() {
        if (isAdded && !isDetached) {
            findNavController().navigate(R.id.action_onboarding3_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
