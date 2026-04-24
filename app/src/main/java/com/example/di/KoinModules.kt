package com.customlauncher.app.di

import com.example.di.appModule
import com.zoobiapps.admobpreloader.appOpenAds.di.appOpenAdsModule
import com.zoobiapps.admobpreloader.interstitialAds.di.interstitialAdsModule
import com.zoobiapps.admobpreloader.nativeAds.di.nativeAdsModule
import com.zoobiapps.admobpreloader.bannerAds.di.bannerAdsModule
import com.zoobiapps.core.di.coreModules

val appModules = listOf(
    coreModules,
    interstitialAdsModule,
    nativeAdsModule,
    bannerAdsModule,
    appOpenAdsModule,
    appModule
)