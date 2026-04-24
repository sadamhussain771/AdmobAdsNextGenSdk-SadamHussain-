package com.customlauncher.app.remoteconfig

import android.util.Log
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.APP_OPEN
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.APP_OPEN_SPLASH
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_CLAP_SPLASH
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_DASHBOARD
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_ENTRANCE
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_FEATURE_1A
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_FEATURE_1B
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_FEATURE_2A
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_FEATURE_2B
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_INTRUDER_SPLASH
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.BANNER_ON_BOARDING
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.DOWNLOAD_BUTTON
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_BOOKMARKED
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_CLAP_SPLASH
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_DASHBOARD
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_INTRUDER_SERVICE
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_CLAP_SERVICE
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_DP_MAKER
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_INTRUDER_SPLASH
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_ON_BOARDING
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.INTER_SAVED
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_CLAP_DIALOG
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_EXIT
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_CLAP_HOME
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_CLAP_SETTINGS
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_CLAP_SOUNDS
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_FULL_SCREEN
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_FULL_SCREEN2
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_GET_STARTED
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_HOME
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_INTRUDER_GALLERY
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_INTRUDER_HOME
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_INTRUDER_SETTINGS
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_INTRUDER_SOUNDS
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_LANGUAGE
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_LANGUAGE2
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_ON_BOARDING
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_ON_BOARDING2
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.NATIVE_ON_BOARDING3
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.REWARDED_AI_FEATURE
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.REWARDED_API_VIDEO_DOWNLOAD
import com.customlauncher.app.remoteconfig.RemoteConfigKeys.REWARDED_INTER_AI_FEATURE
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.zoobiapps.core.storage.SharedPreferencesDataSource

