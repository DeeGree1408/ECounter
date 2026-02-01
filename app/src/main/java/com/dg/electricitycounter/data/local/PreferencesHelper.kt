package com.dg.electricitycounter.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
    
    // ТАРИФ
    fun getTariff(): String = prefs.getString("tariff", "6.84") ?: "6.84"
    
    fun saveTariff(tariff: String) {
        prefs.edit().putString("tariff", tariff).apply()
    }
    
    fun getTariffChangeDate(): String {
        return prefs.getString("tariff_change_date", getCurrentDate()) ?: getCurrentDate()
    }
    
    fun saveTariffChangeDate(date: String) {
        prefs.edit().putString("tariff_change_date", date).apply()
    }
    
    // БЛОКИРОВКИ
    fun isTariffLocked(): Boolean = prefs.getBoolean("tariff_locked", true)
    
    fun setTariffLocked(locked: Boolean) {
        prefs.edit().putBoolean("tariff_locked", locked).apply()
    }
    
    fun isPreviousLocked(): Boolean = prefs.getBoolean("previous_locked", true)
    
    fun setPreviousLocked(locked: Boolean) {
        prefs.edit().putBoolean("previous_locked", locked).apply()
    }
    
    // НАПОМИНАНИЯ
    fun isReminderEnabled(): Boolean = prefs.getBoolean("reminder_enabled", false)
    
    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
    }
    
    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
    }
}
