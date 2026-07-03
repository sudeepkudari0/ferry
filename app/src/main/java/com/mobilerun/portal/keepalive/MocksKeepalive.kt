package com.mobilerun.portal.keepalive

import android.content.Context

class KeepAliveController {
    companion object {
        fun reconcileBestEffort(context: Context): KeepAliveResult = KeepAliveResult(null)
        fun retryStartupIfEnabledAndInactive(context: Context): String? = null
    }
}

class KeepAliveResult(val deferredReason: String?)
