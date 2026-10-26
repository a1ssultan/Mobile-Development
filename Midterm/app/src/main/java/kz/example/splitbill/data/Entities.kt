package kz.example.splitbill.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isMe: Boolean = false
)

@Entity(
    tableName = "bills",
    foreignKeys = [ForeignKey(FriendEntity::class, ["id"], ["payerId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("payerId")]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val place: String,
    val createdAt: Long,
    val servicePercent: Int,
    val payerId: Long?
)

@Entity(
    tableName = "bill_participants",
    primaryKeys = ["billId", "friendId"],
    foreignKeys = [
        ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FriendEntity::class, ["id"], ["friendId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("friendId")]
)
data class ParticipantEntity(val billId: Long, val friendId: Long)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("billId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val title: String,
    val price: Long,
    val quantity: Int
)

/** Who shares an item. No rows for an item means it is split between everyone. */
@Entity(
    tableName = "item_shares",
    primaryKeys = ["itemId", "friendId"],
    foreignKeys = [
        ForeignKey(ItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FriendEntity::class, ["id"], ["friendId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("friendId")]
)
data class ItemShareEntity(val itemId: Long, val friendId: Long)

/** A row means the friend has returned their share of the bill to the payer. */
@Entity(
    tableName = "repayments",
    primaryKeys = ["billId", "friendId"],
    foreignKeys = [
        ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FriendEntity::class, ["id"], ["friendId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("friendId")]
)
data class RepaymentEntity(val billId: Long, val friendId: Long)
