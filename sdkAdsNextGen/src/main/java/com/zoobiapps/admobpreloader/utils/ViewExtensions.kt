package com.zoobiapps.admobpreloader.utils

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.zoobiapps.admobpreloader.utils.AdLogger.TAG_ADS

fun ViewGroup.addCleanView(view: View?) {
    if (view == null) {
        Log.e(TAG_ADS, "addCleanView: View ref is null")
        return
    }
    (view.parent as? ViewGroup)?.removeView(view)
    this.removeAllViews()
    view.let { this.addView(it) }
    this.visibility = View.VISIBLE
}