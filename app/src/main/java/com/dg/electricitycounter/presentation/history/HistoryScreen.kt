package com.dg.electricitycounter.presentation.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.util.formatToDisplay
import androidx.compose.material.icons.filled.BarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Показываем ошибки через Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    // Диалог удаления
    if (uiState.showDeleteDialog && uiState.readings.isNotEmpty()) {
        DeleteConfirmationDialog(
            reading = uiState.readings.first(),
            onConfirm = viewModel::deleteLatestReading,
            onDismiss = viewModel::hideDeleteDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🕒 ИСТОРИЯ", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = onNavigateToStatistics,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3C72)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "СТАТИСТИКА",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // СТАТИСТИКА
            val stats = uiState.readings.toStats()
            StatisticsCard(stats)

            Spacer(modifier = Modifier.height(8.dp))

            // КНОПКА НАЗАД
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3C72)
                )
            ) {
                Text("<-- ВЕРНУТЬСЯ", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // СПИСОК ИСТОРИИ
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.readings.isEmpty()) {
                EmptyHistoryCard()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(uiState.readings) { index, reading ->
                        HistoryCard(
                            reading = reading,
                            isLatest = index == 0,
                            onDeleteClick = if (index == 0) viewModel::showDeleteDialog else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(stats: HistoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F3FF)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {  // Ещё меньше
            // Заголовок
            Text(
                text = "📊 ИТОГИ ЗА ВСЁ ВРЕМЯ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО! Сжимает высоту строки
                color = Color(0xFF1E3C72)
            )

            // Период данных (БЕЗ Spacer!)
            if (stats.firstDate.isNotEmpty() && stats.lastDate.isNotEmpty()) {
                Text(
                    text = "🗓️ с ${stats.firstDate} по ${stats.lastDate} (${stats.monthsCount} месяцев)",
                    fontSize = 12.sp,
                    lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)  // Минимальный отступ
                )
            }

            Spacer(modifier = Modifier.height(6.dp))  // Только перед списком

            // Статистика (ВООБЩЕ БЕЗ vertical padding!)
            StatRow("📝 Записей:", "${stats.recordsCount}")
            StatRow("💰 Оплачено:", "${String.format("%.2f", stats.totalPaid)} ₽")
            StatRow("⚡ Израсходовано:", "${String.format("%.0f", stats.totalConsumption)} кВт·ч")
            StatRow("🔄 В среднем/месяц:", "${String.format("%.0f", stats.averageConsumption)} кВт·ч")
            StatRow("🎯 В среднем/год:", "${String.format("%.0f", stats.averagePerYear)} кВт·ч")
            StatRow("📉 Мин. расход:", "${String.format("%.0f", stats.minConsumption)} кВт·ч")
            StatRow("📈 Макс. расход:", "${String.format("%.0f", stats.maxConsumption)} кВт·ч")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),  // 🔥 УБРАЛ padding(vertical)!
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            lineHeight = 1.4.em,  // 🔥 ДОБАВЛЕНО! Сжимает текст
            color = Color(0xFF333333)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 1.4.em,  // 🔥 ДОБАВЛЕНО!
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3C72)
        )
    }
}

@Composable
fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📭",
                fontSize = 36.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ИСТОРИЯ ПУСТА",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Выполните расчёт на главном экране,\nчтобы добавить запись",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryCard(
    reading: Reading,
    isLatest: Boolean,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)  // Уменьшено с 10dp
        ) {
            // ЗАГОЛОВОК
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLatest) "📅 ${reading.date.formatToDisplay()} ⭐" else "📅 ${reading.date.formatToDisplay()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                    color = if (isLatest) Color(0xFFDC3545) else Color(0xFF1E3C72)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLatest) {
                        Text(
                            text = "УДАЛИТЬ ЗАПИСЬ",
                            fontSize = 10.sp,
                            lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )

                        if (onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить последнюю запись",
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ОСНОВНАЯ ИНФОРМАЦИЯ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ПОКАЗАНИЯ",
                        fontSize = 10.sp,
                        lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                        color = Color.Gray
                    )
                    Text(
                        text = "${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}",
                        fontSize = 16.sp,
                        lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "РАСХОД",
                        fontSize = 10.sp,
                        lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.0f", reading.consumption)} кВт·ч",
                        fontSize = 16.sp,
                        lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF28A745)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ДЕТАЛИ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ТАРИФ",
                        fontSize = 10.sp,
                        lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", reading.tariff)} ₽",
                        fontSize = 12.sp,
                        lineHeight = 1.3.em  // 🔥 ДОБАВЛЕНО!
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "СУММА",
                        fontSize = 10.sp,
                        lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", reading.amount)} ₽",
                        fontSize = 14.sp,
                        lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC3545)
                    )
                }
            }

            // СТРОКА ДЛЯ БАНКА (только для последней записи)
            if (isLatest) {
                Spacer(modifier = Modifier.height(6.dp))

                val bankString = "Эл-во ${reading.address} - расход ${reading.consumption.toInt()} кВт, показания ${reading.currentReading.toInt()} на ${reading.date.formatToDisplay()}"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)  // Уменьшено с 8dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 ДЛЯ БАНКА",
                                fontSize = 11.sp,
                                lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Для банка", bankString)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Скопировано для банка!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.CopyAll,
                                    contentDescription = "Копировать",
                                    tint = Color(0xFF1E3C72),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = bankString,
                            fontSize = 12.sp,
                            lineHeight = 1.3.em,  // 🔥 ДОБАВЛЕНО!
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Нажмите на кнопку справа для копирования",
                            fontSize = 9.sp,
                            lineHeight = 1.2.em,  // 🔥 ДОБАВЛЕНО!
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    reading: Reading,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🗑️ УДАЛИТЬ ПОСЛЕДНЮЮ ЗАПИСЬ?", fontSize = 16.sp)
        },
        text = {
            Column {
                Text("Вы уверены, что хотите удалить последнюю запись из истории?", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${reading.date.formatToDisplay()}: ${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Расход: ${String.format("%.0f", reading.consumption)} кВт·ч",
                    fontSize = 12.sp
                )
                Text(
                    text = "Сумма: ${String.format("%.2f", reading.amount)} ₽",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Эта операция необратима!",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text("УДАЛИТЬ", fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp)
            ) {
                Text("ОТМЕНА", fontSize = 12.sp)
            }
        }
    )
}
