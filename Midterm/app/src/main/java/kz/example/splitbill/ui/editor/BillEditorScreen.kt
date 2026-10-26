package kz.example.splitbill.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.bill.servicePercents
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.BackButton
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.displayName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillEditorScreen(
    onBack: () -> Unit,
    onSaved: (billId: Long, isNew: Boolean) -> Unit,
    viewModel: BillEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val savedBillId by viewModel.savedBillId.collectAsStateWithLifecycle()
    // Dialog visibility is pure UI state, so it lives in the composable and survives rotation.
    var addFriend by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedBillId) { savedBillId?.let { onSaved(it, viewModel.isNew) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Новый счёт" else "Изменить счёт") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = form.problem == null) { Text("Сохранить") }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Название") },
                placeholder = { Text("Например, ужин в кафе") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.place,
                onValueChange = viewModel::onPlaceChange,
                label = { Text("Место (необязательно)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeading("Участники", "${form.participantIds.size} выбрано")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                friends.forEach { friend ->
                    FilterChip(
                        selected = friend.id in form.participantIds,
                        onClick = { viewModel.toggleParticipant(friend.id) },
                        label = { Text(friend.displayName()) },
                        leadingIcon = { Avatar(friend, size = 24.dp) }
                    )
                }
                AssistChip(
                    onClick = { addFriend = true },
                    label = { Text("Новый друг") },
                    leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) }
                )
            }

            SectionHeading("Кто оплатил счёт")
            val participants = friends.filter { it.id in form.participantIds }
            if (participants.isEmpty()) {
                Text("Сначала выберите участников", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { friend ->
                    FilterChip(
                        selected = form.payerId == friend.id,
                        onClick = { viewModel.onPayerChange(friend.id) },
                        label = { Text(friend.displayName()) },
                        leadingIcon = if (form.payerId == friend.id) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
            Text("Остальные участники вернут свою долю этому человеку.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionHeading("Сервисный сбор")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                servicePercents.forEachIndexed { index, percent ->
                    SegmentedButton(
                        selected = form.servicePercent == percent,
                        onClick = { viewModel.onServiceChange(percent) },
                        shape = SegmentedButtonDefaults.itemShape(index, servicePercents.size)
                    ) { Text("$percent%") }
                }
            }

            form.problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = viewModel::save, enabled = form.problem == null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (viewModel.isNew) "Создать счёт" else "Сохранить изменения")
            }
        }
    }

    if (addFriend) NewFriendDialog(
        onDismiss = { addFriend = false },
        onAdd = { viewModel.addFriend(it); addFriend = false }
    )
}

@Composable
fun NewFriendDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit, initial: String = "", title: String = "Новый друг") {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Имя") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = { Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
