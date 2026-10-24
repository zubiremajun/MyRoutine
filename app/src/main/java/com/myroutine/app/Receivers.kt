package com.myroutine.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.scheduleAll(c)            // queue the same item's next occurrence
        CallNotifier.show(c, i.getIntExtra("idx", -1))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { Scheduler.scheduleAll(c) }
}
