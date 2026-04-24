package com.example.admobnextgensdk.ui.container

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.customlauncher.app.ads.ConsentManager
import com.example.admobnextgensdk.BuildConfig
import com.example.admobnextgensdk.R
import com.example.admobnextgensdk.databinding.ActivityMainBinding

/**
 * Single activity host.
 *
 * Responsibilities:
 *  1. Request UMP consent on first launch (before any ad is shown).
 *  2. Host the NavHostFragment for the full navigation graph.
 *  3. Delegate navigateUp to NavController.
 *
 * Ad initialization (MobileAds.initialize) happens in [com.example.admobnextgensdk.App.onCreate].
 * All ad loading/showing is done inside individual fragments via [com.example.admobnextgensdk.utils.BaseFragment.adManager].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Request UMP consent — this must complete before any ad is loaded.
        // The navigation graph starts at SplashFragment which waits for its own
        // ad loads, so consent is guaranteed to be resolved before any ad request.
        ConsentManager.requestConsent(
            activity = this,
            isDebug = BuildConfig.DEBUG
        ) { canRequestAds ->
            // canRequestAds is informational here — the SDK managers check it internally.
            // Nothing to do: SplashFragment will start loading ads on its own lifecycle.
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}