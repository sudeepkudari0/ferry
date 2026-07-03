package com.mobilerun.portal.api

import android.content.pm.PackageManager
import com.mobilerun.portal.core.StateRepository
import com.mobilerun.portal.input.MobilerunKeyboardIME
import com.mobilerun.portal.service.MobilerunAccessibilityService

class ApiHandler(
    val stateRepository: StateRepository,
    val getIme: () -> MobilerunKeyboardIME?,
    val getPackageManager: () -> PackageManager,
    val getVersionName: () -> String,
    val service: MobilerunAccessibilityService
) {
    fun handleRequest(request: String): String = "{}"
    fun close() {}
}
