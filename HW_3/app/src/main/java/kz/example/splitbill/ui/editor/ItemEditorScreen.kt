package kz.example.splitbill.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.example.splitbill.model.BillCalculator
import kz.example.splitbill.ui.AppViewModelProvider
import kz.example.splitbill.ui.components.Avatar
import kz.example.splitbill.ui.components.BackButton
import kz.example.splitbill.ui.components.SectionHeading
import kz.example.splitbill.ui.components.tenge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditorScreen(
    onDone: () -> Unit,
    viewModel: ItemEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val addedCount by viewModel.addedCount.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }

    val everyone = form.sharedBy.isEmpty()
    val sharers = if (everyone) participants else participants.filter { it.id in form.sharedBy }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (viewModel.isNew) "Новая позиция" else "Изменить позицию") }, navigationIcon = { BackButton(onDone) })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (addedCount > 0) {
                Text("Добавлено позиций: $addedCount. Введите следующую или вернитесь назад.", color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Название") },
                placeholder = { Text("Например, паста") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.price,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text("Цена, ₸") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { viewModel.changeQuantity(-1) }, enabled = form.quantity > 1) {
                    Icon(Icons.Filled.Remove, contentDescription = "Меньше")
                }
                Text("× ${form.quantity}", style = MaterialTheme.typography.titleMedium)
                FilledTonalIconButton(onClick = { viewModel.changeQuantity(1) }, enabled = form.quantity < MAX_QUANTITY) {
                    Icon(Icons.Filled.Add, contentDescription = "Больше")
                }
            }

            SectionHeading("Кто разделяет", if (everyone) "все" else "${sharers.size} из ${participants.size}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = everyone,
                    onClick = viewModel::shareWithEveryone,
                    label = { Text("Все") },
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) }
                )
                participants.forEach { friend ->
                    FilterChip(
                        selected = !everyone && friend.id in form.sharedBy,
                        onClick = { viewModel.toggleSharer(friend.id) },
                        label = { Text(friend.name) },
                        leadingIcon = { Avatar(friend, size = 24.dp) }
                    )
                }
            }

            // Live preview of how this item is split, using the same calculator as the bill.
            // remember(keys): recalculated only when the amount or the people change, not on every recomposition.
            val parts = remember(form.amount, sharers) {
                if (sharers.isEmpty()) emptyMap() else BillCalculator.splitEvenly(form.amount, sharers.map { it.id })
            }
            if (form.amount > 0 && sharers.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Итого: ${tenge(form.amount)}", fontWeight = FontWeight.Bold)
                        sharers.forEach { Text("${it.name}: ${tenge(parts.getValue(it.id))}", style = MaterialTheme.typography.bodyMedium) }
                        Text("Без учёта сервисного сбора", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            form.problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(addAnother = false) }, enabled = form.problem == null, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isNew) "Добавить" else "Сохранить")
            }
            if (viewModel.isNew) {
                OutlinedButton(onClick = { viewModel.save(addAnother = true) }, enabled = form.problem == null, modifier = Modifier.fillMaxWidth()) {
                    Text("Добавить и ввести ещё")
                }
            }
        }
    }
}
