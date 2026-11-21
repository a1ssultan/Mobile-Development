package kz.example.splitbill.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Bill

enum class BillFilter(val label: String) { ALL("Все"), OPEN("Открытые"), SETTLED("Закрытые") }

data class BillsUiState(
    val loading: Boolean = true,
    val bills: List<Bill> = emptyList(),
    val totalBills: Int = 0,
    val owedToMe: Long = 0,
    val iOwe: Long = 0
)

class BillsViewModel(repository: SplitBillRepository) : ViewModel() {
    // Text field state is kept in its own flow so typing is never delayed by the database.
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(BillFilter.ALL)
    val filter: StateFlow<BillFilter> = _filter.asStateFlow()

    val uiState: StateFlow<BillsUiState> = combine(repository.bills, _query, _filter) { bills, query, filter ->
        val openDebts = bills.flatMap { it.debts }.filterNot { it.paid }
        BillsUiState(
            loading = false,
            bills = bills.filter { bill ->
                val matchesQuery = query.isBlank() || bill.title.contains(query.trim(), ignoreCase = true) ||
                    bill.place.contains(query.trim(), ignoreCase = true) ||
                    bill.participants.any { it.name.contains(query.trim(), ignoreCase = true) }
                val matchesFilter = when (filter) {
                    BillFilter.ALL -> true
                    BillFilter.OPEN -> !bill.isSettled
                    BillFilter.SETTLED -> bill.isSettled
                }
                matchesQuery && matchesFilter
            },
            totalBills = bills.size,
            owedToMe = openDebts.filter { it.to.isMe }.sumOf { it.amount },
            iOwe = openDebts.filter { it.from.isMe }.sumOf { it.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsUiState())

    fun onQueryChange(value: String) { _query.value = value }
    fun onFilterChange(value: BillFilter) { _filter.value = value }
}
