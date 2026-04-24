package com.example.di

import com.example.ads.AdManager
import com.customlauncher.app.billing.BillingManager
import com.customlauncher.app.billing.PremiumManager
import com.customlauncher.app.di.DIComponent
import com.customlauncher.app.remoteconfig.RemoteConfigManager
import com.example.admobnextgensdk.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { RemoteConfigManager(sharedPrefs = get(), isDebug = BuildConfig.DEBUG) }
    single { PremiumManager(androidContext()) }
    single { BillingManager(androidContext(), get()) }
    single { AdManager(sharedPrefs = get(), di = DIComponent()) }
}