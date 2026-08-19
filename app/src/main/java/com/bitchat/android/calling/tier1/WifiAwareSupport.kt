package com.bitchat.android.calling.tier1

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object WifiAwareSupport {
    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
}