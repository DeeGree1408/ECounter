package com.dg.electricitycounter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileHelper {
    
    // СОХРАНЕНИЕ ИСТОРИИ В ФАЙЛ
    fun saveHistoryToFile(context: Context): Boolean {
        return try {
            val historyText = formatHistory()
            val fileName = "history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            
            // Сохраняем в Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            file.writeText(historyText, Charsets.UTF_8)
            
            // Уведомляем систему
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)
            
            Toast.makeText(context, "✅ Файл сохранен: $fileName", Toast.LENGTH_LONG).show()
            true
            
        } catch (e: Exception) {
            Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
    
    // ЗАГРУЗКА ИСТОРИИ ИЗ ФАЙЛА
    fun importHistoryFromFile(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.readText()
            inputStream?.close()
            
            if (content.isNullOrEmpty()) {
                Toast.makeText(context, "❌ Файл пуст", Toast.LENGTH_LONG).show()
                return false
            }
            
            // Парсим данные
            val lines = content.trim().split("\n")
            val newItems = mutableListOf<HistoryItem>()
            var nextId = 1
            var parsedCount = 0
            var errorCount = 0
            
            for ((index, line) in lines.withIndex()) {
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    try {
                        // Разделяем строку по пробелам (один или несколько)
                        val parts = trimmedLine.split("\\s+".toRegex())
                        
                        if (parts.size >= 5) {
                            val date = parts[0]
                            
                            // ЗАМЕНЯЕМ ЗАПЯТЫЕ НА ТОЧКИ для корректного парсинга чисел
                            val currentStr = parts[1]
                            val consumptionStr = parts[2]
                            val tariffStr = parts[3].replace(',', '.')  // ← ИСПРАВЛЕНИЕ: заменяем запятые на точки
                            val amountStr = parts[4].replace(',', '.')  // ← ИСПРАВЛЕНИЕ: заменяем запятые на точки
                            
                            val current = currentStr.toDouble()
                            val consumption = consumptionStr.toDouble()
                            val tariff = tariffStr.toDouble()
                            val amount = amountStr.toDouble()
                            val previous = current - consumption
                            
                            newItems.add(
                                HistoryItem(
                                    id = nextId++,
                                    date = date,
                                    readingDate = convertDate(date),
                                    previous = previous,
                                    current = current,
                                    consumption = consumption,
                                    tariff = tariff,
                                    amount = amount,
                                    address = "уч.143а"
                                )
                            )
                            parsedCount++
                        } else {
                            // Строка имеет неправильный формат
                            errorCount++
                        }
                    } catch (e: Exception) {
                        // Пропускаем некорректные строки
                        errorCount++
                        continue
                    }
                }
            }
            
            if (newItems.isEmpty()) {
                Toast.makeText(
                    context, 
                    "❌ Не найдено корректных записей\n(обработано строк: ${lines.size}, успешно: $parsedCount, ошибок: $errorCount)", 
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
            
            // Сортируем по дате (от новых к старым)
            val sortedItems = newItems.sortedByDescending { 
                parseDate(it.date) 
            }
            
            // ЗАМЕНЯЕМ всю историю
            AppState.historyItems.clear()
            AppState.historyItems.addAll(sortedItems)
            AppState.updateNextId()
            
            // Сохраняем в SharedPreferences
            DataStorage.saveHistory()
            
            // Обновляем предыдущие показания в калькуляторе
            DataStorage.updatePreviousFromHistory()
            
            Toast.makeText(
                context, 
                "✅ Импортировано $parsedCount записей\n(строк в файле: ${lines.size}, ошибок: $errorCount)", 
                Toast.LENGTH_LONG
            ).show()
            true
            
        } catch (e: Exception) {
            Toast.makeText(context, "❌ Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
    
    // КОМПОЗАБЛ ДЛЯ ВЫБОРА ФАЙЛА
    @Composable
    fun rememberFilePicker(onFileSelected: (Uri) -> Unit) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { onFileSelected(it) }
        }
    )
    
    // ПРОВЕРКА ДОСТУПНОСТИ ВНЕШНЕГО ХРАНИЛИЩА
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    // КОНВЕРТАЦИЯ ФОРМАТА ДАТЫ
    private fun convertDate(date: String): String {
        return try {
            val parts = date.split(".")
            if (parts.size == 3) "${parts[0]}/${parts[1]}/${parts[2]}" else date
        } catch (e: Exception) {
            date
        }
    }
    
    // ПАРСИНГ ДАТЫ ДЛЯ СОРТИРОВКИ
    private fun parseDate(dateStr: String): Date? {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    // ФОРМАТИРОВАНИЕ ИСТОРИИ ДЛЯ ЭКСПОРТА
    private fun formatHistory(): String {
        return AppState.historyItems.joinToString("\n") { item ->
            "${item.date} ${item.current.toInt()} ${item.consumption.toInt()} ${String.format("%.2f", item.tariff)} ${String.format("%.2f", item.amount)}"
        }
    }
    
    // ФОРМАТИРОВАНИЕ ИСТОРИИ ДЛЯ EMAIL
    fun formatHistoryForEmail(): String {
        return formatHistory()
    }
}