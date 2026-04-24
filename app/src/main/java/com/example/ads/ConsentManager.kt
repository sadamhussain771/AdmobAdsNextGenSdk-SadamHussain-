package com.customlauncher.app.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {

    private const val TAG = "ConsentManager"

    /**
     * Request consent information and show the form if required.
     * Always calls [onComplete] with whether ads can be requested.
     *
     * @param debugGeography Set to [ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA]
     *                       during development to simulate EEA behaviour. Remove for production.
     */
    fun requestConsent(
        activity: Activity,
        isDebug: Boolean = false,
        onComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        val params = ConsentRequestParameters.Builder()
            .build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Info updated — load and show form if required
                if (consentInfo.isConsentFormAvailable &&
                    consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
                ) {
                    loadAndShowForm(activity, consentInfo, onComplete)
                } else {
                    Log.d(
                        TAG,
                        "Consent not required or already obtained — status: ${consentInfo.consentStatus}"
                    )
                    onComplete(consentInfo.canRequestAds())
                }
            },
            { error ->
                // Non-fatal — proceed with ads anyway (fail open)
                Log.w(TAG, "Consent info update failed: ${error.message} — proceeding with ads")
                onComplete(true)
            }
        )
    }

    private fun loadAndShowForm(
        activity: Activity,
        consentInfo: ConsentInformation,
        onComplete: (canRequestAds: Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Consent form error: ${formError.message} — proceeding with ads")
                onComplete(true)
            } else {
                Log.d(TAG, "Consent form completed — canRequestAds: ${consentInfo.canRequestAds()}")
                onComplete(consentInfo.canRequestAds())
            }
        }
    }

    /** True if the SDK has consent (or consent is not required). */
    fun canRequestAds(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).canRequestAds()

    /**
     * Call from a Settings screen to let users update their consent choices.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: () -> Unit = {}
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options form error: ${formError.message}")
            onComplete()
        }
    }
}

/** Extension on ConsentInformation for cleaner call sites. */
fun ConsentInformation.canRequestAds(): Boolean =
    consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
            consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
