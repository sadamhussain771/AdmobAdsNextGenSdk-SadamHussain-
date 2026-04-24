package com.zoobiapps.admobpreloader.nativeAds.model

/**
 * Runtime info for a native placement, stored in the registry.
 */
data class AdInfo(
    val adUnitId: String,
    val canShare: Boolean,
    val canReuse: Boolean,
    val bufferSize: Int?
)