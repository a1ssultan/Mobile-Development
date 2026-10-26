package kz.example.splitbill.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.example.splitbill.data.SplitBillRepository
import kz.example.splitbill.model.Balances
import kz.example.splitbill.model.Friend

data class FriendRow(val friend: Friend, val billCount: Int, val balance: Long, val spent: Long)

class FriendsViewModel(private val repository: SplitBillRepository) : ViewModel() {

    val friends: StateFlow<List<FriendRow>?> = combine(repository.friends, repository.bills) { friends, bills ->
        val balances = Balances.net(bills.flatMap { it.debts }).associate { it.person.id to it.amount }
        friends.map { friend ->
            FriendRow(
                friend = friend,
                billCount = bills.count { bill -> bill.participants.any { it.id == friend.id } },
                balance = balances[friend.id] ?: 0,
                spent = bills.sumOf { bill -> bill.shares.firstOrNull { it.person.id == friend.id }?.total ?: 0 }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** One-off message for the snackbar; the screen clears it after showing. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun messageShown() { _message.value = null }

    fun add(name: String) = viewModelScope.launch {
        val clean = name.trim()
        when {
            clean.isEmpty() -> return@launch
            friends.value.orEmpty().any { it.friend.name.equals(clean, ignoreCase = true) } -> _message.value = "«$clean» уже есть в списке"
            else -> repository.addFriend(clean)
        }
    }

    fun rename(friend: Friend, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.renameFriend(friend.id, name.trim())
    }

    fun delete(friend: Friend) = viewModelScope.launch {
        if (!repository.deleteFriend(friend.id)) {
            _message.value = if (friend.isMe) "Себя удалить нельзя" else "${friend.name} участвует в счетах — сначала уберите из них"
        }
    }
}
