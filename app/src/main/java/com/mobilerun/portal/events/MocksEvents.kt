package com.mobilerun.portal.events

import com.mobilerun.portal.events.model.PortalEvent

object EventHub {
    fun init(config: Any) {}
    fun dispatch(event: PortalEvent) {}
    fun emit(event: PortalEvent) {}
}

class PortalWebSocketServer(val p1: Int, val p2: Any, val p3: Any, val p4: Any) {
    fun start() {}
    fun stop() {}
    fun stopSafely() {}
}