class RemoteConfigManager(
    private val sharedPrefs: SharedPreferencesDataSource,
    private val isDebug: Boolean = false
) {

    private val tag = "RemoteConfigManager"
    private var configUpdateListenerRegistration: ConfigUpdateListenerRegistration? = null

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().also { rc ->
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (isDebug) 0L else 3600L)
                .build()
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(buildDefaults())
        }
    }

    /**
     * Fetch & activate Remote Config, then persist values to SharedPrefs.
     * [onComplete] is always called — true = fresh values applied, false = cached/defaults used.
     */
    fun fetchAndApply(onComplete: (success: Boolean) -> Unit = {}) {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { activated ->
                Log.d(tag, "fetchAndActivate success — new values activated: $activated")
                applyToSharedPrefs()
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(tag, "fetchAndActivate failed — using cached/default values: ${e.message}")
                // Still apply whatever is cached (could be previous fetch or in-memory defaults)
                applyToSharedPrefs()
                onComplete(false)
            }

        startRealtimeListener()
    }

    private fun startRealtimeListener() {
        // Avoid duplicate registrations
        configUpdateListenerRegistration?.remove()

        configUpdateListenerRegistration = remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(
                        tag,
                        "Real-time config update received. Updated keys: ${configUpdate.updatedKeys}"
                    )
                    // Activate the newly fetched values, then sync to SharedPrefs
                    remoteConfig.activate().addOnSuccessListener {
                        Log.d(tag, "Real-time config activated successfully")
                        applyToSharedPrefs()
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(tag, "Real-time config listener error: ${error.message}")
                }
            }
        )
    }

    fun stopRealtimeListener() {
        configUpdateListenerRegistration?.remove()
        configUpdateListenerRegistration = null
        Log.d(tag, "Real-time config listener removed")
    }

    /** Write all RC long values into SharedPrefs so ad managers pick them up. */
    private fun applyToSharedPrefs() {
        sharedPrefs.rcAppOpen = remoteConfig.getLong(APP_OPEN).toInt()
        sharedPrefs.rcAppOpenSplash = remoteConfig.getLong(APP_OPEN_SPLASH).toInt()

        sharedPrefs.rcBannerEntrance = remoteConfig.getLong(BANNER_ENTRANCE).toInt()
        sharedPrefs.rcBannerOnBoarding = remoteConfig.getLong(BANNER_ON_BOARDING).toInt()
        sharedPrefs.rcBannerDashboard = remoteConfig.getLong(BANNER_DASHBOARD).toInt()
        sharedPrefs.rcBannerFeatureOneA = remoteConfig.getLong(BANNER_FEATURE_1A).toInt()
        sharedPrefs.rcBannerFeatureOneB = remoteConfig.getLong(BANNER_FEATURE_1B).toInt()
        sharedPrefs.rcBannerFeatureTwoA = remoteConfig.getLong(BANNER_FEATURE_2A).toInt()
        sharedPrefs.rcBannerFeatureTwoB = remoteConfig.getLong(BANNER_FEATURE_2B).toInt()

        sharedPrefs.rcInterOnBoarding = remoteConfig.getLong(INTER_ON_BOARDING).toInt()
        sharedPrefs.rcInterDashboard = remoteConfig.getLong(INTER_DASHBOARD).toInt()
        sharedPrefs.rcInterSavedVideos = remoteConfig.getLong(INTER_SAVED).toInt()
        sharedPrefs.rcInterBookmarkedVideos = remoteConfig.getLong(INTER_BOOKMARKED).toInt()
        sharedPrefs.rcInterDpMakerVideos = remoteConfig.getLong(INTER_DP_MAKER).toInt()

        sharedPrefs.rcNativeLanguage = remoteConfig.getLong(NATIVE_LANGUAGE).toInt()
        sharedPrefs.rcNativeLanguage2 = remoteConfig.getLong(NATIVE_LANGUAGE2).toInt()
        sharedPrefs.rcNativeOnBoarding = remoteConfig.getLong(NATIVE_ON_BOARDING).toInt()
        sharedPrefs.rcNativeOnBoarding2 = remoteConfig.getLong(NATIVE_ON_BOARDING2).toInt()
        sharedPrefs.rcNativeOnBoarding3 = remoteConfig.getLong(NATIVE_ON_BOARDING3).toInt()
        sharedPrefs.rcNativeOnGetStarted = remoteConfig.getLong(NATIVE_GET_STARTED).toInt()
        sharedPrefs.rcNativeHome = remoteConfig.getLong(NATIVE_HOME).toInt()
        sharedPrefs.rcNativeExit = remoteConfig.getLong(NATIVE_EXIT).toInt()
        sharedPrefs.rcNativeFullScreen = remoteConfig.getLong(NATIVE_FULL_SCREEN).toInt()
        sharedPrefs.rcNativeFullScreen2 = remoteConfig.getLong(NATIVE_FULL_SCREEN2).toInt()

        sharedPrefs.rcRewardedAiFeature = remoteConfig.getLong(REWARDED_AI_FEATURE).toInt()
        sharedPrefs.rcRewardedApiVideoDownload =
            remoteConfig.getLong(REWARDED_API_VIDEO_DOWNLOAD).toInt()
        sharedPrefs.rcRewardedInterAiFeature =
            remoteConfig.getLong(REWARDED_INTER_AI_FEATURE).toInt()

        sharedPrefs.rcDownloadButton = remoteConfig.getLong(DOWNLOAD_BUTTON).toInt()

        Log.d(tag, "Remote config applied to SharedPrefs")
    }

    /**
     * In-memory defaults — all ads enabled (1).
     * These are used immediately on first launch before any fetch completes,
     * and as fallback if fetch fails and no cached values exist.
     */
    private fun buildDefaults(): Map<String, Any> = mapOf(
        APP_OPEN to 0L,   // App Open off by default (common practice)
        APP_OPEN_SPLASH to 1L,

        BANNER_ENTRANCE to 1L,
        BANNER_CLAP_SPLASH to 1L,
        BANNER_INTRUDER_SPLASH to 1L,
        BANNER_ON_BOARDING to 1L,
        BANNER_DASHBOARD to 1L,
        BANNER_FEATURE_1A to 1L,
        BANNER_FEATURE_1B to 1L,
        BANNER_FEATURE_2A to 1L,
        BANNER_FEATURE_2B to 1L,

        INTER_CLAP_SPLASH to 1L,
        INTER_INTRUDER_SPLASH to 1L,
        INTER_ON_BOARDING to 1L,
        INTER_DASHBOARD to 1L,
        INTER_SAVED to 1L,
        INTER_INTRUDER_SERVICE to 1L,
        INTER_CLAP_SERVICE to 1L,
        INTER_BOOKMARKED to 1L,
        INTER_DP_MAKER to 1L,

        NATIVE_LANGUAGE to 1L,
        NATIVE_LANGUAGE2 to 1L,
        NATIVE_ON_BOARDING to 1L,
        NATIVE_ON_BOARDING2 to 1L,
        NATIVE_ON_BOARDING3 to 1L,
        NATIVE_GET_STARTED to 1L,
        NATIVE_HOME to 1L,
        NATIVE_CLAP_HOME to 1L,
        NATIVE_CLAP_DIALOG to 1L,
        NATIVE_INTRUDER_HOME to 1L,
        NATIVE_INTRUDER_SOUNDS to 1L,
        NATIVE_CLAP_SOUNDS to 1L,
        NATIVE_CLAP_SETTINGS to 1L,
        NATIVE_INTRUDER_SETTINGS to 1L,
        NATIVE_INTRUDER_GALLERY to 1L,
        NATIVE_EXIT to 1L,
        NATIVE_FULL_SCREEN to 1L,
        NATIVE_FULL_SCREEN2 to 1L,

        REWARDED_AI_FEATURE to 1L,
        REWARDED_API_VIDEO_DOWNLOAD to 1L,
        REWARDED_INTER_AI_FEATURE to 1L,

        DOWNLOAD_BUTTON to 1L,
    )
}
