package kz.example.splitbill.ui.bill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kz.example.splitbill.model.Bill
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Share
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.BackButton
import kz.example.splitbill.ui.components.EmptyState
import kz.example.splitbill.ui.components.MoneyRow
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.displayName
import kz.example.splitbill.ui.components.formatDate
import kz.example.splitbill.ui.components.tenge

val servicePercents = listOf(0, 5, 10, 15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddItem: (Long) -> Unit,
    onEditItem: (billId: Long, itemId: Long) -> Unit,
    onSettle: (Long) -> Unit,
    viewModel: BillDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // The bill was deleted (here or from another screen): leave the screen.
    LaunchedEffect(state) { if (state is BillUiState.NotFound) onBack() }

    val bill = (state as? BillUiState.Ready)?.bill
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bill?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (bill != null) {
                        IconButton(onClick = { onEdit(bill.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Изменить счёт") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Удалить счёт") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (bill != null) FloatingActionButton(onClick = { onAddItem(bill.id) }) { Icon(Icons.Filled.Add, contentDescription = "Добавить позицию") }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (bill == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TotalCard(bill, onSettle = { onSettle(bill.id) }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading("Сервисный сбор")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        servicePercents.forEachIndexed { index, percent ->
                            SegmentedButton(
                                selected = bill.servicePercent == percent,
                                onClick = { viewModel.setServicePercent(percent) },
                                shape = SegmentedButtonDefaults.itemShape(index, servicePercents.size)
                            ) { Text("$percent%") }
                        }
                    }
                }
            }
            item { SectionHeading("Кто сколько платит", "${bill.participants.size} чел.") }
            items(bill.shares, key = { "share-${it.person.id}" }) { share -> ShareCard(share, isPayer = share.person.id == bill.payer?.id) }
            item { SectionHeading("Позиции", "${bill.items.size} поз.") }
            if (bill.items.isEmpty()) {
                item { EmptyState(Icons.Filled.RestaurantMenu, "Нет позиций", "Нажмите «+», чтобы добавить блюдо или напиток из чека.") }
            }
            items(bill.items, key = { "item-${it.id}" }) { item ->
                ItemCard(
                    item = item,
                    owners = sharedByText(bill, item),
                    onClick = { onEditItem(bill.id, item.id) },
                    onDelete = {
                        viewModel.deleteItem(item)
                        scope.launch {
                            val result = snackbar.showSnackbar("«${item.title}» удалено", actionLabel = "Вернуть", withDismissAction = true)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreItem(item)
                        }
                    }
                )
            }
        }
    }

    if (confirmDelete && bill != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Удалить счёт?") },
        text = { Text("«${bill.title}» и все его позиции будут удалены без возможности восстановления.") },
        confirmButton = {
            Button(onClick = { confirmDelete = false; viewModel.deleteBill() }) { Text("Удалить") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
    )
}

private fun sharedByText(bill: Bill, item: BillItem): String {
    val names = bill.participants.filter { it.id in item.sharedBy }.map { it.name }
    return when {
        names.isEmpty() || names.size == bill.participants.size -> "На всех"
        names.size == 1 -> names.single()
        else -> names.joinToString(", ") + " · пополам".takeIf { names.size == 2 }.orEmpty()
    }
}

@Composable
private fun TotalCard(bill: Bill, onSettle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listOf(formatDate(bill.createdAt), bill.place).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            Text(tenge(bill.total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            MoneyRow("Позиции", bill.subtotal)
            MoneyRow("Сервис ${bill.servicePercent}%", bill.serviceTotal)
            Text("Платил(а): ${bill.payer?.displayName() ?: "не указан"}", style = MaterialTheme.typography.bodyMedium)
            if (bill.debts.isNotEmpty()) {
                Button(onClick = onSettle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Payments, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (bill.isSettled) "Все рассчитались ✓" else "Рассчитаться · осталось ${tenge(bill.outstandingAmount)}")
                }
            } else if (bill.payer == null) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Укажите, кто платил, чтобы посчитать долги") }
            }
        }
    }
}

@Composable
private fun ShareCard(share: Share, isPayer: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(share.person)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(share.person.displayName() + if (isPayer) " · платил(а)" else "", style = MaterialTheme.typography.titleSmall)
                Text("Заказ: ${tenge(share.itemsTotal)} · Сервис: ${tenge(share.service)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(tenge(share.total), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ItemCard(item: BillItem, owners: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(owners, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(tenge(item.amount), fontWeight = FontWeight.SemiBold)
                if (item.quantity > 1) Text("${item.quantity} × ${tenge(item.price)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Удалить позицию") }
        }
    }
}
