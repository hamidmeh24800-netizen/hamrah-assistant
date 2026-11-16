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
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 48, 48, 48) }
        
        val status = TextView(this).apply { text = "در حال آماده سازی..."; textSize = 16f }
        val resultView = TextView(this).apply { text = "نتیجه اینجا نمایش داده می‌شود"; textSize = 18f; setPadding(0, 30, 0, 30) }
        
        sm = SpeechManager(
            this,
            onCommand = { cmd -> 
                runOnUiThread { 
                    status.text = "دستور: $cmd\nدر حال ارسال به سرور..."
                    router.route(cmd) 
                } 
            },
            onStatusUpdate = { msg -> runOnUiThread { status.text = msg } },
            onModelReady = { runOnUiThread { status.text = "مدل آماده است." } }
        )
        router = CommandRouter(this, sm)

        val btn = Button(this).apply {
            text = "🎙 شروع شنیدن"; textSize = 18f
            setOnClickListener {
                if (!isListening) {
                    if (sm.isModelReady()) {
                        sm.startListening()
                        isListening = true
                        text = "⏹ توقف"
                        status.text = "گوش می‌دهم..."
                    } else {
                        status.text = "مدل هنوز آماده نیست."
                    }
                } else {
                    sm.stopListening()
                    isListening = false
                    text = "🎙 شروع شنیدن"
                    status.text = "متوقف شد."
                }
            }
        }

        layout.addView(status)
        layout.addView(btn)
        layout.addView(resultView)
        setContentView(layout)

        sm.initialize()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
    }
    override fun onDestroy() { sm.stopListening(); super.onDestroy() }
}
