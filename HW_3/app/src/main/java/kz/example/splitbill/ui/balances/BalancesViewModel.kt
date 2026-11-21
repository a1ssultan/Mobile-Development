package kz.example.splitbill.ui.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Balance
import kz.example.splitbill.model.Balances
import kz.example.splitbill.model.Debt
import kz.example.splitbill.model.Transfer

data class BalancesUiState(
    val loading: Boolean = true,
    val myBalance: Long = 0,
    val balances: List<Balance> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val openDebts: List<Debt> = emptyList()
)

class BalancesViewModel(repository: SplitBillRepository) : ViewModel() {
    val uiState: StateFlow<BalancesUiState> = repository.bills.map { bills ->
        val openDebts = bills.flatMap { it.debts }.filterNot { it.paid }
        val balances = Balances.net(openDebts)
        BalancesUiState(
            loading = false,
            myBalance = balances.firstOrNull { it.person.isMe }?.amount ?: 0,
            balances = balances,
            transfers = Balances.simplify(balances),
            openDebts = openDebts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalancesUiState())
}
