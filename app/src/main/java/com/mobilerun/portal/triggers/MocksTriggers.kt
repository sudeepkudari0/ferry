package com.mobilerun.portal.triggers

import android.content.Context

class TriggerRuntime {
    companion object {
        fun initialize(context: Context) {}
        fun processEvent(event: Any?) {}
        fun processNodeReplaced(packageName: String?, activityName: String?) {}
    }
}
