package com.dg.electricitycounter.presentation.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.ReminderScheduler
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import com.dg.electricitycounter.domain.usecase.AddReadingUseCase
import com.dg.electricitycounter.domain.usecase.AddReadingResult
import com.dg.electricitycounter.domain.usecase.DeleteLatestReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetAllReadingsUseCase
import com.dg.electricitycounter.domain.usecase.GetLatestReadingUseCase
import com.dg.electricitycounter.util.formatToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addReadingUseCase: AddReadingUseCase,
    private val getLatestReadingUseCase: GetLatestReadingUseCase,
    private val getAllReadingsUseCase: GetAllReadingsUseCase,
    private val deleteLatestReadingUseCase: DeleteLatestReadingUseCase,
    private val preferencesHelper: PreferencesHelper,
    private val repository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    // 💰 Отслеживание изменений членского взноса
    private var prevMembershipNumber: String = ""
    private var prevMembershipArea: String = ""
    private var prevMembershipTariff: String = ""

    init {
        loadData()
        loadMembershipSettings()
    }

    // ==========================================
    // 🔌 ЭЛЕКТРИЧЕСТВО (загрузка данных)
    // ==========================================
    fun loadData() {
        viewModelScope.launch {
            val tariff = preferencesHelper.getTariff()
            val tariffChangeDate = preferencesHelper.getTariffChangeDate()
            val isTariffLocked = preferencesHelper.isTariffLocked()
            val isPreviousLocked = preferencesHelper.isPreviousLocked()

            _uiState.update {
                it.copy(
                    tariff = tariff,
                    tariffChangeDate = tariffChangeDate,
                    isTariffLocked = isTariffLocked,
                    isPreviousLocked = isPreviousLocked
                )
            }

            getLatestReadingUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { reading ->
                    _uiState.update { state ->
                        state.copy(
                            previousReading = reading?.currentReading?.toInt()?.toString() ?: "",
                            lastReadingDate = reading?.date?.formatToDisplay() ?: "",
                            isLoading = false
                        )
                    }
                }
        }
    }

    // ==========================================
    //  ЧЛЕНСКИЙ ВЗНОС (логика + сохранение + отправка)
    // ==========================================
    fun loadMembershipSettings() {
        val prefs = context.getSharedPreferences("membership_fee_prefs", Context.MODE_PRIVATE)
        val number = prefs.getString("membership_number", "143а") ?: "143а"
        val area = prefs.getString("membership_area", "") ?: ""
        val tariff = prefs.getString("membership_tariff", "") ?: ""

        _uiState.update {
            it.copy(
                membershipPlotNumber = number,
                membershipPlotArea = area,
                membershipTariff = tariff
            )
        }
        // 🔑 Фиксируем исходные значения
        prevMembershipNumber = number
        prevMembershipArea = area
        prevMembershipTariff = tariff
        calculateMembershipFee()
    }

    fun onMembershipNumberChange(value: String) {
        _uiState.update { it.copy(membershipPlotNumber = value) }
    }
    fun onMembershipAreaChange(value: String) {
        _uiState.update { it.copy(membershipPlotArea = value) }
        calculateMembershipFee()
    }
    fun onMembershipTariffChange(value: String) {
        _uiState.update { it.copy(membershipTariff = value) }
        calculateMembershipFee()
    }

    fun toggleMembershipNumberLock() {
        val state = _uiState.value
        val newState = !state.isMembershipNumberLocked
        if (newState) prevMembershipNumber = state.membershipPlotNumber
        else if (state.membershipPlotNumber != prevMembershipNumber && state.membershipPlotNumber.isNotEmpty()) saveAndPromptMembershipEmail()
        _uiState.update { it.copy(isMembershipNumberLocked = newState) }
    }

    fun toggleMembershipAreaLock() {
        val state = _uiState.value
        val newState = !state.isMembershipAreaLocked
        if (newState) prevMembershipArea = state.membershipPlotArea
        else if (state.membershipPlotArea != prevMembershipArea && state.membershipPlotArea.isNotEmpty()) saveAndPromptMembershipEmail()
        _uiState.update { it.copy(isMembershipAreaLocked = newState) }
    }

    fun toggleMembershipTariffLock() {
        val state = _uiState.value
        val newState = !state.isMembershipTariffLocked
        if (newState) prevMembershipTariff = state.membershipTariff
        else if (state.membershipTariff != prevMembershipTariff && state.membershipTariff.isNotEmpty()) saveAndPromptMembershipEmail()
        _uiState.update { it.copy(isMembershipTariffLocked = newState) }
    }

    private fun saveAndPromptMembershipEmail() {
        val state = _uiState.value
        val prefs = context.getSharedPreferences("membership_fee_prefs", Context.MODE_PRIVATE)
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        prefs.edit()
            .putString("membership_number", state.membershipPlotNumber)
            .putString("membership_area", state.membershipPlotArea)
            .putString("membership_tariff", state.membershipTariff)
            .apply()

        _uiState.update { it.copy(membershipChangeDate = currentDate) }

        //  Визуальный отклик (чтобы точно видеть, что триггер сработал)
        Toast.makeText(context, "💾 Сохранено! Открываю почту...", Toast.LENGTH_SHORT).show()

        val monthName = getPreviousMonthName()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val emailBody = "Обновление параметров членского взноса.\n" +
                "Участок: ${state.membershipPlotNumber}\n" +
                "Период: за $monthName $year\n" +
                "Площадь: ${state.membershipPlotArea} сот.\n" +
                "Тариф: ${state.membershipTariff} ₽/сот.\n" +
                "Сумма: ${state.membershipFeeTotal}"

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
            putExtra(Intent.EXTRA_SUBJECT, "Обновление членского взноса ($currentDate)")
            putExtra(Intent.EXTRA_TEXT, emailBody)
            // ✅ Убрали FLAG_ACTIVITY_NEW_TASK, он часто блокирует Chooser
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Отправить данные о взносе"))
        } catch (e: Exception) {
            Toast.makeText(context, "⚠️ Почтовый клиент не найден. Тестируй на реальном устройстве.", Toast.LENGTH_LONG).show()
        }
    }

    fun copyMembershipToClipboard() {
        val state = _uiState.value
        val monthName = getPreviousMonthName()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

        // ✅ Точный формат по ТЗ
        val text = "Членский взнос уч.${state.membershipPlotNumber} за $monthName $year"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("membership_fee", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📋 Скопировано: $text", Toast.LENGTH_SHORT).show()
    }

    private fun calculateMembershipFee() {
        val state = _uiState.value
        val area = state.membershipPlotArea.toFloatOrNull() ?: 0f
        val tariff = state.membershipTariff.toFloatOrNull() ?: 0f
        val total = area * tariff
        _uiState.update { it.copy(membershipFeeTotal = String.format(Locale.getDefault(), "%.2f ₽", total)) }
    }

    private fun getPreviousMonthName(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        return listOf(
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
        )[calendar.get(Calendar.MONTH)]
    }

    // ==========================================
    // 🔌 ЭЛЕКТРИЧЕСТВО (ввод и отправка)
    // ==========================================
    fun onCurrentReadingChange(value: String) { _uiState.update { it.copy(currentReading = value, error = null) } }
    fun onPreviousReadingChange(value: String) { _uiState.update { it.copy(previousReading = value, error = null) } }

    fun onTariffChange(value: String) {
        val oldTariff = _uiState.value.tariff
        _uiState.update { it.copy(tariff = value, error = null) }
        preferencesHelper.saveTariff(value)
        if (oldTariff != value && value.isNotEmpty()) {
            val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            preferencesHelper.saveTariffChangeDate(currentDate)
            _uiState.update { it.copy(tariffChangeDate = currentDate) }
        }
    }

    fun toggleTariffLock() {
        val newState = !_uiState.value.isTariffLocked
        _uiState.update { it.copy(isTariffLocked = newState) }
        preferencesHelper.setTariffLocked(newState)
    }

    fun togglePreviousLock() {
        val newState = !_uiState.value.isPreviousLocked
        _uiState.update { it.copy(isPreviousLocked = newState) }
        preferencesHelper.setPreviousLocked(newState)
    }

    fun submitReading() {
        viewModelScope.launch {
            val state = _uiState.value
            val current = state.currentReading.toDoubleOrNull()
            val previous = state.previousReading.toDoubleOrNull()
            val tariff = state.tariff.toDoubleOrNull()

            if (current == null || previous == null || tariff == null) {
                _uiState.update { it.copy(error = " Заполните все поля корректными числами!") }
                return@launch
            }
            if (current < previous) {
                _uiState.update { it.copy(error = "⚠️ ВНИМАНИЕ!\nТекущие показания меньше предыдущих.\nВозможно, был сброс счётчика.", showResult = true) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            addReadingUseCase(previous = previous, current = current, tariff = tariff)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { result ->
                    when (result) {
                        is AddReadingResult.Success -> handleSuccess(result.reading, current)
                        is AddReadingResult.NeedReplacement -> _uiState.update { it.copy(showReplaceDialog = true, existingReading = result.existingReading, newReading = result.newReading, isLoading = false) }
                        is AddReadingResult.OutsidePeriod -> _uiState.update { it.copy(showPeriodErrorDialog = true, periodErrorDay = result.currentDay, isLoading = false) }
                        is AddReadingResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                }
        }
    }

    fun confirmReplacement() {
        viewModelScope.launch {
            val newReading = _uiState.value.newReading ?: return@launch
            _uiState.update { it.copy(isLoading = true, showReplaceDialog = false) }
            try {
                deleteLatestReadingUseCase()
                repository.addReading(newReading)
                handleSuccess(newReading, newReading.currentReading)
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun dismissReplaceDialog() { _uiState.update { it.copy(showReplaceDialog = false, existingReading = null, newReading = null, isLoading = false) } }
    fun dismissPeriodErrorDialog() { _uiState.update { it.copy(showPeriodErrorDialog = false, periodErrorDay = 0) } }

    private suspend fun handleSuccess(reading: Reading, current: Double) {
        _uiState.update {
            it.copy(
                currentReading = "", previousReading = current.toInt().toString(),
                lastReadingDate = reading.date.formatToDisplay(), resultText = formatResult(reading),
                showResult = true, error = null, isLoading = false, isPreviousLocked = true,
                showReplaceDialog = false, existingReading = null, newReading = null
            )
        }
        preferencesHelper.setPreviousLocked(true)
        stopRemindersIfEnabled()
        exportAndSendHistory()
    }

    private fun stopRemindersIfEnabled() {
        if (preferencesHelper.isReminderEnabled()) {
            val scheduler = ReminderScheduler(context)
            scheduler.cancelReminders()
            scheduler.scheduleReminder()
        }
    }

    private fun exportAndSendHistory() {
        viewModelScope.launch {
            try {
                val readings = getAllReadingsUseCase().first()
                if (readings.isEmpty()) return@launch
                val historyText = readings.joinToString("\n") { reading ->
                    val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(reading.date))
                    "$date ${reading.currentReading.toInt()} ${reading.consumption.toInt()} ${String.format("%.2f", reading.tariff)} ${String.format("%.2f", reading.amount)}"
                }
                val fileName = "history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                file.writeText(historyText, Charsets.UTF_8)
                val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                val uri = try { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { return@launch }

                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
                    putExtra(Intent.EXTRA_SUBJECT, "показания счётчика $currentDate")
                    putExtra(Intent.EXTRA_TEXT, "История показаний во вложении.\n\nОтправлено из приложения Электросчётчик")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(emailIntent, "Отправить историю"))
            } catch (e: Exception) { /* Игнорируем */ }
        }
    }

    private fun formatResult(reading: Reading): String {
        return """
         ПОКАЗАНИЯ ПЕРЕДАНЫ
        📈 ИЗРАСХОДОВАНО: ${String.format("%.1f", reading.consumption)} кВт·ч
        💰 ТАРИФ: ${String.format("%.2f", reading.tariff)} ₽/кВт·ч
        🏦 СУММА К ОПЛАТЕ: ${String.format("%.2f", reading.amount)} ₽
        📅 Дата передачи: ${reading.date.formatToDisplay()}
        🔄 Показания: ${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}
        ✅ Предыдущие показания обновлены | ✅ Запись в истории |  История отправлена
        ${if (preferencesHelper.isReminderEnabled()) "\n🔕 Напоминания остановлены" else ""}
        """.trimIndent()
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}