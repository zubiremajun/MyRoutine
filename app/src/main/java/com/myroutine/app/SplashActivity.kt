package com.myroutine.app
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.view.WindowManager
import android.widget.ImageView

class SplashActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.splash_banner)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF0A1A3A.toInt())
        }
        setContentView(iv)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
