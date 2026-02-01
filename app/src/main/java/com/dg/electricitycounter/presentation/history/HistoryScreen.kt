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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.util.formatToDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
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
                    Text("📊 ИСТОРИЯ РАСЧЁТОВ", fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState.readings.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::showDeleteDialog
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить последнюю запись",
                                tint = Color.Red
                            )
                        }
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
                Text("← ВЕРНУТЬСЯ", fontSize = 12.sp)
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
                            isLatest = index == 0
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F3FF)
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 СТАТИСТИКА",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3C72),
                    fontSize = 16.sp
                )
                Text(
                    text = "6,84 ₽/кВт·ч",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3C72),
                    fontWeight = FontWeight.Medium
                )
            }

            // Первая строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("${String.format("%.2f", stats.totalPaid)} ₽", "Оплачено")
                StatItem("${String.format("%.0f", stats.totalConsumption)}", "Всего кВт·ч")
                StatItem("${stats.recordsCount}", "Расчётов")
            }

            // Вторая строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    "${String.format("%.0f", stats.averageConsumption)} кВт·ч",
                    "В среднем в месяц"
                )
                StatItem(
                    "${String.format("%.0f", stats.averagePerYear)} кВт·ч",
                    "В среднем в год"
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
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
    isLatest: Boolean
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
                .padding(10.dp)
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
                    color = if (isLatest) Color(0xFFDC3545) else Color(0xFF1E3C72)
                )
                
                if (isLatest) {
                    Text(
                        text = "ПОСЛЕДНЯЯ",
                        fontSize = 10.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
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
                        color = Color.Gray
                    )
                    Text(
                        text = "${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "РАСХОД",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.0f", reading.consumption)} кВт·ч",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF28A745)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // ДЕТАЛИ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ТАРИФ",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", reading.tariff)} ₽",
                        fontSize = 12.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "СУММА",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", reading.amount)} ₽",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC3545)
                    )
                }
            }
            
            // СТРОКА ДЛЯ БАНКА (только для последней записи)
            if (isLatest) {
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 ДЛЯ БАНКА",
                                fontSize = 11.sp,
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
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = bankString,
                            fontSize = 12.sp,
                            color = Color(0xFF333333)
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = "Нажмите на кнопку справа для копирования",
                            fontSize = 9.sp,
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
