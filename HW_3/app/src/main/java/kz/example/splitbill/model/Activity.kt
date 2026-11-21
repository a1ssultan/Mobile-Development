package kz.example.splitbill.model

enum class ActivityType {
    BILL_CREATED, BILL_EDITED, BILL_TRASHED, BILL_RESTORED, BILL_PURGED,
    ITEM_ADDED, ITEM_EDITED, ITEM_DELETED, ITEM_RESTORED,
    SERVICE_CHANGED, REPAID, REPAYMENT_CANCELLED,
    FRIEND_ADDED, FRIEND_RENAMED, FRIEND_DELETED;

    companion object {
        fun of(name: String): ActivityType = entries.firstOrNull { it.name == name } ?: BILL_EDITED
    }
}

/** A single event in the history log. amount is set for money events (repayments, items). */
data class Activity(
    val id: Long,
    val createdAt: Long,
    val type: ActivityType,
    val billId: Long?,
    val text: String,
    val amount: Long?
)

/** Statistics for the history screen, computed from the log for the last [days] days. */
data class ActivityStats(
    val days: Int,
    val actions: Int,
    val returned: Long,
    val repayments: Int,
    val billsCreated: Int
) {
    companion object {
        const val DAY = 24 * 60 * 60 * 1000L

        fun of(log: List<Activity>, now: Long, days: Int = 7): ActivityStats {
            val recent = log.filter { it.createdAt >= now - days * DAY }
            val repaid = recent.filter { it.type == ActivityType.REPAID }
            val cancelled = recent.filter { it.type == ActivityType.REPAYMENT_CANCELLED }
            return ActivityStats(
                days = days,
                actions = recent.size,
                returned = (repaid.sumOf { it.amount ?: 0 } - cancelled.sumOf { it.amount ?: 0 }).coerceAtLeast(0),
                repayments = (repaid.size - cancelled.size).coerceAtLeast(0),
                billsCreated = recent.count { it.type == ActivityType.BILL_CREATED }
            )
        }
    }
}
