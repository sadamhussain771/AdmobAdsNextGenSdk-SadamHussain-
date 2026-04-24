package com.zoobiapps.admobpreloader.nativeAds.enums

/**
 * Mirrors interstitial's InterAdKey but for native placements.
 */
enum class NativeAdKey(val value: String) {
    LANGUAGE("language"),
    LANGUAGE2("language2"),
    ON_BOARDING("onBoarding"),
    ON_BOARDING2("onBoarding2"),
    ON_BOARDING3("onBoarding3"),
    GET_STARTED("getStarted"),
    DASHBOARD("dashboard"),
    FEATURE("feature"),

    // EXIT("exit"),
    FULL_SCREEN("fullScreen"),
    FULL_SCREEN2("fullScreen2")
}