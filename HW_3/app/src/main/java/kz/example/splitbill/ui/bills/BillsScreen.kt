package kz.example.splitbill.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.model.Bill
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Friend
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.EmptyState
import kz.example.splitbill.ui.components.formatDate
import kz.example.splitbill.ui.components.tenge
import kz.example.splitbill.ui.theme.Negative
import kz.example.splitbill.ui.theme.Positive
import kz.example.splitbill.ui.theme.SplitBillTheme

/** Stateful wrapper: connects the ViewModel to the stateless [BillsContent]. */
@Composable
fun BillsScreen(
    onBillClick: (Long) -> Unit,
    onNewBill: () -> Unit,
    viewModel: BillsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    BillsContent(
        state = state,
        query = query,
        filter = filter,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onBillClick = onBillClick,
        onNewBill = onNewBill
    )
}

/**
 * State hoisting: this composable owns no state. It receives values and reports events up,
 * so it can be reused and previewed without a ViewModel or database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsContent(
    state: BillsUiState,
    query: String,
    filter: BillFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (BillFilter) -> Unit,
    onBillClick: (Long) -> Unit,
    onNewBill: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SplitBill", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewBill,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Новый счёт") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DebtSummaryCard(state.owedToMe, state.iOwe) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск: счёт, место, друг") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Очистить")
                        }
                    },
                    singleLine = true
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BillFilter.entries) { option ->
                        FilterChip(selected = filter == option, onClick = { onFilterChange(option) }, label = { Text(option.label) })
                    }
                }
            }
            when {
                state.loading -> item {
                    Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                }
                state.totalBills == 0 -> item {
                    EmptyState(Icons.Filled.Receipt, "Пока нет счетов", "Создайте первый счёт: добавьте друзей и позиции, а приложение посчитает, кто сколько должен.")
                }
                state.bills.isEmpty() -> item {
                    EmptyState(Icons.Filled.SearchOff, "Ничего не найдено", "Попробуйте изменить запрос или фильтр.")
                }
                else -> items(state.bills, key = { it.id }) { bill -> BillCard(bill, onClick = { onBillClick(bill.id) }) }
            }
        }
    }
}

@Composable
private fun DebtSummaryCard(owedToMe: Long, iOwe: Long) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Вам должны", style = MaterialTheme.typography.labelLarge)
                Text(tenge(owedToMe), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Positive)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Вы должны", style = MaterialTheme.typography.labelLarge)
                Text(tenge(iOwe), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (iOwe > 0) Negative else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun BillCard(bill: Bill, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(bill.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(formatDate(bill.createdAt), bill.place).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(tenge(bill.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "${bill.participants.size} чел. · ${bill.items.size} поз. · платил(а): ${bill.payer?.name ?: "не указан"}",
                style = MaterialTheme.typography.bodySmall
            )
            if (bill.isSettled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Positive, modifier = Modifier.padding(end = 4.dp))
                    Text("Все рассчитались", color = Positive, style = MaterialTheme.typography.labelLarge)
                }
            } else if (bill.debts.isNotEmpty()) {
                val returned = bill.returnedAmount.toFloat() / (bill.returnedAmount + bill.outstandingAmount)
                LinearProgressIndicator(progress = { returned }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                Text("Осталось вернуть: ${tenge(bill.outstandingAmount)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BillsContentPreview() {
    val me = Friend(1, "Я", isMe = true)
    val ali = Friend(2, "Али")
    val bill = Bill(
        id = 1, title = "Ужин в кафе", place = "Алматы", createdAt = 0, servicePercent = 10, payer = me,
        participants = listOf(me, ali), items = listOf(BillItem(1, "Пицца", 6000, 1)), paidBack = emptySet()
    )
    SplitBillTheme {
        BillsContent(
            state = BillsUiState(loading = false, bills = listOf(bill), totalBills = 1, owedToMe = bill.outstandingAmount),
            query = "",
            filter = BillFilter.ALL,
            onQueryChange = {}, onFilterChange = {}, onBillClick = {}, onNewBill = {}
        )
    }
}
