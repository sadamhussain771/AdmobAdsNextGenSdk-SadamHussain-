package com.example.admobnextgensdk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.admobnextgensdk.utils.BaseFragment
import com.example.admobnextgensdk.databinding.FragmentHome2Binding
import com.zoobiapps.admobpreloader.interstitialAds.callbacks.InterstitialShowListener
import com.zoobiapps.admobpreloader.interstitialAds.enums.InterAdKey
import com.zoobiapps.admobpreloader.nativeAds.enums.NativeAdKey

/**
 * Home Fragment 2:
 *  - Loads SAVED_VIDEOS interstitial on entry (for use on back/action).
 *  - Loads FULL_SCREEN2 large native ad on-demand.
 *  - Shows interstitial on back button press, then pops back stack.
 */
class HomeFragment2 : BaseFragment() {

    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preload SAVED_VIDEOS interstitial for this fragment
        adManager.loadInterstitial(InterAdKey.SAVED_VIDEOS)

        // Load FULL_SCREEN2 large native on-demand
        loadLargeNativeAd()

        binding.btnBack.setOnClickListener {
            binding.btnBack.isEnabled = false
            showInterAndGoBack()
        }
    }

    private fun loadLargeNativeAd() {
        if (!adManager.shouldShowNative(NativeAdKey.FULL_SCREEN2)) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }
        adManager.getOrLoadNative(NativeAdKey.FULL_SCREEN2) { nativeAd ->
            if (_binding == null) return@getOrLoadNative
            if (nativeAd == null) {
                binding.nativeAdContainer.visibility = View.GONE
                return@getOrLoadNative
            }
            binding.nativeAdContainer.visibility = View.VISIBLE
            binding.nativeAdLargeView.setNativeAd(nativeAd)
        }
    }

    private fun showInterAndGoBack() {
        if (adManager.isInterstitialLoaded(InterAdKey.SAVED_VIDEOS)) {
            adManager.showInterstitial(
                activity,
                InterAdKey.SAVED_VIDEOS,
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
