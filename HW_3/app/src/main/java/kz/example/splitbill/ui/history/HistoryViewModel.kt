package kz.example.splitbill.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Activity
import kz.example.splitbill.model.ActivityStats
import kz.example.splitbill.model.Bill
import java.util.Calendar

enum class HistoryTab { LOG, TRASH }

/** Activities of one calendar day; dayStart is used as a stable key for the list header. */
data class DayGroup(val dayStart: Long, val entries: List<Activity>)

data class TrashedBill(val bill: Bill, val daysLeft: Int)

data class HistoryUiState(
    val loading: Boolean = true,
    val days: List<DayGroup> = emptyList(),
    val stats: ActivityStats = ActivityStats(7, 0, 0, 0, 0),
    val trash: List<TrashedBill> = emptyList(),
    /** Bills that still exist and are not in the trash: only their log entries can be opened. */
    val activeBillIds: Set<Long> = emptySet()
)

/** One-off feedback for the snackbar. billId is set when the snackbar can offer "Open". */
data class HistoryMessage(val text: String, val billId: Long? = null)

class HistoryViewModel(private val repository: SplitBillRepository) : ViewModel() {

    private val _tab = MutableStateFlow(HistoryTab.LOG)
    val tab: StateFlow<HistoryTab> = _tab.asStateFlow()

    private val _message = MutableStateFlow<HistoryMessage?>(null)
    val message: StateFlow<HistoryMessage?> = _message.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(repository.activity, repository.trash, repository.bills) { log, trash, bills ->
        val now = repository.now()
        HistoryUiState(
            loading = false,
            days = log.groupBy { startOfDay(it.createdAt) }.map { (day, entries) -> DayGroup(day, entries) },
            stats = ActivityStats.of(log, now),
            trash = trash.map { bill ->
                val deletedFor = now - (bill.deletedAt ?: now)
                TrashedBill(bill, (SplitBillRepository.TRASH_DAYS - (deletedFor / ActivityStats.DAY).toInt()).coerceAtLeast(1))
            },
            activeBillIds = bills.map { it.id }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectTab(tab: HistoryTab) { _tab.value = tab }

    fun messageShown() { _message.value = null }

    fun restore(bill: Bill) = viewModelScope.launch {
        repository.restoreBill(bill.id)
        _message.value = HistoryMessage("«${bill.title}» восстановлен", billId = bill.id)
    }

    fun deleteForever(bill: Bill) = viewModelScope.launch {
        repository.deleteBillForever(bill.id)
        _message.value = HistoryMessage("«${bill.title}» удалён навсегда")
    }

    fun emptyTrash() = viewModelScope.launch {
        val count = repository.emptyTrash()
        _message.value = HistoryMessage(if (count > 0) "Корзина очищена" else "Корзина уже пуста")
    }

    fun clearHistory() = viewModelScope.launch {
        repository.clearHistory()
        _message.value = HistoryMessage("История очищена")
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().run {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}
