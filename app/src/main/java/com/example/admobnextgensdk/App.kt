package com.example.admobnextgensdk

import android.app.Application
import com.customlauncher.app.di.appModules
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.concurrent.Executors

/**
 * Application class — the single initialization point for:
 *  1. Koin DI (all modules)
 *  2. Google Mobile Ads Next Gen SDK (MobileAds.initialize)
 *
 * MobileAds.initialize is @WorkerThread — run it on a background thread.
 * Consent is handled in MainActivity before any ad is requested.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Start Koin — must be first so all managers are available
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModules)
        }

        // 2. Initialize the AdMob Next Gen SDK on a background thread (@WorkerThread)
        Executors.newSingleThreadExecutor().execute {
            val config = InitializationConfig.Builder(
                getString(R.string.admob_app_id)
            ).build()
            MobileAds.initialize(this, config) { }
        }
    }
}
