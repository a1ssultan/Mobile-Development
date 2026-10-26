package kz.example.splitbill.ui.bill

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Bill
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.ui.components.formatDate
import kz.example.splitbill.ui.components.tenge

sealed interface BillUiState {
    data object Loading : BillUiState
    data object NotFound : BillUiState
    data class Ready(val bill: Bill) : BillUiState
}

/** Used by both BillDetailRoute and SettleRoute: both carry a "billId" argument. */
class BillDetailViewModel(savedStateHandle: SavedStateHandle, private val repository: SplitBillRepository) : ViewModel() {
    private val billId: Long = checkNotNull(savedStateHandle["billId"])

    val uiState: StateFlow<BillUiState> = repository.bill(billId)
        .map { bill -> if (bill == null) BillUiState.NotFound else BillUiState.Ready(bill) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillUiState.Loading)

    fun setServicePercent(percent: Int) = viewModelScope.launch { repository.setServicePercent(billId, percent) }

    fun deleteItem(item: BillItem) = viewModelScope.launch { repository.deleteItem(item.id) }

    /** Undo for the snackbar: the item is saved again with the same data. */
    fun restoreItem(item: BillItem) = viewModelScope.launch {
        repository.saveItem(billId, null, item.title, item.price, item.quantity, item.sharedBy)
    }

    fun deleteBill() = viewModelScope.launch { repository.deleteBill(billId) }

    fun setRepaid(friendId: Long, repaid: Boolean) = viewModelScope.launch { repository.setRepaid(billId, friendId, repaid) }

    fun markAllRepaid(bill: Bill) = viewModelScope.launch {
        bill.debts.filterNot { it.paid }.forEach { repository.setRepaid(billId, it.from.id, true) }
    }

    fun resetRepayments() = viewModelScope.launch { repository.clearRepayments(billId) }

    /** Plain text that can be sent to a group chat. */
    fun shareText(bill: Bill): String = buildString {
        appendLine("🧾 ${bill.title}" + if (bill.place.isNotBlank()) " — ${bill.place}" else "")
        appendLine(formatDate(bill.createdAt))
        appendLine("Итого: ${tenge(bill.total)} (сервис ${bill.servicePercent}%: ${tenge(bill.serviceTotal)})")
        appendLine()
        bill.shares.forEach { appendLine("• ${it.person.name}: ${tenge(it.total)}") }
        if (bill.payer != null && bill.debts.isNotEmpty()) {
            appendLine()
            appendLine("Платил(а) ${bill.payer.name}. Вернуть:")
            bill.debts.forEach { appendLine("${if (it.paid) "✅" else "⏳"} ${it.from.name} → ${tenge(it.amount)}") }
        }
        append("Посчитано в SplitBill")
    }
}
