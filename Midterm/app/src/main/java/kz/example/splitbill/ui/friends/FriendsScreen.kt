package kz.example.splitbill.ui.friends

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.displayName
import kz.example.splitbill.ui.components.tenge
import kz.example.splitbill.ui.editor.NewFriendDialog
import kz.example.splitbill.ui.theme.Negative
import kz.example.splitbill.ui.theme.Positive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: FriendsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var adding by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Друзья", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Добавить") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val rows = friends
        if (rows == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Нажмите на друга, чтобы переименовать. Баланс учитывает только невозвращённые долги.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(rows, key = { it.friend.id }) { row ->
                Card(onClick = { renamingId = row.friend.id }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(row.friend)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row.friend.displayName(), style = MaterialTheme.typography.titleSmall)
                            Text("Счетов: ${row.billCount} · всего потрачено ${tenge(row.spent)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BalanceText(row.balance)
                        IconButton(onClick = { viewModel.delete(row.friend) }, enabled = !row.friend.isMe) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }

    if (adding) NewFriendDialog(onDismiss = { adding = false }, onAdd = { viewModel.add(it); adding = false })
    val renaming = friends?.firstOrNull { it.friend.id == renamingId }?.friend
    if (renaming != null) NewFriendDialog(
        title = "Переименовать",
        initial = renaming.name,
        onDismiss = { renamingId = null },
        onAdd = { viewModel.rename(renaming, it); renamingId = null }
    )
}

@Composable
private fun BalanceText(balance: Long) {
    when {
        balance > 0 -> Column(horizontalAlignment = Alignment.End) {
            Text("+${tenge(balance)}", color = Positive, fontWeight = FontWeight.Bold)
            Text("получит", style = MaterialTheme.typography.labelSmall, color = Positive)
        }
        balance < 0 -> Column(horizontalAlignment = Alignment.End) {
            Text("−${tenge(-balance)}", color = Negative, fontWeight = FontWeight.Bold)
            Text("должен(на)", style = MaterialTheme.typography.labelSmall, color = Negative)
        }
        else -> Text("0 ₸", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
