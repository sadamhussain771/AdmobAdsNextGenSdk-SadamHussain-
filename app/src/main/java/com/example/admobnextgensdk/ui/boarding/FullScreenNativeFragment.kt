package com.example.admobnextgensdk.ui.boarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentFullscreenNativeBinding
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Full-screen native ad fragment shown between Onboarding2 and Onboarding3.
 *
 * Displays the FULL_SCREEN large native ad (preloaded in Onboarding1).
 *
 * Key fix: Onboarding2 uses isNativeReady() (non-consuming) to decide whether to
 * navigate here. This fragment then calls getOrLoadNative() which polls the ad
 * exactly once — the buffer is intact when we arrive.
 *
 * If the ad is unavailable (RC off, no internet, no fill), navigates to Onboarding3.
 */
class FullScreenNativeFragment : BaseFragment() {

    private var _binding: FragmentFullscreenNativeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullscreenNativeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showFullScreenNative()

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_fullscreen_native_to_onboarding3)
        }
    }

    private fun showFullScreenNative() {
        if (!adManager.shouldShowNative(NativeAdKey.FULL_SCREEN)) {
            goToOnboarding3()
            return
        }

        // getOrLoadNative polls the preloaded buffer first (single consume),
        // then falls back to loading if not yet ready.
        // Onboarding2 used isNativeReady() (non-consuming) so the buffer is intact here.
        adManager.getOrLoadNative(NativeAdKey.FULL_SCREEN) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                goToOnboarding3()
                return@getOrLoadNative
            }
            // NativeAdLargeView handles shimmer → ad transition internally
            binding.nativeAdLargeView.setNativeAd(nativeAd)
        }
    }

    private fun goToOnboarding3() {
        if (isAdded && !isDetached) {
            findNavController().navigate(R.id.action_fullscreen_native_to_onboarding3)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
