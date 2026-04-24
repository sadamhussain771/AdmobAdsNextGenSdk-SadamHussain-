package com.example.admobnextgensdk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.databinding.FragmentHome3Binding
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Home Fragment 3:
 *  - Loads BOOKMARK_VIDEOS interstitial on entry.
 *  - Loads FEATURE native ad on-demand (small native, buffered).
 *  - Shows interstitial on back button press, then pops back stack.
 */
class HomeFragment3 : BaseFragment() {

    private var _binding: FragmentHome3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preload BOOKMARK_VIDEOS interstitial for this fragment
        adManager.loadInterstitial(InterAdKey.BOOKMARK_VIDEOS)

        // Load FEATURE native ad on-demand (small)
        loadNativeAd()

        binding.btnBack.setOnClickListener {
            binding.btnBack.isEnabled = false
            showInterAndGoBack()
        }
    }

    private fun loadNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.FEATURE)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.FEATURE) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            binding.nativeAdContainer.visibility = View.VISIBLE
            binding.nativeAdView.setNativeAd(nativeAd)
        }
    }

    private fun showInterAndGoBack() {
        if (adManager.isInterstitialLoaded(InterAdKey.BOOKMARK_VIDEOS)) {
            adManager.showInterstitial(
                activity,
                InterAdKey.BOOKMARK_VIDEOS,
                object : InterstitialShowListener {
                    override fun onAdDismissed(key: String) = popBack()
                    override fun onAdFailedToShow(key: String, reason: String) = popBack()
                }
            )
        } else {
            popBack()
        }
    }

    private fun popBack() {
        if (isAdded && !isDetached) {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
