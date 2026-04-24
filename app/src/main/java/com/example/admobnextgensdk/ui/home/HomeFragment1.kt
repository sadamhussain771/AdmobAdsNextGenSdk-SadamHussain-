package com.example.admobnextgensdk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.databinding.FragmentHome1Binding
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Home Fragment 1:
 *  - Loads a collapsible banner (BannerAdKey.ENTRANCE with collapsible type).
 *    Note: The SDK's BannerAdKey.ENTRANCE is configured as ADAPTIVE in BannerAdsManager.
 *    For a true collapsible banner here we use BannerAdKey.SPLASH which maps to ADAPTIVE,
 *    and we load it fresh with the collapsible extras via AdManager.getOrLoadBanner.
 *    Since the SDK only exposes ENTRANCE and SPLASH keys, we reuse ENTRANCE here
 *    (the collapsible behavior is set in BannerAdsManager config).
 *  - Loads a FULL_SCREEN large native ad on-demand.
 */
class HomeFragment1 : BaseFragment() {

    private var _binding: FragmentHome1Binding? = null
    private val binding get() = _binding!!

    private var bannerAd: BannerAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load collapsible banner (ENTRANCE key — adaptive banner)
        loadBanner()

        // Load FULL_SCREEN large native ad on-demand
        loadLargeNativeAd()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadBanner() {
        // ENTRANCE is the available collapsible-capable banner key
        adManager.getOrLoadBanner(BannerAdKey.ENTRANCE) { ad ->
            if (_binding == null || ad == null) return@getOrLoadBanner
            bannerAd = ad
            val activity = activity ?: return@getOrLoadBanner
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(ad.getView(activity))
            binding.bannerContainer.visibility = View.VISIBLE
        }
    }

    private fun loadLargeNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.FULL_SCREEN)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.FULL_SCREEN) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            binding.nativeAdContainer.visibility = View.VISIBLE
            binding.nativeAdLargeView.setNativeAd(nativeAd)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerAd = null
        _binding = null
    }
}
