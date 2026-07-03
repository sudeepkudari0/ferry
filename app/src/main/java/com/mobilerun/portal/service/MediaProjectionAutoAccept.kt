package com.mobilerun.portal.service

object MediaProjectionAutoAccept {
    sealed class AutoAcceptResult {
        object Failed : AutoAcceptResult()
        object Success : AutoAcceptResult()
    }
    fun isMediaProjectionDialog(event: Any?, className: String?): Boolean = false
    fun tryAutoAccept(root: android.view.accessibility.AccessibilityNodeInfo?, className: String?): AutoAcceptResult = AutoAcceptResult.Success
}
