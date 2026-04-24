package com.example.admobnextgensdk.ui.boarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentOnboarding1Binding
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Onboarding screen 1:
 *  - Shows shimmer immediately while ON_BOARDING native loads.
 *  - Hides container if ad unavailable (RC off / no internet).
 *  - Preloads ON_BOARDING2 native for Onboarding2.
 *  - Preloads FULL_SCREEN native for the FullScreenNativeFragment (between Onboarding2 and 3).
 */
class Onboarding1Fragment : BaseFragment() {

    private var _binding: FragmentOnboarding1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show shimmer immediately — container is visible by default in XML
        showNativeAd()

        // Preload ON_BOARDING2 native for Onboarding2
        adManager.loadNative(NativeAdKey.ON_BOARDING2)

        // Preload FULL_SCREEN native for the full-screen native fragment after Onboarding2
        adManager.loadNative(NativeAdKey.FULL_SCREEN)

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding1_to_onboarding2)
        }
    }

    private fun showNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.ON_BOARDING)) {
            // RC disabled or premium — hide container (no shimmer needed)
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        // Container is already visible (shimmer running) — wait for ad
        adManager.getOrLoadNative(NativeAdKey.ON_BOARDING) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                // No fill — collapse container
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
