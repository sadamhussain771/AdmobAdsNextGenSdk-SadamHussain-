package com.zoobiapps.admobpreloader.bannerAds.di

import com.zoobiapps.admobpreloader.bannerAds.BannerAdsManager
import com.zoobiapps.admobpreloader.bannerAds.engine.PreloadEngine
import com.zoobiapps.admobpreloader.bannerAds.engine.ShowEngine
import com.zoobiapps.admobpreloader.bannerAds.storage.AdRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for BannerAdsManager and its dependencies.
 */
val bannerAdsModule = module {
    single { AdRegistry() }
    single { PreloadEngine(get()) }
    single { ShowEngine(get(), get()) }
    single {
        BannerAdsManager(
            context = androidContext(),
            registry = get(),
            preloadEngine = get(),
            showEngine = get(),
            internetManager = get(),
            sharedPrefs = get()
        )
    }
}