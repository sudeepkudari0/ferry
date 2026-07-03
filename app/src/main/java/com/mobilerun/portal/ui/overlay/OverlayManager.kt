package com.mobilerun.portal.ui.overlay

import android.content.Context
class OverlayManager(val context: Context) {
    fun updateOverlayOffset(offset: Int) {}
    fun updateOverlayVisibility(visible: Boolean) {}
    fun onOverlayVisibilityChanged(visible: Boolean) {}
    fun onOverlayOffsetChanged(offset: Int) {}
    fun onSocketServerEnabledChanged(enabled: Boolean) {}
    fun onSocketServerPortChanged(port: Int) {}
    fun onWebSocketEnabledChanged(enabled: Boolean) {}
    fun onWebSocketPortChanged(port: Int) {}
    fun clearElements() {}
    fun refreshOverlay() {}
    fun setPositionOffsetY(y: Int) {}
    fun calculateAutoOffset(): Int = 0
    fun getPositionOffsetY(): Int = 0
    fun showOverlay() {}
    fun hideOverlay() {}
    fun isDrawingEnabled(): Boolean = false
    fun setDrawingEnabled(enabled: Boolean) {}
    fun addElement(text: String?, rect: android.graphics.Rect, type: String, index: Int) {}
    fun close() {}
}
