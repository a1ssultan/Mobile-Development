package kz.example.splitbill.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kz.example.splitbill.model.Activity
import kz.example.splitbill.model.ActivityStats
import kz.example.splitbill.model.ActivityType
import kz.example.splitbill.model.Bill
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Friend

/**
 * Single source of truth for the UI. ViewModels never touch Room directly:
 * they get domain models (Bill, Friend, Activity) as Flows and call suspend functions to change data.
 * Every change is also written to the activity history.
 */
class SplitBillRepository(
    private val dao: SplitBillDao,
    private val clock: () -> Long = System::currentTimeMillis
) {

    val friends: Flow<List<Friend>> = dao.friends().map { list -> list.map { it.toFriend() } }

    /** All bills, including the ones in the trash. */
    private val allBills: Flow<List<Bill>> = combine(
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
                paidBack = repaidByBill[bill.id].orEmpty().toSet(),
                deletedAt = bill.deletedAt
            )
        }
    }

    /** Active bills: everything the Bills, Balances and Friends screens work with. */
    val bills: Flow<List<Bill>> = allBills.map { list -> list.filter { it.deletedAt == null } }

    /** Bills in the trash, most recently deleted first. */
    val trash: Flow<List<Bill>> = allBills.map { list ->
        list.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }
    }

    val activity: Flow<List<Activity>> = dao.activity().map { rows ->
        rows.map { Activity(it.id, it.createdAt, ActivityType.of(it.type), it.billId, it.text, it.amount) }
    }

    /** An active bill; emits null when it does not exist or was moved to the trash. */
    fun bill(id: Long): Flow<Bill?> = bills.map { list -> list.firstOrNull { it.id == id } }

    fun now(): Long = clock()

    private suspend fun log(type: ActivityType, billId: Long?, text: String, amount: Long? = null) {
        dao.insertActivity(ActivityEntity(createdAt = clock(), type = type.name, billId = billId, text = text, amount = amount))
    }

    private suspend fun billTitle(id: Long): String = dao.bill(id)?.title ?: "счёт"
    private suspend fun friendName(id: Long): String = dao.friend(id)?.name ?: "друг"

    // ---- Bills ----

    suspend fun saveBill(id: Long?, title: String, place: String, servicePercent: Int, payerId: Long?, participantIds: List<Long>): Long {
        val old = id?.let { dao.bill(it) }
        val createdAt = old?.createdAt ?: clock()
        val savedId = dao.saveBill(BillEntity(id ?: 0, title, place, createdAt, servicePercent, payerId, old?.deletedAt), participantIds)
        if (id == null) log(ActivityType.BILL_CREATED, savedId, "Создан счёт «$title»")
        else log(ActivityType.BILL_EDITED, savedId, "Изменён счёт «$title»")
        return savedId
    }

    /** Soft delete: the bill disappears from every screen but can be restored from the trash. */
    suspend fun moveBillToTrash(id: Long) {
        dao.setDeletedAt(id, clock())
        log(ActivityType.BILL_TRASHED, id, "Счёт «${billTitle(id)}» перемещён в корзину")
    }

    suspend fun restoreBill(id: Long) {
        dao.setDeletedAt(id, null)
        log(ActivityType.BILL_RESTORED, id, "Счёт «${billTitle(id)}» восстановлен из корзины")
    }

    /** Permanent delete: items, participants and repayments are removed by CASCADE. */
    suspend fun deleteBillForever(id: Long) {
        val title = billTitle(id)
        dao.deleteBill(id)
        log(ActivityType.BILL_PURGED, id, "Счёт «$title» удалён навсегда")
    }

    suspend fun emptyTrash(): Int {
        val bills = dao.trashed()
        bills.forEach { dao.deleteBill(it.id) }
        if (bills.isNotEmpty()) log(ActivityType.BILL_PURGED, null, "Корзина очищена: удалено счетов — ${bills.size}")
        return bills.size
    }

    /** Bills stay in the trash for [TRASH_DAYS] days, then they are deleted automatically (called on app start). */
    suspend fun purgeOldTrash() {
        val old = dao.trashedBefore(clock() - TRASH_DAYS * ActivityStats.DAY)
        old.forEach {
            dao.deleteBill(it.id)
            log(ActivityType.BILL_PURGED, it.id, "Счёт «${it.title}» автоматически удалён из корзины")
        }
    }

    suspend fun setServicePercent(billId: Long, percent: Int) {
        dao.setServicePercent(billId, percent)
        log(ActivityType.SERVICE_CHANGED, billId, "Сервис в «${billTitle(billId)}» изменён на $percent%")
    }

    // ---- Items ----

    suspend fun saveItem(billId: Long, itemId: Long?, title: String, price: Long, quantity: Int, sharedBy: Set<Long>, restored: Boolean = false) {
        dao.saveItem(ItemEntity(itemId ?: 0, billId, title, price, quantity), sharedBy)
        val bill = billTitle(billId)
        val amount = price * quantity
        when {
            restored -> log(ActivityType.ITEM_RESTORED, billId, "Возвращена позиция «$title» в «$bill»", amount)
            itemId == null -> log(ActivityType.ITEM_ADDED, billId, "Добавлена позиция «$title» в «$bill»", amount)
            else -> log(ActivityType.ITEM_EDITED, billId, "Изменена позиция «$title» в «$bill»", amount)
        }
    }

    suspend fun deleteItem(billId: Long, item: BillItem) {
        dao.deleteItem(item.id)
        log(ActivityType.ITEM_DELETED, billId, "Удалена позиция «${item.title}» из «${billTitle(billId)}»", item.amount)
    }

    // ---- Repayments ----

    suspend fun setRepaid(billId: Long, friendId: Long, repaid: Boolean, amount: Long) {
        val name = friendName(friendId)
        val bill = billTitle(billId)
        if (repaid) {
            dao.insertRepayment(RepaymentEntity(billId, friendId))
            log(ActivityType.REPAID, billId, "$name вернул(а) долг за «$bill»", amount)
        } else {
            dao.deleteRepayment(billId, friendId)
            log(ActivityType.REPAYMENT_CANCELLED, billId, "Отменён возврат: $name, «$bill»", amount)
        }
    }

    suspend fun clearRepayments(billId: Long, returnedAmount: Long) {
        dao.clearRepayments(billId)
        log(ActivityType.REPAYMENT_CANCELLED, billId, "Сброшены все возвраты в «${billTitle(billId)}»", returnedAmount)
    }

    // ---- Friends ----

    suspend fun addFriend(name: String): Long {
        val id = dao.insertFriend(FriendEntity(name = name))
        log(ActivityType.FRIEND_ADDED, null, "Добавлен друг $name")
        return id
    }

    suspend fun renameFriend(id: Long, name: String) {
        val friend = dao.friend(id) ?: return
        if (friend.name == name) return
        dao.updateFriend(friend.copy(name = name))
        log(ActivityType.FRIEND_RENAMED, null, "${friend.name} переименован(а) в $name")
    }

    /** A friend can be deleted only if they are not "me" and not in any bill (including the trash), so no history is lost. */
    suspend fun deleteFriend(id: Long): Boolean {
        val friend = dao.friend(id) ?: return false
        if (friend.isMe || dao.billCountOf(id) > 0) return false
        dao.deleteFriend(id)
        log(ActivityType.FRIEND_DELETED, null, "Удалён друг ${friend.name}")
        return true
    }

    // ---- History ----

    suspend fun clearHistory() = dao.clearActivity()

    companion object {
        const val TRASH_DAYS = 30
    }
}

private fun FriendEntity.toFriend() = Friend(id, name, isMe)
