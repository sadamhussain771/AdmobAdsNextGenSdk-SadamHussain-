package com.zoobiapps.admobpreloader.bannerAds.model

import com.zoobiapps.admobpreloader.bannerAds.enums.BannerAdType

/**
 * Static configuration for a banner placement.
 *
 *  - bufferSize: null => single-shot; >0 => let SDK buffer that many ads (if/when supported).
 *  - canShare / canReuse: reserved for potential cross-key reuse policies (mirrors native/inter).
 */
data class AdConfig(
    val adUnitId: String,
    val isRemoteEnabled: Boolean,
    val bannerAdType: BannerAdType,
    val bufferSize: Int?,       // null => single-shot; >0 => pass to Preloader
    val canShare: Boolean,      // allow other screens to show it
    val canReuse: Boolean       // allow reuse (i.e., use other ads if this not available)
)