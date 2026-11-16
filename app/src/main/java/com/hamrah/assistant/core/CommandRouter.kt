package com.hamrah.assistant.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CommandRouter(private val ctx: Context, private val sm: SpeechManager) {
    private val serverUrl = "https://hamid248-hamrah-ai.hf.space/api/predict"

    fun route(cmd: String) {
        sm.speak("در حال بیدار کردن سرور...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. بیدار کردن سرور (Ping)
                val pingUrl = URL("https://hamid248-hamrah-ai.hf.space")
                val pingConn = pingUrl.openConnection() as HttpURLConnection
                pingConn.connectTimeout = 20000
                pingConn.readTimeout = 20000
                pingConn.requestMethod = "GET"
                pingConn.connect()
                pingConn.disconnect()

                withContext(Dispatchers.Main) { sm.speak("در حال پردازش دستور...") }

                // 2. ارسال درخواست اصلی
                val url = URL(serverUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 60000
                conn.readTimeout = 60000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val jsonPayload = JSONObject().apply {
                    put("data", org.json.JSONArray().put(cmd))
                }

                val outputStream = OutputStreamWriter(conn.outputStream, "UTF-8")
                outputStream.write(jsonPayload.toString())
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = JSONObject(response.toString())
                    val aiOutput = jsonResponse.getJSONArray("data").getString(0)

                    withContext(Dispatchers.Main) {
                        executeAction(aiOutput)
                    }
                } else {
                    throw Exception("HTTP Error: $responseCode")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sm.speak("خطای شبکه: ${e.message}")
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
            sm.speak("خطا در خواندن پاسخ.")
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
            sm.speak("برنامه باز شد.")
        }
    }
}
