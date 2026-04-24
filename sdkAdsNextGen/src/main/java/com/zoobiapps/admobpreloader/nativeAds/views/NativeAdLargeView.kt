package com.zoobiapps.admobpreloader.nativeAds.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.zoobiapps.admobpreloader.databinding.LayoutNativeLargeBinding

class NativeAdLargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutNativeLargeBinding

    init {
        binding = LayoutNativeLargeBinding.inflate(LayoutInflater.from(context), this, true)
        // Start shimmer by default (loading state)
        binding.shimmerLayout.startShimmer()
    }

    fun setNativeAd(nativeAd: NativeAd) {
        // Stop and hide shimmer
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = GONE

        // Show ad content
        binding.adMediaView.visibility = VISIBLE
        binding.adAttribute.visibility = VISIBLE
        binding.adCallToAction.visibility = VISIBLE
        binding.adAppIcon.visibility = VISIBLE
        binding.adHeadline.visibility = VISIBLE
        binding.adBody.visibility = VISIBLE

        // Assign views to NativeAdView
        binding.nativeAdView.advertiserView = binding.adAttribute
        binding.nativeAdView.iconView = binding.adAppIcon
        binding.nativeAdView.headlineView = binding.adHeadline
        binding.nativeAdView.bodyView = binding.adBody
        binding.nativeAdView.callToActionView = binding.adCallToAction

        // Fill content
        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction
        binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)

        // Conditional visibility
        binding.adAppIcon.isVisible = nativeAd.icon?.drawable != null
        binding.adCallToAction.isVisible = nativeAd.callToAction.isNullOrEmpty().not()

        visibility = VISIBLE
        binding.nativeAdView.registerNativeAd(nativeAd, binding.adMediaView)
    }

    fun clearView() {
        // Hide ad content, show shimmer again
        binding.adMediaView.visibility = GONE
        binding.adAttribute.visibility = GONE
        binding.adCallToAction.visibility = GONE
        binding.adAppIcon.visibility = GONE
        binding.adHeadline.visibility = GONE
        binding.adBody.visibility = GONE

        binding.shimmerLayout.visibility = VISIBLE
        binding.shimmerLayout.startShimmer()
    }

    /** Call when RC disables the ad or no ad is available — collapses the entire view to zero height. */
    fun hideAd() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = GONE
        visibility = GONE
    }
}
