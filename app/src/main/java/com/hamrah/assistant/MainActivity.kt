package com.hamrah.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hamrah.assistant.core.CommandRouter
import com.hamrah.assistant.core.SpeechManager

class MainActivity : AppCompatActivity() {
    private lateinit var sm: SpeechManager
    private lateinit var router: CommandRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sm = SpeechManager(this) { cmd -> runOnUiThread { router.route(cmd) } }
        router = CommandRouter(this, sm)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48,48,48,48)
        }
        val status = TextView(this).apply { text = "آماده"; textSize = 18f }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "🎙 شروع شنیدن"; textSize = 18f
            setOnClickListener { sm.startListening(); status.text = "در حال شنیدن..." }
        })
        setContentView(layout)

        sm.initialize()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
    }
    override fun onDestroy() { sm.stopListening(); super.onDestroy() }
}
