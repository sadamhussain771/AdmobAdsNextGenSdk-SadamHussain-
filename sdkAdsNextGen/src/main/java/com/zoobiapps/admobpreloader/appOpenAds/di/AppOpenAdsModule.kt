package com.zoobiapps.admobpreloader.appOpenAds.di

import com.zoobiapps.admobpreloader.appOpenAds.AppOpenAdsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appOpenAdsModule = module {
    single {
        AppOpenAdsManager(
            resources = androidContext().resources,
            sharedPrefs = get()
        )
    }
}
