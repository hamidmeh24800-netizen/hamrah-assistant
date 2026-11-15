package com.hamrah.assistant.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hamrah.assistant.core.BlacklistManager

class HamrahAccessibilityService : AccessibilityService() {
    companion object { var instance: HamrahAccessibilityService? = null; private set }
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun executeAction(action: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (!BlacklistManager.isPackageAllowed(root.packageName?.toString())) return false
        return when (action) {
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            else -> false
        }
    }
}
