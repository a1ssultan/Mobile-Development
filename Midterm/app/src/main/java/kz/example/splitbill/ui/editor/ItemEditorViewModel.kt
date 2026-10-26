package kz.example.splitbill.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Friend
import kz.example.splitbill.ui.navigation.ItemEditorRoute

const val MAX_PRICE = 10_000_000L
const val MAX_QUANTITY = 99

data class ItemForm(
    val title: String = "",
    val price: String = "",
    val quantity: Int = 1,
    val sharedBy: Set<Long> = emptySet() // empty = everyone
) {
    val priceValue: Long? get() = price.toLongOrNull()
    val amount: Long get() = (priceValue ?: 0) * quantity
    val problem: String?
        get() {
            val price = priceValue
            return when {
                title.isBlank() -> "Введите название"
                price == null || price <= 0 -> "Введите цену"
                price > MAX_PRICE -> "Слишком большая цена"
                else -> null
            }
        }
}

class ItemEditorViewModel(savedStateHandle: SavedStateHandle, private val repository: SplitBillRepository) : ViewModel() {
    private val route = savedStateHandle.toRoute<ItemEditorRoute>()
    val isNew = route.itemId == 0L

    private val _form = MutableStateFlow(ItemForm())
    val form: StateFlow<ItemForm> = _form.asStateFlow()

    val participants: StateFlow<List<Friend>> = repository.bill(route.billId)
        .map { it?.participants.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    /** Counts items added with "save and add another", shown as a hint. */
    private val _addedCount = MutableStateFlow(0)
    val addedCount: StateFlow<Int> = _addedCount.asStateFlow()

    init {
        if (!isNew) viewModelScope.launch {
            val item = repository.bill(route.billId).first()?.items?.firstOrNull { it.id == route.itemId }
            if (item != null) _form.value = ItemForm(item.title, item.price.toString(), item.quantity, item.sharedBy)
        }
    }

    fun onTitleChange(value: String) = _form.update { it.copy(title = value.take(40)) }
    fun onPriceChange(value: String) = _form.update { it.copy(price = value.filter(Char::isDigit).take(9)) }
    fun changeQuantity(delta: Int) = _form.update { it.copy(quantity = (it.quantity + delta).coerceIn(1, MAX_QUANTITY)) }
    fun shareWithEveryone() = _form.update { it.copy(sharedBy = emptySet()) }

    fun toggleSharer(id: Long) = _form.update { form ->
        val all = participants.value.map { it.id }.toSet()
        val current = form.sharedBy.ifEmpty { all }
        val next = if (id in current) current - id else current + id
        // Selecting everybody (or nobody) is stored as "shared by everyone".
        form.copy(sharedBy = if (next.isEmpty() || next == all) emptySet() else next)
    }

    fun save(addAnother: Boolean) = viewModelScope.launch {
        val form = _form.value
        if (form.problem != null) return@launch
        repository.saveItem(route.billId, route.itemId.takeIf { !isNew }, form.title.trim(), form.priceValue!!, form.quantity, form.sharedBy)
        if (addAnother) {
            // Keep who shares it: the next item from the receipt is often for the same people.
            _form.value = ItemForm(sharedBy = form.sharedBy)
            _addedCount.update { it + 1 }
        } else {
            _done.value = true
        }
    }
}
