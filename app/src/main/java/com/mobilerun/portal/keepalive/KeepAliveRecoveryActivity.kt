package com.mobilerun.portal.keepalive
import android.app.Activity
class KeepAliveRecoveryActivity : Activity() {
    companion object {
        const val EXTRA_REASON = "reason"
        const val EXTRA_RECOVERY_TOKEN = "token"
    }
}
