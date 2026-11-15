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
    private val onCommand: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun initialize() {
        tts = TextToSpeech(context, this)
        val dir = File(context.filesDir, "vosk-model-small-fa-0.42")
        if (File(dir, "conf/model.conf").exists()) {
            try { model = Model(dir.absolutePath); Log.d("Hamrah", "Vosk loaded") }
            catch (e: Exception) { Log.e("Hamrah", "Vosk load failed", e) }
        }
    }

    fun startListening() {
        val m = model ?: run { speak("مدل گفتار آماده نیست"); return }
        try {
            val r = Recognizer(m, 16000.0f)
            speechService = SpeechService(r, 16000.0f)
            speechService?.startListening(this)
        } catch (e: Exception) { Log.e("Hamrah", "Listen failed", e) }
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
