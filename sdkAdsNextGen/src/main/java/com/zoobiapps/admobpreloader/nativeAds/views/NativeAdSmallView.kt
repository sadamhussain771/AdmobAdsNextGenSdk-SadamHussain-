package com.zoobiapps.admobpreloader.nativeAds.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.zoobiapps.admobpreloader.databinding.LayoutNativeSmallBinding

class NativeAdSmallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutNativeSmallBinding =
        LayoutNativeSmallBinding.inflate(LayoutInflater.from(context), this, true)

    private val nativeAdView: NativeAdView = binding.nativeAdViewRoot

    fun setNativeAd(nativeAd: NativeAd) {
/*        binding.adMedia.visibility = VISIBLE
        binding.adAttribution.visibility = VISIBLE
        binding.adAppIcon.visibility = VISIBLE
        binding.adHeadline.visibility = VISIBLE
        binding.adBody.visibility = VISIBLE
        binding.adCallToAction.visibility = VISIBLE*/

        // Register ALL asset views — advertiserView must be registered to pass validator
        nativeAdView.advertiserView = binding.adAttribution
        nativeAdView.iconView = binding.adAppIcon
        nativeAdView.headlineView = binding.adHeadline
        nativeAdView.bodyView = binding.adBody
        nativeAdView.callToActionView = binding.adCallToAction

        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction
        binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
       /* binding.adAttribution.text = nativeAd.advertiser ?: context.getString(
            com.zoobiapps.admobpreloader.R.string.ad
        )*/

        binding.adAppIcon.isVisible = nativeAd.icon?.drawable != null
        binding.adCallToAction.isVisible = !nativeAd.callToAction.isNullOrEmpty()

        val starRating = nativeAd.starRating
        if (starRating != null && starRating > 0) {
            binding.ratingbar.rating = starRating.toFloat()
            binding.ratingbar.visibility = VISIBLE
        } else {
            binding.ratingbar.visibility = GONE
        }

        visibility = VISIBLE
        nativeAdView.registerNativeAd(nativeAd, binding.adMedia)
    }

    fun clearView() {
        binding.adMedia.visibility = GONE
        binding.adAttribution.visibility = GONE
        binding.adCallToAction.visibility = GONE
        binding.adAppIcon.visibility = GONE
        binding.adHeadline.visibility = GONE
        binding.adBody.visibility = GONE
    }

    fun hideAd() {
        visibility = GONE
    }
}
