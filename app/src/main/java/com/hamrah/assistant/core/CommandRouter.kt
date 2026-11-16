package com.hamrah.assistant.core

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.hamrah.assistant.service.HamrahAccessibilityService
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CommandRouter(private val ctx: Context, private val sm: SpeechManager) {
    private val client = OkHttpClient()
    // آدرس سرور Hugging Face شما
    private val serverUrl = "https://hamid248-hamrah-ai.hf.space/api/predict"

    fun route(cmd: String) {
        sm.speak("در حال پردازش...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonPayload = JSONObject().apply {
                    put("data", org.json.JSONArray().put(cmd))
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(serverUrl).post(body).build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val responseData = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseData)
                    val aiOutput = jsonResponse.getJSONArray("data").getString(0)
                    
                    withContext(Dispatchers.Main) {
                        executeAction(aiOutput)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sm.speak("خطا در اتصال به سرور.")
                }
            }
        }
    }

    private fun executeAction(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val action = json.optString("action")
            
            when (action) {
                "OPEN_APP" -> {
                    val target = json.optString("target")
                    launchApp(target)
                }
                "CALL" -> {
                    val target = json.optString("target")
                    sm.speak("قابلیت تماس در نسخه بعدی فعال می‌شود. $target")
                    // اینجا می‌توانید دستور تماس را پیاده‌سازی کنید
                }
                "SET_ALARM" -> {
                    val time = json.optString("time")
                    sm.speak("تنظیم هشدار برای $time")
                    // پیاده‌سازی هشدار
                }
                "UNKNOWN" -> sm.speak("متوجه دستور نشدم.")
                else -> sm.speak("این دستور پشتیبانی نمی‌شود.")
            }
        } catch (e: Exception) {
            sm.speak("خطا در خواندن پاسخ.")
        }
    }

    private fun launchApp(appName: String) {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
        
        // جستجوی ساده برای نام برنامه
        val targetApp = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }

        if (targetApp == null) {
            sm.speak("برنامه‌ای با نام $appName پیدا نکردم.")
            return
        }

        if (!BlacklistManager.isPackageAllowed(targetApp.packageName)) {
            sm.speak("دسترسی به این برنامه مسدود است.")
            return
        }

        pm.getLaunchIntentForPackage(targetApp.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
            sm.speak("برنامه ${pm.getApplicationLabel(targetApp)} باز شد.")
        }
    }
}
