package com.mobilerun.portal.streaming

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder

class ScreenCaptureActivity : Activity() {
    companion object {
        fun createInstallPermissionIntent(context: android.content.Context): Intent = Intent()
    }
}
class ScreenCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
