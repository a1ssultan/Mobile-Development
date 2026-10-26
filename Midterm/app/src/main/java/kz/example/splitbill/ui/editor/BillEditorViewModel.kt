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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Friend
import kz.example.splitbill.ui.navigation.BillEditorRoute

data class BillForm(
    val title: String = "",
    val place: String = "",
    val servicePercent: Int = 10,
    val participantIds: Set<Long> = emptySet(),
    val payerId: Long? = null
) {
    /** Why the bill cannot be saved yet, or null if everything is fine. */
    val problem: String?
        get() = when {
            title.isBlank() -> "Введите название счёта"
            participantIds.size < 2 -> "Выберите хотя бы двух участников"
            payerId == null || payerId !in participantIds -> "Укажите, кто оплатил счёт"
            else -> null
        }
}

class BillEditorViewModel(savedStateHandle: SavedStateHandle, private val repository: SplitBillRepository) : ViewModel() {
    private val billId: Long = savedStateHandle.toRoute<BillEditorRoute>().billId
    val isNew = billId == 0L

    private val _form = MutableStateFlow(BillForm())
    val form: StateFlow<BillForm> = _form.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Set after a successful save; the screen reacts by navigating away. */
    private val _savedBillId = MutableStateFlow<Long?>(null)
    val savedBillId: StateFlow<Long?> = _savedBillId.asStateFlow()

    val friends: StateFlow<List<Friend>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (isNew) {
                val me = repository.friends.first().firstOrNull { it.isMe }
                if (me != null) _form.update { it.copy(participantIds = setOf(me.id), payerId = me.id) }
            } else {
                repository.bill(billId).first()?.let { bill ->
                    _form.value = BillForm(bill.title, bill.place, bill.servicePercent, bill.participants.map { it.id }.toSet(), bill.payer?.id)
                }
            }
            _loading.value = false
        }
    }

    fun onTitleChange(value: String) = _form.update { it.copy(title = value.take(60)) }
    fun onPlaceChange(value: String) = _form.update { it.copy(place = value.take(80)) }
    fun onServiceChange(value: Int) = _form.update { it.copy(servicePercent = value) }
    fun onPayerChange(id: Long) = _form.update { it.copy(payerId = id) }

    fun toggleParticipant(id: Long) = _form.update { form ->
        val ids = if (id in form.participantIds) form.participantIds - id else form.participantIds + id
        // If the payer was removed from the bill, nobody is the payer any more.
        form.copy(participantIds = ids, payerId = form.payerId?.takeIf { it in ids })
    }

    /** Adds a new friend right from the editor and selects them; an existing name is just selected. */
    fun addFriend(name: String) = viewModelScope.launch {
        val clean = name.trim()
        if (clean.isEmpty()) return@launch
        val existing = friends.value.firstOrNull { it.name.equals(clean, ignoreCase = true) }
        val id = existing?.id ?: repository.addFriend(clean)
        _form.update { it.copy(participantIds = it.participantIds + id) }
    }

    fun save() = viewModelScope.launch {
        val form = _form.value
        if (form.problem != null) return@launch
        _savedBillId.value = repository.saveBill(
            id = billId.takeIf { !isNew },
            title = form.title.trim(),
            place = form.place.trim(),
            servicePercent = form.servicePercent,
            payerId = form.payerId,
            participantIds = form.participantIds.toList()
        )
    }
}
