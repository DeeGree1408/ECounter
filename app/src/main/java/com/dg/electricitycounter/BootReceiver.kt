package com.dg.electricitycounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Log.d("BootReceiver", "📱 Получено событие: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d("BootReceiver", "📱 Устройство перезагружено, проверяем напоминания...")

            // ✅ ИСПРАВЛЕНО: используем правильное имя SharedPreferences
            val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
            val isReminderEnabled = prefs.getBoolean("reminder_enabled", false)

            Log.d("BootReceiver", "🔍 reminder_enabled = $isReminderEnabled")

            if (isReminderEnabled) {
                Log.d("BootReceiver", "✅ Напоминания были включены - восстанавливаем будильник!")

                // Восстанавливаем будильник
                val scheduler = ReminderScheduler(context)
                scheduler.scheduleReminder()

                Log.d("BootReceiver", "🔔 Будильник восстановлен после перезагрузки")
            } else {
                Log.d("BootReceiver", "🔕 Напоминания были выключены - ничего не делаем")
            }
        }
    }
}
