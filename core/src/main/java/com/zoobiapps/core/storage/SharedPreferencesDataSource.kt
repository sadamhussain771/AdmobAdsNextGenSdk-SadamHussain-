package com.zoobiapps.core.storage

import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesDataSource(private val sharedPreferences: SharedPreferences) {

    private val billingRequireKey = "isAppPurchased"
    private val isShowFirstScreenKey = "showFirstScreen"

    /**
     *  ------------------- Billing -------------------
     */
    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(billingRequireKey, false)
        set(value) = sharedPreferences.edit { putBoolean(billingRequireKey, value) }

    /**
     *  ------------------- UI -------------------
     */
    var showFirstScreen: Boolean
        get() = sharedPreferences.getBoolean(isShowFirstScreenKey, true)
        set(value) = sharedPreferences.edit { putBoolean(isShowFirstScreenKey, value) }

    /* ---------------------------------------- Ads ---------------------------------------- */
    val appOpen = "appOpen"
    val appOpenSplash = "appOpenSplash"

    val bannerEntrance = "bannerEntrance"
    val bannerSplash = "bannerSplash"
    val bannerOnBoarding = "bannerOnBoarding"
    val bannerDashboard = "bannerDashboard"
    val bannerFeatureOneA = "bannerFeatureOneA"
    val bannerFeatureOneB = "bannerFeatureOneB"
    val bannerFeatureTwoA = "bannerFeatureTwoA"
    val bannerFeatureTwoB = "bannerFeatureTwoB"

    val interEntrance = "interEntrance"
    val interOnBoarding = "interOnBoarding"
    val interDashboard = "interDashboard"
    val interSavedVideos = "interSavedVideos"
    val interDownloaderVideos = "interDownloaderVideos"
    val interBookmarkedVideos = "interBookmarkedVideos"
    val interDpMaker = "interDpMaker"
    val interBottomNavigation = "interBottomNavigation"
    val interBackPress = "interBackPress"
    val interExit = "interExit"

    val nativeLanguage = "nativeLanguage"
    val nativeLanguage2 = "nativeLanguage2"
    val nativeOnBoarding = "nativeOnBoarding"
    val nativeOnBoarding2 = "nativeOnBoarding2"
    val nativeOnBoarding3 = "nativeOnBoarding3"
    val nativeOnGetStarted = "nativeGetStarted"
    val nativeFeature = "nativeFeature"
    val nativeHome = "nativeHome"
    val nativeExit = "nativeExit"
    val nativeFullScreen = "nativeFullScreen"
    val nativeFullScreen2 = "nativeFullScreen2"

    val rewardedAiFeature = "rewardedVideoDownload"
    val rcRewardedApiVideoFeature = "rewardedApiVideoDownload"
    val rewardedInterAiFeature = "rewardedInterAiFeature"

    /**
     *  ------------------- AppOpen Ads -------------------
     */
    var rcAppOpen: Int
        get() = sharedPreferences.getInt(appOpen, 0)
        set(value) = sharedPreferences.edit { putInt(appOpen, value) }

    var rcAppOpenSplash: Int
        get() = sharedPreferences.getInt(appOpenSplash, 1)
        set(value) = sharedPreferences.edit { putInt(appOpenSplash, value) }

    /**
     *  ------------------- Banner Ads -------------------
     */
    var rcBannerEntrance: Int
        get() = sharedPreferences.getInt(bannerEntrance, 1)
        set(value) = sharedPreferences.edit { putInt(bannerEntrance, value) }

    var rcBannerSplash: Int
        get() = sharedPreferences.getInt(bannerSplash, 1)
        set(value) = sharedPreferences.edit { putInt(bannerSplash, value) }

    var rcBannerOnBoarding: Int
        get() = sharedPreferences.getInt(bannerOnBoarding, 1)
        set(value) = sharedPreferences.edit { putInt(bannerOnBoarding, value) }

    var rcBannerDashboard: Int
        get() = sharedPreferences.getInt(bannerDashboard, 1)
        set(value) = sharedPreferences.edit { putInt(bannerDashboard, value) }

    var rcBannerFeatureOneA: Int
        get() = sharedPreferences.getInt(bannerFeatureOneA, 1)
        set(value) = sharedPreferences.edit { putInt(bannerFeatureOneA, value) }

    var rcBannerFeatureOneB: Int
        get() = sharedPreferences.getInt(bannerFeatureOneB, 1)
        set(value) = sharedPreferences.edit { putInt(bannerFeatureOneB, value) }

    var rcBannerFeatureTwoA: Int
        get() = sharedPreferences.getInt(bannerFeatureTwoA, 1)
        set(value) = sharedPreferences.edit { putInt(bannerFeatureTwoA, value) }

    var rcBannerFeatureTwoB: Int
        get() = sharedPreferences.getInt(bannerFeatureTwoB, 1)
        set(value) = sharedPreferences.edit { putInt(bannerFeatureTwoB, value) }

    /**
     *  ------------------- Interstitial Ads -------------------
     */
    var rcInterEntrance: Int
        get() = sharedPreferences.getInt(interEntrance, 1)
        set(value) = sharedPreferences.edit { putInt(interEntrance, value) }

    var rcInterOnBoarding: Int
        get() = sharedPreferences.getInt(interOnBoarding, 1)
        set(value) = sharedPreferences.edit { putInt(interOnBoarding, value) }

    var rcInterDashboard: Int
        get() = sharedPreferences.getInt(interDashboard, 1)
        set(value) = sharedPreferences.edit { putInt(interDashboard, value) }

    var rcInterSavedVideos: Int
        get() = sharedPreferences.getInt(interSavedVideos, 1)
        set(value) = sharedPreferences.edit { putInt(interSavedVideos, value) }

    var rcInterDownloaderVideos: Int
        get() = sharedPreferences.getInt(interDownloaderVideos, 1)
        set(value) = sharedPreferences.edit { putInt(interDownloaderVideos, value) }

    var rcInterBookmarkedVideos: Int
        get() = sharedPreferences.getInt(interBookmarkedVideos, 1)
        set(value) = sharedPreferences.edit { putInt(interBookmarkedVideos, value) }

    var rcInterDpMakerVideos: Int
        get() = sharedPreferences.getInt(interDpMaker, 1)
        set(value) = sharedPreferences.edit { putInt(interDpMaker, value) }

    /**
     *  ------------------- Native Ads -------------------
     */
    var rcNativeLanguage: Int
        get() = sharedPreferences.getInt(nativeLanguage, 1)
        set(value) = sharedPreferences.edit { putInt(nativeLanguage, value) }

    var rcNativeLanguage2: Int
        get() = sharedPreferences.getInt(nativeLanguage2, 1)
        set(value) = sharedPreferences.edit { putInt(nativeLanguage2, value) }

    var rcNativeOnBoarding: Int
        get() = sharedPreferences.getInt(nativeOnBoarding, 1)
        set(value) = sharedPreferences.edit { putInt(nativeOnBoarding, value) }

    var rcNativeOnBoarding2: Int
        get() = sharedPreferences.getInt(nativeOnBoarding2, 1)
        set(value) = sharedPreferences.edit { putInt(nativeOnBoarding2, value) }

    var rcNativeOnBoarding3: Int
        get() = sharedPreferences.getInt(nativeOnBoarding3, 1)
        set(value) = sharedPreferences.edit { putInt(nativeOnBoarding3, value) }

    var rcNativeOnGetStarted: Int
        get() = sharedPreferences.getInt(nativeOnGetStarted, 1)
        set(value) = sharedPreferences.edit { putInt(nativeOnGetStarted, value) }

    var rcNativeHome: Int
        get() = sharedPreferences.getInt(nativeHome, 1)
        set(value) = sharedPreferences.edit { putInt(nativeHome, value) }

    var rcNativeFeature: Int
        get() = sharedPreferences.getInt(nativeFeature, 1)
        set(value) = sharedPreferences.edit { putInt(nativeFeature, value) }

    var rcNativeExit: Int
        get() = sharedPreferences.getInt(nativeExit, 1)
        set(value) = sharedPreferences.edit { putInt(nativeExit, value) }

    var rcNativeFullScreen: Int
        get() = sharedPreferences.getInt(nativeFullScreen, 1)
        set(value) = sharedPreferences.edit { putInt(nativeFullScreen, value) }

    var rcNativeFullScreen2: Int
        get() = sharedPreferences.getInt(nativeFullScreen2, 1)
        set(value) = sharedPreferences.edit { putInt(nativeFullScreen2, value) }

    /**
     *  ------------------- Rewarded Ads -------------------
     */
    var rcRewardedAiFeature: Int
        get() = sharedPreferences.getInt(rewardedAiFeature, 1)
        set(value) = sharedPreferences.edit { putInt(rewardedAiFeature, value) }

    var rcRewardedApiVideoDownload: Int
        get() = sharedPreferences.getInt(rcRewardedApiVideoFeature, 1)
        set(value) = sharedPreferences.edit { putInt(rcRewardedApiVideoFeature, value) }

    var rcRewardedInterAiFeature: Int
        get() = sharedPreferences.getInt(rewardedInterAiFeature, 1)
        set(value) = sharedPreferences.edit { putInt(rewardedInterAiFeature, value) }

    /* ---------------------------------------- Features ---------------------------------------- */

    var rcDownloadButton: Int
        get() = sharedPreferences.getInt("downloadButton", 1)
        set(value) = sharedPreferences.edit { putInt("downloadButton", value) }
}