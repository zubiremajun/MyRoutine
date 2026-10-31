package com.myroutine.app
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

// Shown instead of a silent crash: the app's own error, in plain text,
// so it can be read and copied straight off the screen.
class CrashActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val msg = intent.getStringExtra("msg") ?: "Unknown error"
        val title = TextView(this).apply {
            text = "MyRoutine crashed — error details:"
            textSize = 18f; setTextColor(Color.WHITE); setPadding(32, 60, 32, 20)
        }
        val body = TextView(this).apply {
            text = msg; textSize = 13f; setTextColor(0xFFFFC266.toInt())
            setPadding(32, 0, 32, 20); setTextIsSelectable(true)
        }
        val copyBtn = Button(this).apply {
            text = "Copy error text"
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("error", msg))
                Toast.makeText(this@CrashActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
        val closeBtn = Button(this).apply { text = "Close"; setOnClickListener { finish() } }
        val scroll = ScrollView(this).apply { addView(body) }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B1F18"))
            addView(title); addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(copyBtn); addView(closeBtn)
            gravity = Gravity.TOP
        }
        setContentView(col)
    }
}
