package com.customlauncher.app.di

import com.example.ads.AdManager
import com.customlauncher.app.billing.BillingManager
import com.customlauncher.app.billing.PremiumManager
import com.customlauncher.app.remoteconfig.RemoteConfigManager
import com.zoobiapps.admobpreloader.appOpenAds.AppOpenAdsManager
import com.zoobiapps.admobpreloader.bannerAds.BannerAdsManager
import com.zoobiapps.admobpreloader.interstitialAds.InterstitialAdsManager
import com.zoobiapps.admobpreloader.nativeAds.NativeAdsManager
import com.zoobiapps.core.storage.SharedPreferencesDataSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DIComponent : KoinComponent {
    val interstitialAdsManager by inject<InterstitialAdsManager>()
    val nativeAdsManager by inject<NativeAdsManager>()
    val bannerAdsManager by inject<BannerAdsManager>()
    val appOpenAdsManager by inject<AppOpenAdsManager>()
    val sharedPrefs by inject<SharedPreferencesDataSource>()
    val adManager by inject<AdManager>()
    val billingManager by inject<BillingManager>()
    val premiumManager by inject<PremiumManager>()
    val remoteConfigManager by inject<RemoteConfigManager>()
}
