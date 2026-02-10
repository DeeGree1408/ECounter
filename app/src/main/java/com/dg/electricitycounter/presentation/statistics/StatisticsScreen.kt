package com.dg.electricitycounter.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 СТАТИСТИКА", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // КНОПКИ ПЕРИОДОВ
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = viewModel::onPeriodSelected
            )

            // ГРАФИК
            if (uiState.stats != null && uiState.stats!!.monthlyData.isNotEmpty()) {
                BarChartCard(
                    monthlyData = uiState.stats!!.monthlyData,
                    average = uiState.stats!!.averageConsumption
                )
            }

            // ИТОГИ
            if (uiState.stats != null) {
                SummaryCard(stats = uiState.stats!!, period = uiState.selectedPeriod)
            }

            // ПРОГНОЗ
            if (uiState.forecast != null) {
                ForecastCard(forecast = uiState.forecast!!)
            }

            // ИСТОРИЯ ТАРИФОВ
            if (uiState.tariffHistory.isNotEmpty()) {
                TariffHistoryCard(tariffHistory = uiState.tariffHistory)
            }

            // КНОПКА НАЗАД
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3C72)
                )
            ) {
                Text("← ВЕРНУТЬСЯ В ИСТОРИЮ", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit
) {
    val lastYear = Calendar.getInstance().get(Calendar.YEAR) - 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📅 ПЕРИОД",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PeriodButton("3\nмес", Period.THREE_MONTHS, selectedPeriod, onPeriodSelected, Modifier.weight(1f))
                PeriodButton("6\nмес", Period.SIX_MONTHS, selectedPeriod, onPeriodSelected, Modifier.weight(1f))
                PeriodButton("12\nмес", Period.TWELVE_MONTHS, selectedPeriod, onPeriodSelected, Modifier.weight(1f))
                PeriodButton("$lastYear\nгод", Period.LAST_YEAR, selectedPeriod, onPeriodSelected, Modifier.weight(1f))
                PeriodButton("Все", Period.ALL, selectedPeriod, onPeriodSelected, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PeriodButton(
    text: String,
    period: Period,
    selectedPeriod: Period,
    onSelected: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onSelected(period) },
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedPeriod == period) Color(0xFF1E3C72) else Color(0xFFE0E0E0),
            contentColor = if (selectedPeriod == period) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun BarChartCard(monthlyData: List<MonthData>, average: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Заголовок с padding
            Text(
                text = "📊 РАСХОД ПО МЕСЯЦАМ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // График БЕЗ padding по бокам
            BarChart(data = monthlyData, average = average)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFFF8C00), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выше среднего", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF28A745), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ниже среднего", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<MonthData>,
    average: Double
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.consumption } ?: 1.0
    val fontSize = if (data.size >= 12) 8.sp else if (data.size > 6) 10.sp else 12.sp

    Column {
        // Подпись среднего
        Text(
            text = "━ ━ ━  Средний: ${average.toInt()} кВт·ч  ━ ━ ━",
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Значения НАД столбцами
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

        data.forEach { monthData ->
                Text(
                    text = "${monthData.consumption.toInt()}",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ГРАФИК
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {

        val spacing = 4.dp.toPx()
            val barWidth = (size.width - spacing * (data.size + 1)) / data.size
            val chartHeight = size.height

            // Пунктирная линия среднего
            val averageY = (chartHeight - (average / maxValue * chartHeight).toFloat())
            drawLine(
                color = Color.Gray,
                start = Offset(0f, averageY),
                end = Offset(size.width, averageY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )

            // Столбцы
            data.forEachIndexed { index, monthData ->
                val x = spacing + index * (barWidth + spacing)
                val barHeight = (monthData.consumption / maxValue * chartHeight).toFloat()
                val y = chartHeight - barHeight

                val barColor = if (monthData.isAboveAverage) {
                    Color(0xFFFF8C00)
                } else {
                    Color(0xFF28A745)
                }

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // НАЗВАНИЯ МЕСЯЦЕВ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

        data.forEach { monthData ->
                Text(
                    text = monthData.month,
                    fontSize = if (data.size >= 12) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SummaryCard(stats: PeriodStats, period: Period) {
    val periodName = when (period) {
        Period.THREE_MONTHS -> "3 МЕСЯЦА"
        Period.SIX_MONTHS -> "6 МЕСЯЦЕВ"
        Period.TWELVE_MONTHS -> "12 МЕСЯЦЕВ"
        Period.LAST_YEAR -> "${Calendar.getInstance().get(Calendar.YEAR) - 1} ГОД"
        Period.ALL -> "ВСЁ ВРЕМЯ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F3FF)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 ИТОГИ ЗА $periodName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E3C72)
            )
            Spacer(modifier = Modifier.height(12.dp))

            StatRow("💰 Оплачено:", "${String.format("%.2f", stats.totalPaid)} ₽")
            StatRow("⚡ Израсходовано:", "${String.format("%.0f", stats.totalConsumption)} кВт·ч")
            StatRow("📈 Средний расход:", "${String.format("%.0f", stats.averageConsumption)} кВт·ч")
            StatRow("📉 Мин. расход:", "${String.format("%.0f", stats.minConsumption)} кВт·ч")
            StatRow("📈 Макс. расход:", "${String.format("%.0f", stats.maxConsumption)} кВт·ч")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF333333))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3C72))
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun ForecastCard(forecast: Forecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔮 ПРОГНОЗ НА ${forecast.nextMonth.uppercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF856404)
            )
            Spacer(modifier = Modifier.height(12.dp))

            StatRow("⚡ Ожидаемый расход:", "~${forecast.expectedConsumption} кВт·ч")
            StatRow("💰 Примерная сумма:", "~${String.format("%.2f", forecast.expectedAmount)} ₽")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ℹ️ На основе среднего за 3 месяца",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TariffHistoryCard(tariffHistory: List<TariffChange>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💵 ИСТОРИЯ ТАРИФОВ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E3C72)
            )
            Spacer(modifier = Modifier.height(12.dp))

            tariffHistory.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (change.isCurrent) "●" else "○",
                        fontSize = 20.sp,
                        color = if (change.isCurrent) Color(0xFF28A745) else Color.Gray,
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row {
                            Text(
                                text = "${String.format("%.2f", change.tariff)} ₽/кВт·ч",
                                fontSize = 14.sp,
                                fontWeight = if (change.isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "с ${change.date}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        if (change.isCurrent) {
                            Text(
                                text = "текущий",
                                fontSize = 11.sp,
                                color = Color(0xFF28A745)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
