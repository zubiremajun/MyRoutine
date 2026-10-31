package com.myroutine.app
import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

// Catches any crash anywhere in the app and shows it on screen instead of
// silently closing, so the real cause can be read and copied.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            try {
                val sw = StringWriter(); ex.printStackTrace(PrintWriter(sw))
                val i = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("msg", sw.toString())
                startActivity(i)
            } catch (e: Exception) { }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
