package com.zoobiapps.core.di

import android.content.Context
import android.net.ConnectivityManager
import com.zoobiapps.core.network.InternetManager
import com.zoobiapps.core.storage.SharedPreferencesDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModules = module {
    single { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    single { androidContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE) }

    single { InternetManager(get()) }
    single { SharedPreferencesDataSource(get()) }
}