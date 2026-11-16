package com.hamrah.assistant.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CommandRouter(private val ctx: Context, private val sm: SpeechManager) {
    // افزایش زمان انتظار به ۶۰ ثانیه
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
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
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val responseData = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseData)
                    val aiOutput = jsonResponse.getJSONArray("data").getString(0)
                    
                    withContext(Dispatchers.Main) {
                        executeAction(aiOutput)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errMsg = e.message ?: "ناشناخته"
                    sm.speak("خطا در اتصال به سرور: $errMsg")
                }
            }
        }
    }

    private fun executeAction(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val action = json.optString("action")
            val target = json.optString("target")
            
            when (action) {
                "OPEN_APP" -> launchApp(target)
                "UNKNOWN" -> sm.speak("متوجه دستور نشدم.")
                else -> sm.speak("دستور $action پشتیبانی نمی‌شود.")
            }
        } catch (e: Exception) {
            sm.speak("خطا در خواندن پاسخ: $jsonStr")
        }
    }

    private fun launchApp(appName: String) {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
        val targetApp = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }

        if (targetApp == null) {
            sm.speak("برنامه‌ای با نام $appName پیدا نکردم.")
            return
        }

        pm.getLaunchIntentForPackage(targetApp.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
            sm.speak("برنامه ${pm.getApplicationLabel(targetApp)} باز شد.")
        }
    }
}
