package com.mobilerun.portal.taskprompt

class TaskPromptSettingsConstraints(
    var llmModel: String = "",
    var reasoning: Boolean = false,
    var vision: Boolean = false,
    var maxSteps: Int = 0,
    var temperature: Float = 0.0f,
    var executionTimeout: Int = 0
) {
    companion object {
        fun clampTemperature(t: Float): Float = t
        fun clampExecutionTimeout(t: Int): Int = t
        fun clampMaxSteps(t: Int): Int = t
    }
}

class PortalCloudClient {
    companion object {
        const val DEFAULT_EXECUTION_TIMEOUT = 300
        const val DEFAULT_MODEL_ID = "default"
        const val DEFAULT_TEMPERATURE = 0.0f
        const val DEFAULT_MAX_STEPS = 50
        const val DEFAULT_REASONING = false
        const val DEFAULT_VISION = false
    }
}
class PortalTaskSettings(
    var llmModel: String = "",
    var reasoning: Boolean = false,
    var vision: Boolean = false,
    var maxSteps: Int = 0,
    var temperature: Double = 0.0,
    var executionTimeout: Int = 0
)
class PortalActiveTaskRecord(
    var taskId: String = "",
    var promptPreview: String = "",
    var startedAtMs: Long = 0,
    var executionTimeoutSec: Int = 0,
    var pollDeadlineMs: Long = 0,
    var lastStatus: String = "",
    var startedToastShown: Boolean = false,
    var terminalToastShown: Boolean = false,
    var triggerRuleId: String? = null,
    var returnToPortalOnTerminal: Boolean = false,
    var terminalReturnHandled: Boolean = false,
    var terminalTransitionHandled: Boolean = false
)
class PortalTaskTracking {
    companion object {
        const val STATUS_CREATED = "created"
    }
}
