package com.myroutine.app
import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var previewPlayer: MediaPlayer? = null

    inner class Bridge {
        @JavascriptInterface fun getSchedule(): String = Store.json(this@MainActivity) ?: ""
        @JavascriptInterface fun saveSchedule(s: String) { Store.save(this@MainActivity, s); Scheduler.scheduleAll(this@MainActivity) }
        @JavascriptInterface fun getBanner(): String = Store.banner(this@MainActivity) ?: ""
        @JavascriptInterface fun saveBanner(d: String) { Store.saveBanner(this@MainActivity, d) }
        @JavascriptInterface fun testCall() { CallNotifier.show(this@MainActivity, -1) }
        @JavascriptInterface fun requestPermissions() { runOnUiThread { askPermissions() } }

        @JavascriptInterface fun saveRingtone(name: String, dataUrl: String): String =
            RingtoneStore.save(this@MainActivity, name, dataUrl) ?: ""
        @JavascriptInterface fun listRingtones(): String = RingtoneStore.list(this@MainActivity)
        @JavascriptInterface fun deleteRingtone(id: String) { RingtoneStore.delete(this@MainActivity, id) }

        @JavascriptInterface fun playPreview(id: String) {
            runOnUiThread {
                stopPreviewInternal()
                try {
                    val path = RingtoneStore.path(this@MainActivity, id) ?: return@runOnUiThread
                    previewPlayer = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        setDataSource(path); prepare(); start()
                        setOnCompletionListener { stopPreviewInternal() }
                    }
                } catch (e: Exception) {}
            }
        }
        @JavascriptInterface fun stopPreview() { runOnUiThread { stopPreviewInternal() } }
    }

    private fun stopPreviewInternal() {
        try { previewPlayer?.stop(); previewPlayer?.release() } catch (e: Exception) {}
        previewPlayer = null
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                // Without this, a WebView renderer crash (common on low-RAM phones)
                // takes the whole app down instead of just the page. We recover by
                // rebuilding the screen instead of letting Android kill the app.
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Toast.makeText(this@MainActivity, "Reloading…", Toast.LENGTH_SHORT).show()
                    recreate()
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                // Without this, <input type="file"> (banner / ringtone upload) does
                // nothing at all when tapped inside a WebView.
                override fun onShowFileChooser(
                    view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?
                ): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback
                    val intent = (params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).addType()).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    return try { startActivityForResult(intent, 7171); true }
                    catch (e: Exception) { filePathCallback = null; false }
                }
                private fun Intent.addType(): Intent { type = "*/*"; return this }
            }
            addJavascriptInterface(Bridge(), "Android")
            setBackgroundColor(0xFF0F1A17.toInt())
        }
        setContentView(web)
        web.setOnApplyWindowInsetsListener { v, ins ->
            if (Build.VERSION.SDK_INT >= 30) {
                val s = ins.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(s.left, s.top, s.right, s.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(ins.systemWindowInsetLeft, ins.systemWindowInsetTop, ins.systemWindowInsetRight, ins.systemWindowInsetBottom)
            }
            ins
        }
        web.loadUrl("file:///android_asset/index.html")
        askPermissions()
        Scheduler.scheduleAll(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 7171) {
            val uris = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun askPermissions() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != 0)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                try { startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))) }
                catch (e: Exception) {}
            }
        }
    }

    override fun onDestroy() { stopPreviewInternal(); super.onDestroy() }
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
