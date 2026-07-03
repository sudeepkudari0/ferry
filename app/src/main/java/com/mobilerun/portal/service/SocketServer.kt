package com.mobilerun.portal.service

import com.mobilerun.portal.api.ApiHandler
import com.mobilerun.portal.config.ConfigManager

class SocketServer(val apiHandler: ApiHandler, val configManager: ConfigManager, val actionDispatcher: ActionDispatcher) {
    fun start(port: Int): Boolean = true
    fun stop() {}
    fun getPort(): Int = 8080
    fun isRunning(): Boolean = false
}
