package kz.example.splitbill.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Person
import kz.example.splitbill.model.Share
import kz.example.splitbill.viewmodel.BillViewModel
import java.text.NumberFormat
import java.util.Locale

private fun tenge(value: Long): String = "${NumberFormat.getNumberInstance(Locale("ru", "RU")).format(value)} ₸"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(vm: BillViewModel) {
    var addPerson by rememberSaveable { mutableStateOf(false) }
    var addItem by rememberSaveable { mutableStateOf(false) }
    var showPeople by rememberSaveable { mutableStateOf(false) }
    val shares = vm.shares

    Scaffold(
        topBar = { TopAppBar(title = { Text("SplitBill", fontWeight = FontWeight.Bold) }, actions = {
            TextButton(onClick = { showPeople = true }) { Text("Участники (${vm.people.size})") }
        }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addItem = true }
            ) {
                Text("Добавить заказ")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SummaryCard(vm.subtotal, vm.serviceTotal, vm.grandTotal) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сервисный сбор", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(0, 5, 10, 15).forEachIndexed { index, percent ->
                            SegmentedButton(
                                selected = vm.servicePercent == percent,
                                onClick = { vm.updateServicePercent(percent) },
                                shape = SegmentedButtonDefaults.itemShape(index, 4)
                            ) { Text("$percent%") }
                        }
                    }
                }
            }
            item { SectionHeading("Кто сколько платит", "${vm.people.size} чел.") }
            if (shares.isEmpty()) {
                item { Text("Добавьте участника, чтобы рассчитать счёт.") }
            } else {
                items(shares, key = { "share-${it.person.id}" }) { PersonShareCard(it) }
            }
            item { SectionHeading("Заказы", "${vm.items.size} поз.") }
            if (vm.items.isEmpty()) {
                item { Text("Пока нет заказов. Нажмите «Добавить заказ».") }
            } else {
                items(vm.items, key = { "item-${it.id}" }) { item ->
                    BillItemCard(item, vm.people.firstOrNull { it.id == item.personId }?.name ?: "На всех") {
                        vm.removeItem(item.id)
                    }
                }
            }
            item { OutlinedButton(onClick = { addPerson = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Добавить участника") } }
        }
    }
    if (addPerson) PersonDialog(onDismiss = { addPerson = false }, onAdd = {
        if (vm.addPerson(it)) addPerson = false
    })
    if (addItem) ItemDialog(people = vm.people, onDismiss = { addItem = false }, onAdd = { title, price, id ->
        if (vm.addItem(title, price, id)) addItem = false
    })
    if (showPeople) AlertDialog(
        onDismissRequest = { showPeople = false },
        title = { Text("Участники") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.people.forEach { person ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(person.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.removePerson(person.id) }) { Text("Удалить") }
                    }
                }
                Text("При удалении участника удаляются его личные заказы. Общие остаются.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showPeople = false; addPerson = true }) { Text("+ Добавить") }
            }
        },
        confirmButton = { TextButton(onClick = { showPeople = false }) { Text("Готово") } }
    )
}

@Composable
fun SummaryCard(subtotal: Long, service: Long, total: Long) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Общий счёт", style = MaterialTheme.typography.titleMedium)
            Text(tenge(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Заказы"); Text(tenge(subtotal))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Сервис"); Text(tenge(service))
            }
        }
    }
}

@Composable
fun SectionHeading(title: String, note: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PersonShareCard(share: Share) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(share.person.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(share.person.name, style = MaterialTheme.typography.titleMedium)
                Text("Заказ: ${tenge(share.itemsTotal)} · Сервис: ${tenge(share.service)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(tenge(share.total), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BillItemCard(item: BillItem, owner: String, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(owner, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            Text(tenge(item.amount), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDelete) { Text("✕") }
        }
    }
}

@Composable
fun PersonDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый участник") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, singleLine = true) },
        confirmButton = { Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun ItemDialog(people: List<Person>, onDismiss: () -> Unit, onAdd: (String, String, Int?) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var selectedPersonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val selectedPerson = people.firstOrNull { it.id == selectedPersonId }?.name ?: "На всех"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить заказ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Цена, ₸") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Кому: $selectedPerson ▾") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("На всех") }, onClick = { selectedPersonId = null; expanded = false })
                        people.forEach { person ->
                            DropdownMenuItem(text = { Text(person.name) }, onClick = { selectedPersonId = person.id; expanded = false })
                        }
                    }
                }
                Text("Общие заказы делятся поровну между участниками.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(title, price, selectedPersonId) }, enabled = title.isNotBlank() && (price.toLongOrNull() ?: 0L) in 1..100_000_000 && people.isNotEmpty()) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
