package com.zoobiapps.admobpreloader.interstitialAds.di

import com.zoobiapps.admobpreloader.interstitialAds.InterstitialAdsManager
import com.zoobiapps.admobpreloader.interstitialAds.engine.PreloadEngine
import com.zoobiapps.admobpreloader.interstitialAds.engine.ShowEngine
import com.zoobiapps.admobpreloader.interstitialAds.storage.AdRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for InterstitialAdsManager and its dependencies.
 */
val interstitialAdsModule = module {
    single { AdRegistry() }
    single { PreloadEngine(get()) }
    single { ShowEngine(get(), get()) }
    single {
        InterstitialAdsManager(
            resources = androidContext().resources,
            registry = get(),
            preloadEngine = get(),
            showEngine = get(),
            internetManager = get(),
            sharedPrefs = get(),
        )
    }
}