package kz.example.splitbill.ui.bill

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.model.Debt
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.BackButton
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.displayName
import kz.example.splitbill.ui.components.tenge
import kz.example.splitbill.ui.theme.Positive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleScreen(
    onBack: () -> Unit,
    viewModel: BillDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state) { if (state is BillUiState.NotFound) onBack() }
    val bill = (state as? BillUiState.Ready)?.bill

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расчёт") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (bill != null) IconButton(onClick = {
                        // Implicit intent: Android shows the share sheet (WhatsApp, Telegram, ...).
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, viewModel.shareText(bill))
                        context.startActivity(Intent.createChooser(send, "Отправить расчёт"))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Поделиться") }
                }
            )
        }
    ) { padding ->
        if (bill == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val total = bill.returnedAmount + bill.outstandingAmount
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(bill.title, style = MaterialTheme.typography.titleMedium)
                        Text("Платил(а): ${bill.payer?.displayName() ?: "—"}")
                        Text("Возвращено ${tenge(bill.returnedAmount)} из ${tenge(total)}", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { if (total == 0L) 1f else bill.returnedAmount.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                        if (bill.isSettled) Text("Все рассчитались 🎉", color = Positive, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { SectionHeading("Кто кому должен", "отметьте, когда вернули") }
            items(bill.debts, key = { it.from.id }) { debt ->
                DebtRow(debt, onCheckedChange = { viewModel.setRepaid(debt.from.id, it) })
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.markAllRepaid(bill) }, enabled = !bill.isSettled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Все вернули")
                    }
                    TextButton(onClick = viewModel::resetRepayments, enabled = bill.returnedAmount > 0) { Text("Сбросить") }
                }
            }
        }
    }
}

@Composable
private fun DebtRow(debt: Debt, onCheckedChange: (Boolean) -> Unit) {
    Card(
        onClick = { onCheckedChange(!debt.paid) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(debt.from, size = 36.dp)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "должен", modifier = Modifier.padding(horizontal = 4.dp))
            Avatar(debt.to, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${debt.from.displayName()} → ${debt.to.name}", style = MaterialTheme.typography.titleSmall)
                Text(
                    tenge(debt.amount),
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (debt.paid) TextDecoration.LineThrough else null,
                    color = if (debt.paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            Checkbox(checked = debt.paid, onCheckedChange = onCheckedChange)
        }
    }
}
