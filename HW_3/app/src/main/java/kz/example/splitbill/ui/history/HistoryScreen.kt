package kz.example.splitbill.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.model.Activity
import kz.example.splitbill.model.ActivityStats
import kz.example.splitbill.model.ActivityType
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.EmptyState
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.formatDate
import kz.example.splitbill.ui.components.tenge
import kz.example.splitbill.ui.theme.Negative
import kz.example.splitbill.ui.theme.Positive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenBill: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Which confirmation dialog is open. Saved, so a rotation does not close it.
    var confirmPurgeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmEmptyTrash by rememberSaveable { mutableStateOf(false) }
    var confirmClearLog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        val msg = message ?: return@LaunchedEffect
        viewModel.messageShown()
        val result = snackbar.showSnackbar(
            msg.text,
            actionLabel = if (msg.billId != null) "Открыть" else null,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && msg.billId != null) onOpenBill(msg.billId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История", fontWeight = FontWeight.Bold) },
                actions = {
                    when (tab) {
                        HistoryTab.LOG -> IconButton(onClick = { confirmClearLog = true }, enabled = state.days.isNotEmpty()) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Очистить историю")
                        }
                        HistoryTab.TRASH -> IconButton(onClick = { confirmEmptyTrash = true }, enabled = state.trash.isNotEmpty()) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Очистить корзину")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                Tab(selected = tab == HistoryTab.LOG, onClick = { viewModel.selectTab(HistoryTab.LOG) }, text = { Text("Журнал") })
                Tab(
                    selected = tab == HistoryTab.TRASH,
                    onClick = { viewModel.selectTab(HistoryTab.TRASH) },
                    text = { Text(if (state.trash.isEmpty()) "Корзина" else "Корзина · ${state.trash.size}") }
                )
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                tab == HistoryTab.LOG -> ActivityLog(state, onOpenBill)
                else -> TrashList(
                    trash = state.trash,
                    onRestore = { viewModel.restore(it.bill) },
                    onDeleteForever = { confirmPurgeId = it.bill.id }
                )
            }
        }
    }

    val purging = state.trash.firstOrNull { it.bill.id == confirmPurgeId }?.bill
    if (purging != null) ConfirmDialog(
        title = "Удалить навсегда?",
        text = "«${purging.title}» и все его позиции и возвраты будут удалены без возможности восстановления.",
        confirm = "Удалить",
        onConfirm = { viewModel.deleteForever(purging); confirmPurgeId = null },
        onDismiss = { confirmPurgeId = null }
    )
    if (confirmEmptyTrash) ConfirmDialog(
        title = "Очистить корзину?",
        text = "Счетов в корзине: ${state.trash.size}. Все они будут удалены навсегда.",
        confirm = "Очистить",
        onConfirm = { viewModel.emptyTrash(); confirmEmptyTrash = false },
        onDismiss = { confirmEmptyTrash = false }
    )
    if (confirmClearLog) ConfirmDialog(
        title = "Очистить историю?",
        text = "Журнал действий и статистика будут сброшены. Счета и друзья не изменятся.",
        confirm = "Очистить",
        onConfirm = { viewModel.clearHistory(); confirmClearLog = false },
        onDismiss = { confirmClearLog = false }
    )
}

