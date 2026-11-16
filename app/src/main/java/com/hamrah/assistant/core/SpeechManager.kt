package com.hamrah.assistant.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onCommand: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit,
    private val onModelReady: () -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun initialize() {
        tts = TextToSpeech(context, this)
        val dir = File(context.filesDir, "vosk-model-small-fa-0.42")
        if (File(dir, "conf/model.conf").exists()) {
            loadModel(dir)
        } else {
            downloadModel(dir)
        }
    }

    private fun downloadModel(dir: File) {
        onStatusUpdate("در حال دانلود مدل گفتار (حدود ۵۰ مگابایت)...\nلطفاً صبر کنید.")
        Thread {
            try {
                val archive = File(context.cacheDir, "vosk-fa.zip")
                val url = java.net.URL("https://alphacephei.com/vosk/models/vosk-model-small-fa-0.42.zip")
                url.openStream().use { input ->
                    archive.outputStream().use { output -> input.copyTo(output) }
                }
                onStatusUpdate("در حال استخراج مدل...")
                dir.deleteRecursively()
                dir.mkdirs()
                java.util.zip.ZipInputStream(archive.inputStream().buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val name = entry.name.replace('\\', '/')
                        if (name.startsWith("/") || name.split('/').any { it == ".." }) throw Exception("Invalid path")
                        val relative = name.substringAfter('/', "")
                        if (relative.isNotEmpty()) {
                            val out = File(dir, relative)
                            if (entry.isDirectory) out.mkdirs() else {
                                out.parentFile?.mkdirs()
                                out.outputStream().use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                    }
                }
                archive.delete()
                loadModel(dir)
            } catch (e: Exception) {
                Log.e("Hamrah", "Download failed", e)
                onStatusUpdate("دانلود ناموفق بود. اینترنت را بررسی کنید.")
            }
        }.start()
    }

    private fun loadModel(dir: File) {
        try {
            model = Model(dir.absolutePath)
            onModelReady()
            onStatusUpdate("آماده است. دکمه را بزنید.")
        } catch (e: Exception) {
            Log.e("Hamrah", "Vosk load failed", e)
            onStatusUpdate("خطا در بارگذاری مدل.")
        }
    }

    fun isModelReady() = model != null

    fun startListening() {
        val m = model ?: return
        try {
            val r = Recognizer(m, 16000.0f)
            speechService = SpeechService(r, 16000.0f)
            speechService?.startListening(this)
        } catch (e: Exception) {
            Log.e("Hamrah", "Listen failed", e)
        }
    }

    fun stopListening() {
        speechService?.stop(); speechService?.shutdown(); speechService = null
    }

    fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "h${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val r = tts?.setLanguage(Locale.forLanguageTag("fa-IR"))
            ttsReady = r == TextToSpeech.LANG_AVAILABLE || r == TextToSpeech.LANG_COUNTRY_AVAILABLE
        }
    }

    override fun onResult(h: String?) {
        val t = parse(h) ?: return
        if (t.isNotBlank()) onCommand(t)
    }
    override fun onPartialResult(h: String?) {}
    override fun onFinalResult(h: String?) { onResult(h) }
    override fun onError(e: Exception?) { stopListening() }
    override fun onTimeout() { stopListening() }

    private fun parse(json: String?): String? = try {
        org.json.JSONObject(json ?: "{}").optString("text", "").trim().ifBlank { null }
    } catch (_: Exception) { null }
}
