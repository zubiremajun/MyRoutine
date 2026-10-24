package com.myroutine.app
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class CallActivity : Activity() {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var vib: Vibrator? = null
    private val h = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var acceptBtn: Button
    private var spoken = ""
    private var dayKey = ""
    private var itemId = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        CallNotifier.cancel(this)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val idx = intent.getIntExtra("idx", -1)
        var name = "Friend"; var title = "Test call"; var kind = "prayer"; var custom = ""; var secs = 60; var ring = "rt1"
        try {
            val root = JSONObject(Store.json(this) ?: "{}")
            name = root.optString("name", "Friend"); secs = root.optInt("ringdur", 60); ring = root.optString("ring", "rt1")
            val items = root.optJSONArray("items")
            if (idx >= 0 && items != null && idx < items.length()) {
                val item = items.getJSONObject(idx)
                title = item.optString("n"); kind = item.optString("k", "work"); custom = item.optString("m", "")
                itemId = item.optString("t", "") + "|" + title
            }
        } catch (e: Exception) {}
        spoken = name + " স্যার, " + (if (custom.isNotBlank()) custom else Msg.of(kind, title))
        dayKey = DateFormat.format("yyyy-MM-dd", Calendar.getInstance()).toString()

        fun tv(t: String, sz: Float, c: Int) = TextView(this).apply {
            text = t; textSize = sz; setTextColor(c); gravity = Gravity.CENTER
        }
        fun circle(color: Int, label: String) = Button(this).apply {
            text = label; textSize = 28f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            layoutParams = LinearLayout.LayoutParams(210, 210).apply { setMargins(70, 0, 70, 0) }
        }
        status = tv("Incoming call...", 16f, 0xFF8FA8A0.toInt())
        val decline = circle(0xFFFF453A.toInt(), "\u2715")
        acceptBtn = circle(0xFF2FD566.toInt(), "\u260E")
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER; addView(decline); addView(acceptBtn) }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B1F18")); setPadding(48, 200, 48, 160)
            addView(tv("MyRoutine", 30f, Color.WHITE)); addView(status)
            addView(tv(title, 20f, 0xFFF2C14E.toInt()).apply { setPadding(0, 40, 0, 0) })
            addView(View(this@CallActivity), LinearLayout.LayoutParams(0, 0, 1f))
            addView(row)
        }
        setContentView(col)

        decline.setOnClickListener { stopRingAndClose() }
        acceptBtn.setOnClickListener { accept() }
        startRinging(ring)
        h.postDelayed({ stopRingAndClose() }, secs * 1000L)
    }

    private fun startRinging(ring: String) {
        try {
            val uri = if (ring.startsWith("c")) {
                val id = ring.substring(1)
                val path = RingtoneStore.path(this, id)
                if (path != null) android.net.Uri.fromFile(java.io.File(path)) else RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } else RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getValidRingtoneUri(this)

            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIC).build())
                setDataSource(this@CallActivity, uri)
                isLooping = true
                prepare(); start()
            }
        } catch (e: Exception) {}

        val pattern = longArrayOf(0, 900, 500)
        vib = if (Build.VERSION.SDK_INT >= 31) (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
              else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        try {
            if (Build.VERSION.SDK_INT >= 26) vib?.vibrate(VibrationEffect.createWaveform(pattern, 1))
            else @Suppress("DEPRECATION") vib?.vibrate(pattern, 1)
        } catch (e: Exception) {}
    }

    private fun stopRinging() {
        try { player?.stop(); player?.release() } catch (e: Exception) {}
        player = null
        try { vib?.cancel() } catch (e: Exception) {}
    }

    private fun stopRingAndClose() { stopRinging(); finish() }

    private fun markDone() {
        try {
            val root = JSONObject(Store.json(this) ?: "{}")
            val done = if (root.has("done")) root.getJSONObject("done") else JSONObject().also { root.put("done", it) }
            val todays = if (done.has(dayKey)) done.getJSONObject(dayKey) else JSONObject().also { done.put(dayKey, it) }
            todays.put(itemId, 1)
            Store.save(this, root.toString())
        } catch (e: Exception) {}
    }

    private fun accept() {
        h.removeCallbacksAndMessages(null)
        stopRinging()
        markDone()
        status.text = "Connected"; acceptBtn.visibility = View.GONE
        tts = TextToSpeech(this) { st ->
            val t = tts
            if (st != TextToSpeech.SUCCESS || t == null) { finishSoon(); return@TextToSpeech }
            var r = t.setLanguage(Locale("bn", "BD"))
            if (r < TextToSpeech.LANG_AVAILABLE) r = t.setLanguage(Locale("bn", "IN"))
            if (r < TextToSpeech.LANG_AVAILABLE) {
                Toast.makeText(this, "Install the Bengali voice in Text-to-speech settings", Toast.LENGTH_LONG).show()
                try { startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } catch (e: Exception) {}
                finishSoon(); return@TextToSpeech
            }
            t.setSpeechRate(0.9f)
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(u: String?) {}
                override fun onError(u: String?) { h.post { finish() } }
                override fun onDone(u: String?) { h.postDelayed({ finish() }, 1000) }
            })
            t.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "u1")
        }
    }

    private fun finishSoon() { h.postDelayed({ finish() }, 3000) }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null); stopRinging()
        tts?.shutdown()
        super.onDestroy()
    }
}