@Composable
private fun ActivityLog(state: HistoryUiState, onOpenBill: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "stats") { StatsCard(state.stats, state.trash.size) }
        if (state.days.isEmpty()) {
            item(key = "empty") {
                EmptyState(Icons.Filled.History, "История пуста", "Здесь появятся все изменения: новые счета и позиции, возвраты долгов, удаления и восстановления.")
            }
        }
        state.days.forEach { day ->
            item(key = "day-${day.dayStart}") { SectionHeading(dayLabel(day.dayStart), "${day.entries.size} действ.") }
            items(day.entries, key = { "activity-${it.id}" }) { entry ->
                val billId = entry.billId
                ActivityRow(
                    entry = entry,
                    onClick = if (billId != null && billId in state.activeBillIds) ({ onOpenBill(billId) }) else null
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: ActivityStats, inTrash: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("За последние ${stats.days} дней", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth()) {
                Stat("Возвращено", tenge(stats.returned), Modifier.weight(1f), highlight = true)
                Stat("Возвратов", "${stats.repayments}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Stat("Новых счетов", "${stats.billsCreated}", Modifier.weight(1f))
                Stat("Действий", "${stats.actions}", Modifier.weight(1f))
                Stat("В корзине", "$inTrash", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(modifier) {
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Positive else MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActivityRow(entry: Activity, onClick: (() -> Unit)?) {
    val (icon, tint) = entry.type.visual()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    time(entry.createdAt) + if (onClick != null) " · открыть счёт" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.amount?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    tenge(it),
                    fontWeight = FontWeight.SemiBold,
                    color = when (entry.type) {
                        ActivityType.REPAID -> Positive
                        ActivityType.REPAYMENT_CANCELLED -> Negative
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun TrashList(trash: List<TrashedBill>, onRestore: (TrashedBill) -> Unit, onDeleteForever: (TrashedBill) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "hint") {
            Text(
                "Удалённые счета не учитываются в балансах. Восстановите счёт, чтобы вернуть его долги, или удалите навсегда. " +
                    "Через 30 дней счета удаляются автоматически.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trash.isEmpty()) {
            item(key = "empty") { EmptyState(Icons.Filled.DeleteOutline, "Корзина пуста", "Удалённые счета попадают сюда, и их можно восстановить.") }
        }
        items(trash, key = { it.bill.id }) { entry ->
            TrashCard(entry, onRestore = { onRestore(entry) }, onDeleteForever = { onDeleteForever(entry) })
        }
    }
}

@Composable
private fun TrashCard(entry: TrashedBill, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val bill = entry.bill
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bill.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tenge(bill.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "${bill.participants.joinToString { it.name }} · ${bill.items.size} поз.",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Удалён ${bill.deletedAt?.let(::formatDate).orEmpty()} · удалится через ${entry.daysLeft} дн.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (bill.outstandingAmount > 0) {
                Text("Невозвращённые долги: ${tenge(bill.outstandingAmount)}", style = MaterialTheme.typography.labelMedium, color = Negative)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Восстановить")
                }
                OutlinedButton(onClick = onDeleteForever, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить")
                }
            }
        }
    }
}

/** Reusable confirmation dialog for destructive actions. */
@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ActivityType.visual(): Pair<ImageVector, androidx.compose.ui.graphics.Color> {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    return when (this) {
        ActivityType.BILL_CREATED -> Icons.Filled.Receipt to primary
        ActivityType.BILL_EDITED -> Icons.Filled.Edit to primary
        ActivityType.BILL_TRASHED -> Icons.Filled.DeleteOutline to error
        ActivityType.BILL_RESTORED -> Icons.Filled.Restore to Positive
        ActivityType.BILL_PURGED -> Icons.Filled.DeleteForever to error
        ActivityType.ITEM_ADDED -> Icons.Filled.Add to primary
        ActivityType.ITEM_EDITED -> Icons.Filled.RestaurantMenu to primary
        ActivityType.ITEM_DELETED -> Icons.Filled.RemoveCircleOutline to error
        ActivityType.ITEM_RESTORED -> Icons.AutoMirrored.Filled.Undo to Positive
        ActivityType.SERVICE_CHANGED -> Icons.Filled.Percent to primary
        ActivityType.REPAID -> Icons.Filled.Payments to Positive
        ActivityType.REPAYMENT_CANCELLED -> Icons.AutoMirrored.Filled.Undo to error
        ActivityType.FRIEND_ADDED -> Icons.Filled.PersonAdd to primary
        ActivityType.FRIEND_RENAMED -> Icons.Filled.DriveFileRenameOutline to primary
        ActivityType.FRIEND_DELETED -> Icons.Filled.PersonRemove to error
    }
}

private val ru = Locale("ru", "RU")

private fun time(millis: Long): String = SimpleDateFormat("HH:mm", ru).format(Date(millis))

private fun dayLabel(dayStart: Long): String {
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        dayStart >= today -> "Сегодня"
        dayStart >= today - ActivityStats.DAY -> "Вчера"
        else -> SimpleDateFormat("d MMMM, EEEE", ru).format(Date(dayStart))
    }
}
