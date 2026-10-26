package kz.example.splitbill.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kz.example.splitbill.model.Bill
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Friend

/**
 * Single source of truth for the UI. ViewModels never touch Room directly:
 * they get domain models (Bill, Friend) as Flows and call suspend functions to change data.
 */
class SplitBillRepository(private val dao: SplitBillDao) {

    val friends: Flow<List<Friend>> = dao.friends().map { list -> list.map { it.toFriend() } }

    val bills: Flow<List<Bill>> = combine(
        dao.bills(),
        dao.friends(),
        dao.participants(),
        dao.items(),
        combine(dao.itemShares(), dao.repayments(), ::Pair)
    ) { bills, friends, participants, items, (shares, repayments) ->
        val friendById = friends.associate { it.id to it.toFriend() }
        val sharesByItem = shares.groupBy({ it.itemId }, { it.friendId })
        val itemsByBill = items.groupBy { it.billId }
        val participantsByBill = participants.groupBy({ it.billId }, { it.friendId })
        val repaidByBill = repayments.groupBy({ it.billId }, { it.friendId })
        bills.map { bill ->
            Bill(
                id = bill.id,
                title = bill.title,
                place = bill.place,
                createdAt = bill.createdAt,
                servicePercent = bill.servicePercent,
                payer = bill.payerId?.let(friendById::get),
                participants = participantsByBill[bill.id].orEmpty().mapNotNull(friendById::get)
                    .sortedWith(compareByDescending<Friend> { it.isMe }.thenBy { it.name.lowercase() }),
                items = itemsByBill[bill.id].orEmpty().map {
                    BillItem(it.id, it.title, it.price, it.quantity, sharesByItem[it.id].orEmpty().toSet())
                },
                paidBack = repaidByBill[bill.id].orEmpty().toSet()
            )
        }
    }

    fun bill(id: Long): Flow<Bill?> = bills.map { list -> list.firstOrNull { it.id == id } }

    suspend fun saveBill(id: Long?, title: String, place: String, servicePercent: Int, payerId: Long?, participantIds: List<Long>): Long {
        val createdAt = id?.let { dao.bill(it)?.createdAt } ?: System.currentTimeMillis()
        return dao.saveBill(BillEntity(id ?: 0, title, place, createdAt, servicePercent, payerId), participantIds)
    }

    suspend fun deleteBill(id: Long) = dao.deleteBill(id)

    suspend fun setServicePercent(billId: Long, percent: Int) = dao.setServicePercent(billId, percent)

    suspend fun saveItem(billId: Long, itemId: Long?, title: String, price: Long, quantity: Int, sharedBy: Set<Long>) {
        dao.saveItem(ItemEntity(itemId ?: 0, billId, title, price, quantity), sharedBy)
    }

    suspend fun deleteItem(id: Long) = dao.deleteItem(id)

    suspend fun setRepaid(billId: Long, friendId: Long, repaid: Boolean) {
        if (repaid) dao.insertRepayment(RepaymentEntity(billId, friendId)) else dao.deleteRepayment(billId, friendId)
    }

    suspend fun clearRepayments(billId: Long) = dao.clearRepayments(billId)

    suspend fun addFriend(name: String): Long = dao.insertFriend(FriendEntity(name = name))

    suspend fun renameFriend(id: Long, name: String) {
        val friend = dao.friend(id) ?: return
        dao.updateFriend(friend.copy(name = name))
    }

    /** A friend can be deleted only if they are not "me" and not in any bill, so no history is lost. */
    suspend fun deleteFriend(id: Long): Boolean {
        val friend = dao.friend(id) ?: return false
        if (friend.isMe || dao.billCountOf(id) > 0) return false
        dao.deleteFriend(id)
        return true
    }
}

private fun FriendEntity.toFriend() = Friend(id, name, isMe)
