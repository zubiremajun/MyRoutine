package com.myroutine.app
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.util.Calendar

object Scheduler {
    private fun receiverPi(c: Context, i: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, i, Intent(c, AlarmReceiver::class.java).putExtra("idx", i),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // JS weekday convention: 0=Sun .. 6=Sat. Calendar.DAY_OF_WEEK is 1=Sun .. 7=Sat.
    private fun jsDow(cal: Calendar) = cal.get(Calendar.DAY_OF_WEEK) - 1

    fun scheduleAll(c: Context) {
        val raw = Store.json(c) ?: return
        val items = try { JSONObject(raw).getJSONArray("items") } catch (e: Exception) { return }
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until maxOf(Store.oldCount(c), items.length())) am.cancel(receiverPi(c, i))
        val show = PendingIntent.getActivity(
            c, 999, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (i in 0 until items.length()) {
            val it = items.getJSONObject(i)
            if (!it.optBoolean("on", true)) continue
            val p = it.optString("t", "").split(":")
            if (p.size != 2) continue
            val days = it.optJSONArray("days")
            val allow: Set<Int>? = if (days != null && days.length() > 0)
                (0 until days.length()).map { days.getInt(it) }.toSet() else null

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, p[0].toInt()); set(Calendar.MINUTE, p[1].toInt())
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            if (allow != null) {
                var guard = 0
                while (jsDow(cal) !in allow && guard < 8) { cal.add(Calendar.DAY_OF_YEAR, 1); guard++ }
                if (guard >= 8) continue // malformed "days" list, skip safely
            }
            // setAlarmClock is exact and survives Doze, but on Android 12+ it throws
            // SecurityException if the person hasn't granted the exact-alarm permission
            // (or it's been revoked). Fall back to a regular alarm instead of crashing;
            // MainActivity also prompts the person to grant the exact permission.
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, show), receiverPi(c, i))
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, receiverPi(c, i))
            }
        }
        Store.setCount(c, items.length())
    }
}
