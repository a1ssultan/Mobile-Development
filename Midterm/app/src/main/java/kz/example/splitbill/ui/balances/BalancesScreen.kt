package kz.example.splitbill.ui.balances

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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.model.Friend
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.EmptyState
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.tenge
import kz.example.splitbill.ui.theme.Negative
import kz.example.splitbill.ui.theme.Positive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    onBillClick: (Long) -> Unit,
    viewModel: BalancesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Балансы", fontWeight = FontWeight.Bold) }) }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { MyBalanceCard(state.myBalance) }
            if (state.openDebts.isEmpty()) {
                item { EmptyState(Icons.Filled.Celebration, "Все в расчёте", "Открытых долгов нет. Новые появятся, когда вы создадите счёт.") }
                return@LazyColumn
            }
            item { SectionHeading("Как рассчитаться проще", "${state.transfers.size} перевод(а)") }
            item {
                Text(
                    "Долги по всем счетам сведены вместе: вместо ${state.openDebts.size} переводов достаточно ${state.transfers.size}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.transfers) { TransferRow(it.from, it.to, it.amount, subtitle = null, onClick = null) }

            item { SectionHeading("Открытые долги по счетам", "${state.openDebts.size}") }
            items(state.openDebts, key = { "${it.billId}-${it.from.id}" }) { debt ->
                TransferRow(debt.from, debt.to, debt.amount, subtitle = debt.billTitle, onClick = { onBillClick(debt.billId) })
            }
        }
    }
}

@Composable
private fun MyBalanceCard(balance: Long) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Мой баланс", style = MaterialTheme.typography.labelLarge)
            Text(
                when {
                    balance > 0 -> "+${tenge(balance)}"
                    balance < 0 -> "−${tenge(-balance)}"
                    else -> tenge(0)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    balance > 0 -> Positive
                    balance < 0 -> Negative
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Text(
                when {
                    balance > 0 -> "Вам должны вернуть"
                    balance < 0 -> "Вы должны вернуть"
                    else -> "Вы никому не должны, и вам тоже"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TransferRow(from: Friend, to: Friend, amount: Long, subtitle: String?, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(from, size = 32.dp)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "переводит", modifier = Modifier.padding(horizontal = 4.dp))
            Avatar(to, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${from.name} → ${to.name}", style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(tenge(amount), fontWeight = FontWeight.Bold)
        }
    }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    if (onClick != null) Card(onClick = onClick, colors = colors) { content() } else Card(colors = colors) { content() }
}
