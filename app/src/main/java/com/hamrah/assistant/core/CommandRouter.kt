package com.hamrah.assistant.core

import android.content.Context
import android.content.Intent
import com.hamrah.assistant.service.HamrahAccessibilityService
import java.util.Calendar

class CommandRouter(private val ctx: Context, private val sm: SpeechManager) {
    fun route(cmd: String) {
        val s = normalize(cmd)
        when {
            s.contains("برو عقب") -> {
                val ok = HamrahAccessibilityService.instance?.executeAction("BACK") == true
                sm.speak(if (ok) "انجام شد" else "متأسفم، نتوانستم")
            }
            s.contains("ساعت") -> {
                val c = Calendar.getInstance()
                sm.speak("ساعت %02d:%02d است".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)))
            }
            s.contains("باز کن") -> launchApp(extractApp(s))
            else -> sm.speak("این دستور را نمی‌شناسم")
        }
    }

    private fun launchApp(label: String?) {
        if (label.isNullOrBlank()) { sm.speak("نام برنامه را نگفتی"); return }
        val pm = ctx.packageManager
        val app = pm.getInstalledApplications(0).firstOrNull {
            pm.getApplicationLabel(it).toString().contains(label, true)
        } ?: run { sm.speak("برنامه‌ای با نام $label پیدا نکردم"); return }

        if (!BlacklistManager.isPackageAllowed(app.packageName)) {
            sm.speak("متأسفم، به این برنامه دسترسی ندارم"); return
        }
        pm.getLaunchIntentForPackage(app.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
            sm.speak("${pm.getApplicationLabel(app)} باز شد")
        }
    }

    private fun extractApp(s: String): String? =
        listOf("باز کن", "اجرا کن", "برو به").firstNotNullOfOrNull { p ->
            s.indexOf(p).takeIf { it > 0 }?.let { s.substring(0, it).trim() }
        }

    private fun normalize(s: String) = s.lowercase()
        .replace('ي','ی').replace('ك','ک').replace("\u200c"," ")
        .replace(Regex("\\s+"), " ").trim()
}
