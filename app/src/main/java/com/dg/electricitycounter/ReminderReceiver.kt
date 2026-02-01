package com.dg.electricitycounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = ReminderScheduler(context)
        val notificationHelper = NotificationHelper(context)
        
        val reminderType = intent.getStringExtra("reminder_type") ?: "first"
        
        // Всегда показываем уведомление, если сработал будильник
        notificationHelper.showReminderNotification()
        
        // Для отладки
        val currentTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date())
        Toast.makeText(
            context,
            "🔔 Напоминание ($reminderType) сработало в $currentTime",
            Toast.LENGTH_LONG
        ).show()
        
        // Если это было первое напоминание (24 число), планируем следующее на завтра
        if (reminderType == "first") {
            scheduler.scheduleNextDayReminder()
        }
        // Если это ежедневное напоминание, планируем следующее на завтра
        else if (reminderType == "daily") {
            scheduler.scheduleNextDayReminder()
        }
    }
}