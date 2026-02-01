package com.dg.electricitycounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("reminder_type") ?: "first"
        val action = intent.action
        
        when {
            // 🔧 ВОССТАНОВЛЕНИЕ ПОСЛЕ ПЕРЕЗАГРУЗКИ
            action == Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            // Обычное напоминание
            else -> {
                handleReminder(context, reminderType)
            }
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        // Проверяем, были ли включены напоминания
        val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
        val isReminderEnabled = prefs.getBoolean("reminder_enabled", false)
        
        if (isReminderEnabled) {
            // Восстанавливаем напоминания
            val scheduler = ReminderScheduler(context)
            scheduler.scheduleMonthlyReminder()
        }
    }
    
    private fun handleReminder(context: Context, reminderType: String) {
        val scheduler = ReminderScheduler(context)
        val notificationHelper = NotificationHelper(context)
        
        // Показываем уведомление
        notificationHelper.showReminderNotification()
        
        // Планируем следующее напоминание на завтра
        if (reminderType == "first" || reminderType == "daily") {
            scheduler.scheduleNextDayReminder()
        }
    }
}
