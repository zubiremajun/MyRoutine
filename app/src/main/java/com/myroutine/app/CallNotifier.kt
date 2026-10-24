package com.myroutine.app
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import org.json.JSONObject

object CallNotifier {
    const val ID = 1001
    private const val CH = "routine_call_v1"

    fun show(c: Context, idx: Int) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH, "Routine calls", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                Settings.System.DEFAULT_RINGTONE_URI,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIC).build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 900, 500, 900, 500, 900)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)

        var title = "Routine call"; var secs = 60
        try {
            val root = JSONObject(Store.json(c) ?: "{}")
            secs = root.optInt("ringdur", 60)
            val items = root.optJSONArray("items")
            if (idx >= 0 && items != null && idx < items.length()) title = items.getJSONObject(idx).optString("n")
        } catch (e: Exception) {}

        val pi = PendingIntent.getActivity(
            c, 100, Intent(c, CallActivity::class.java).putExtra("idx", idx)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("MyRoutine").setContentText(title)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true).setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setTimeoutAfter(secs * 1000L)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(ID, n)
    }

    fun cancel(c: Context) { c.getSystemService(NotificationManager::class.java).cancel(ID) }
}
