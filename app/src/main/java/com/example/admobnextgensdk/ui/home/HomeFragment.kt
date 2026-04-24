package com.example.admobnextgensdk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.FragmentHomeBinding
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdKey
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Home screen:
 *  - Loads and shows ENTRANCE adaptive banner at the bottom.
 *  - Loads DASHBOARD native ad (small) and shows it above the buttons.
 *  - Preloads DASHBOARD interstitial for use when navigating to sub-fragments.
 *  - 3 buttons navigate to HomeFragment1/2/3.
 */
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var bannerAd: BannerAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load ENTRANCE adaptive banner
        loadBanner()

        // Load DASHBOARD native ad on-demand
        loadNativeAd()

        // Preload DASHBOARD interstitial for sub-fragment navigation
        adManager.loadInterstitial(InterAdKey.DASHBOARD)

        binding.btnHome1.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_home1)
        }
        binding.btnHome2.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_home2)
        }
        binding.btnHome3.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_home3)
        }
    }

    private fun loadBanner() {
        adManager.getOrLoadBanner(BannerAdKey.ENTRANCE) { ad ->
            if (_binding == null || ad == null) return@getOrLoadBanner
            bannerAd = ad
            val activity = activity ?: return@getOrLoadBanner
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(ad.getView(activity))
            binding.bannerContainer.visibility = View.VISIBLE
        }
    }

    private fun loadNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.DASHBOARD)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.DASHBOARD) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            binding.nativeAdContainer.visibility = View.VISIBLE
            binding.nativeAdView.setNativeAd(nativeAd)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerAd = null
        _binding = null
    }
}
