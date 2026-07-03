package com.mobilerun.portal.service

import android.content.Context
import com.mobilerun.portal.api.ApiHandler

class ActionDispatcher(val apiHandler: ApiHandler) {
    fun dispatchAction(action: String, args: Any?) {}
    fun close() {}
}

object ReverseConnectionService {
    fun getInstance(): ReverseConnectionService? = null
    fun requestStart(context: Context) {}
    fun requestStop() {}
}

class KeepAliveRecoveryActivity

object MediaProjectionScreenshotter {
    fun getInstance(context: Context): MediaProjectionScreenshotter = MediaProjectionScreenshotter
    fun capture(): java.util.concurrent.CompletableFuture<String> = java.util.concurrent.CompletableFuture()
}
