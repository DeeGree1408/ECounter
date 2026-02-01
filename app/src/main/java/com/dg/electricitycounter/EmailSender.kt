package com.dg.electricitycounter

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object EmailSender {
    
    // ПРОСТАЯ ВЕРСИЯ - отправка через Intent
    fun sendHistoryByEmail(context: Context) {
        try {
            // Формируем текст истории
            val historyText = AppState.historyItems.joinToString("\n\n") { item ->
                "Дата: ${item.date}\n" +
                "Текущие: ${item.current.toInt()}\n" +
                "Расход: ${item.consumption.toInt()} кВт·ч\n" +
                "Тариф: ${String.format("%.2f", item.tariff)} ₽\n" +
                "Сумма: ${String.format("%.2f", item.amount)} ₽"
            }
            
            // Формируем тело письма
            val emailBody = """
                История показаний счетчика:
                
                $historyText
                
                Всего записей: ${AppState.historyItems.size}
                Общий расход: ${AppState.historyItems.sumOf { it.consumption }.toInt()} кВт·ч
                Общая сумма: ${String.format("%.2f", AppState.historyItems.sumOf { it.amount })} ₽
                
                Отправлено из приложения "Электросчётчик"
            """.trimIndent()
            
            val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            
            // Создаем Intent для отправки email
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
                putExtra(Intent.EXTRA_SUBJECT, "показания счётчика $currentDate")
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }
            
            // Пытаемся найти почтовое приложение
            try {
                context.startActivity(Intent.createChooser(emailIntent, "Отправить историю"))
                Toast.makeText(context, "✅ Открывается почтовое приложение...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Нет почтового приложения", Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Экспорт в файл и отправка
    fun exportAndSendHistory(context: Context) {
        // Сначала сохраняем в файл
        val fileSaved = FileHelper.saveHistoryToFile(context)
        
        if (fileSaved) {
            // Потом отправляем email
            sendHistoryByEmail(context)
        }
    }
    
    // Форматирование для файла
    fun formatHistoryForFile(): String {
        return AppState.historyItems.joinToString("\n") { item ->
            "${item.date} ${item.current.toInt()} ${item.consumption.toInt()} ${String.format("%.2f", item.tariff)} ${String.format("%.2f", item.amount)}"
        }
    }
}