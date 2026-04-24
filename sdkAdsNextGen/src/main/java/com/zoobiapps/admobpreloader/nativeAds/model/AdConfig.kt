package com.zoobiapps.admobpreloader.nativeAds.model

/**
 * Static configuration for a native placement.
 *
 * AdConfig:
 *  - bufferSize: null => single-shot; >0 => let SDK buffer that many ads.
 *  - canShare / canReuse: reserved for future cross-key reuse policies.
 */
data class AdConfig(
    val adUnitId: String,
    val isRemoteEnabled: Boolean,
    val bufferSize: Int?,       // null => single-shot; >0 => pass to Preloader
    val canShare: Boolean,      // allow other screens to show it
    val canReuse: Boolean       // allow reuse (i.e., use other ads if this not available)
)