package com.dg.electricitycounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "=== onReceive START ===")
        Log.d(TAG, "📨 Action: ${intent.action}")
        Log.d(TAG, "⏰ Текущее время: ${Date()}")

        when (intent.action) {
            ReminderScheduler.ACTION_REMINDER -> {
                Log.d(TAG, "🔔 Получен ACTION_REMINDER")
                handleReminder(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "🔄 Устройство перезагружено")
                restoreReminders(context)
            }
            else -> {
                Log.w(TAG, "⚠️ Неизвестный action: ${intent.action}")
            }
        }

        Log.d(TAG, "=== onReceive END ===")
    }

    private fun handleReminder(context: Context) {
        Log.d(TAG, "--- handleReminder START ---")
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        Log.d(TAG, "📅 Текущий день месяца: $currentDay")

        // Проверяем, вводились ли показания в этом месяце
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastReadingDate = prefs.getLong("last_reading_date", 0)
        
        val lastReadingCalendar = Calendar.getInstance().apply {
            timeInMillis = lastReadingDate
        }
        
        val hasReadingThisMonth = lastReadingCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                   lastReadingCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)

        Log.d(TAG, "📊 Последние показания: ${Date(lastReadingDate)}")
        Log.d(TAG, "✅ Показания введены в этом месяце: $hasReadingThisMonth")

        if (hasReadingThisMonth) {
            Log.d(TAG, "✅ Показания уже введены - пропускаем уведомление")
            return
        }

        // Показываем уведомление
        Log.d(TAG, "📢 Отправляем уведомление...")
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showReminderNotification() // ✅ БЕЗ ПАРАМЕТРОВ
        Log.d(TAG, "✅ Уведомление отправлено")

        // Планируем следующее напоминание
        if (currentDay == 24) {
            Log.d(TAG, "📅 День 24 - планируем ежедневные напоминания")
            val scheduler = ReminderScheduler(context)
            scheduler.scheduleDailyReminders()
        } else {
            Log.d(TAG, "📅 День >= 25 - продолжаем ежедневные напоминания")
            val scheduler = ReminderScheduler(context)
            scheduler.scheduleDailyReminders()
        }

        Log.d(TAG, "--- handleReminder END ---")
    }

    private fun restoreReminders(context: Context) {
        Log.d(TAG, "--- restoreReminders START ---")
        
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val remindersEnabled = prefs.getBoolean("reminders_enabled", false)
        
        Log.d(TAG, "🔔 Напоминания включены: $remindersEnabled")

        if (remindersEnabled) {
            val scheduler = ReminderScheduler(context)
            scheduler.scheduleReminder()
            Log.d(TAG, "✅ Напоминания восстановлены после перезагрузки")
        }

        Log.d(TAG, "--- restoreReminders END ---")
    }
}
