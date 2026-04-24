package com.example.admobnextgensdk.utils

import androidx.fragment.app.Fragment
import com.customlauncher.app.di.DIComponent
import com.example.ads.AdManager
import org.koin.android.ext.android.inject

/**
 * Base fragment that provides convenient access to [com.example.ads.AdManager] and [com.customlauncher.app.di.DIComponent]
 * for all screens that need to load or show ads.
 */
abstract class BaseFragment : Fragment() {

    /** Koin-injected AdManager — the single gated entry point for all ad operations. */
    protected val adManager: AdManager by inject()

    /** Direct DI component for lower-level SDK access when needed. */
    protected val di: DIComponent by lazy { DIComponent() }
}