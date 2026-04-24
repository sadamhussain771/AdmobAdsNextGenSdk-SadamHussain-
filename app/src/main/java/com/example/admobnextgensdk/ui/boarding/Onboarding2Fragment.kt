package com.example.admobnextgensdk.ui.boarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentOnboarding2Binding
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Onboarding screen 2:
 *  - Shows shimmer immediately while ON_BOARDING2 large native loads.
 *  - Hides container if ad unavailable.
 *  - Preloads ON_BOARDING3 native for Onboarding3.
 *  - Next button: navigates to FullScreenNativeFragment if FULL_SCREEN native is ready,
 *    otherwise skips directly to Onboarding3.
 */
class Onboarding2Fragment : BaseFragment() {

    private var _binding: FragmentOnboarding2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show shimmer immediately — container is visible by default in XML
        showNativeAd()

        // Preload ON_BOARDING3 native for Onboarding3
        adManager.loadNative(NativeAdKey.ON_BOARDING3)

        binding.btnNext.setOnClickListener {
            navigateNext()
        }
    }

    private fun showNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.ON_BOARDING2)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.ON_BOARDING2) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            // NativeAdLargeView.setNativeAd() stops shimmer and shows content
            binding.nativeAdLargeView.setNativeAd(nativeAd)
        }
    }

    private fun navigateNext() {
        // Check availability WITHOUT consuming the ad — FullScreenNativeFragment will poll it
        if (adManager.shouldShowNative(NativeAdKey.FULL_SCREEN) &&
            adManager.isNativeReady(NativeAdKey.FULL_SCREEN)
        ) {
            findNavController().navigate(R.id.action_onboarding2_to_fullscreen_native)
        } else {
            findNavController().navigate(R.id.action_onboarding2_to_onboarding3)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
