package kz.example.splitbill.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SplitBillDao {
    // Each query returns a Flow: Room re-emits it whenever the table changes.
    @Query("SELECT * FROM friends ORDER BY isMe DESC, name COLLATE NOCASE")
    abstract fun friends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    abstract fun bills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bill_participants")
    abstract fun participants(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM items ORDER BY id")
    abstract fun items(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item_shares")
    abstract fun itemShares(): Flow<List<ItemShareEntity>>

    @Query("SELECT * FROM repayments")
    abstract fun repayments(): Flow<List<RepaymentEntity>>

    @Insert abstract suspend fun insertFriend(friend: FriendEntity): Long
    @Update abstract suspend fun updateFriend(friend: FriendEntity)
    @Query("DELETE FROM friends WHERE id = :id") abstract suspend fun deleteFriend(id: Long)
    @Query("SELECT * FROM friends WHERE id = :id") abstract suspend fun friend(id: Long): FriendEntity?
    @Query("SELECT COUNT(*) FROM bill_participants WHERE friendId = :id") abstract suspend fun billCountOf(id: Long): Int

    @Insert abstract suspend fun insertBill(bill: BillEntity): Long
    @Update abstract suspend fun updateBill(bill: BillEntity)
    @Query("SELECT * FROM bills WHERE id = :id") abstract suspend fun bill(id: Long): BillEntity?
    @Query("DELETE FROM bills WHERE id = :id") abstract suspend fun deleteBill(id: Long)
    @Query("UPDATE bills SET servicePercent = :percent WHERE id = :id") abstract suspend fun setServicePercent(id: Long, percent: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertParticipants(rows: List<ParticipantEntity>)
    @Query("DELETE FROM bill_participants WHERE billId = :billId AND friendId NOT IN (:keep)")
    abstract suspend fun removeParticipantsExcept(billId: Long, keep: List<Long>)
    @Query("DELETE FROM item_shares WHERE friendId NOT IN (:keep) AND itemId IN (SELECT id FROM items WHERE billId = :billId)")
    abstract suspend fun removeItemSharesExcept(billId: Long, keep: List<Long>)
    @Query("DELETE FROM repayments WHERE billId = :billId AND friendId NOT IN (:keep)")
    abstract suspend fun removeRepaymentsExcept(billId: Long, keep: List<Long>)

    @Insert abstract suspend fun insertItem(item: ItemEntity): Long
    @Update abstract suspend fun updateItem(item: ItemEntity)
    @Query("DELETE FROM items WHERE id = :id") abstract suspend fun deleteItem(id: Long)
    @Insert abstract suspend fun insertItemShares(rows: List<ItemShareEntity>)
    @Query("DELETE FROM item_shares WHERE itemId = :itemId") abstract suspend fun clearItemShares(itemId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertRepayment(row: RepaymentEntity)
    @Query("DELETE FROM repayments WHERE billId = :billId AND friendId = :friendId") abstract suspend fun deleteRepayment(billId: Long, friendId: Long)
    @Query("DELETE FROM repayments WHERE billId = :billId") abstract suspend fun clearRepayments(billId: Long)

    /** Creates or updates a bill together with its participants in one transaction. */
    @Transaction
    open suspend fun saveBill(bill: BillEntity, participantIds: List<Long>): Long {
        val id = if (bill.id == 0L) insertBill(bill) else bill.id.also { updateBill(bill) }
        removeParticipantsExcept(id, participantIds)
        removeItemSharesExcept(id, participantIds)
        removeRepaymentsExcept(id, participantIds)
        insertParticipants(participantIds.map { ParticipantEntity(id, it) })
        return id
    }

    @Transaction
    open suspend fun saveItem(item: ItemEntity, sharedBy: Set<Long>): Long {
        val id = if (item.id == 0L) insertItem(item) else item.id.also { updateItem(item) }
        clearItemShares(id)
        insertItemShares(sharedBy.map { ItemShareEntity(id, it) })
        return id
    }
}
