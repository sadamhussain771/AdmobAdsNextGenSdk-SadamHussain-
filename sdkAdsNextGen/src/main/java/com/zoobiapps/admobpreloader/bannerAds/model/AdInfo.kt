package com.zoobiapps.admobpreloader.bannerAds.model

import android.os.Bundle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize

/**
 * Runtime info for a banner placement, stored in the registry.
 */
data class AdInfo(
    val adUnitId: String,
    val canShare: Boolean,
    val canReuse: Boolean,
    val bufferSize: Int?,
    val adSize: AdSize?,      // null => use default BANNER; non-null => use this size (e.g., adaptive / MREC)
    val extras: Bundle?       // optional Google extras (e.g., collapsible top/bottom)
)