package com.mobilerun.portal.events.model

class PortalEvent(val type: EventType, val target: Any? = null, val timestamp: Long = 0, val payload: Any? = null)
