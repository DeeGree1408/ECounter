package com.dg.electricitycounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "📱 Получено событие: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d("BootReceiver", "📱 Устройство перезагружено, проверяем напоминания...")

            val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
            val isReminderEnabled = prefs.getBoolean("reminder_enabled", false)

            Log.d("BootReceiver", "🔍 reminder_enabled = $isReminderEnabled")

            if (isReminderEnabled) {
                Log.d("BootReceiver", "✅ Напоминания включены - восстанавливаем будильник!")

                // Запускаем корутину для работы с БД (IO поток)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = com.dg.electricitycounter.data.AppDatabase.getInstance(context)
                        val lastDateMillis = db.readingDao().getLatest()?.date

                        // Переключаемся на Main поток для безопасного вызова планировщика
                        withContext(Dispatchers.Main) {
                            ReminderScheduler(context).scheduleReminder(lastDateMillis)
                            Log.d("BootReceiver", "🔔 Будильник восстановлен (дата БД: ${lastDateMillis ?: "null"})")
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "❌ Ошибка восстановления будильника", e)
                        // Фоллбэк: если БД недоступна, планируем по текущей дате
                        ReminderScheduler(context).scheduleReminder()
                    }
                }
            } else {
                Log.d("BootReceiver", "🔕 Напоминания выключены")
            }
        }
    }
}