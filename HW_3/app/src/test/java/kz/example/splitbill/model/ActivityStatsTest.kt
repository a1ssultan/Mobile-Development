package kz.example.splitbill.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityStatsTest {
    private val now = 100 * ActivityStats.DAY
    private fun entry(id: Long, daysAgo: Int, type: ActivityType, amount: Long? = null) =
        Activity(id, now - daysAgo * ActivityStats.DAY, type, 1, "", amount)

    @Test
    fun countsOnlyLastSevenDays() {
        val log = listOf(
            entry(1, 1, ActivityType.BILL_CREATED),
            entry(2, 2, ActivityType.REPAID, 5000),
            entry(3, 10, ActivityType.REPAID, 9000), // too old
            entry(4, 10, ActivityType.BILL_CREATED)  // too old
        )
        val stats = ActivityStats.of(log, now)
        assertEquals(2, stats.actions)
        assertEquals(1, stats.billsCreated)
        assertEquals(5000, stats.returned)
        assertEquals(1, stats.repayments)
    }

    @Test
    fun cancelledRepaymentIsSubtracted() {
        val log = listOf(
            entry(1, 1, ActivityType.REPAID, 3000),
            entry(2, 1, ActivityType.REPAID, 2000),
            entry(3, 0, ActivityType.REPAYMENT_CANCELLED, 2000)
        )
        val stats = ActivityStats.of(log, now)
        assertEquals(3000, stats.returned)
        assertEquals(1, stats.repayments)
    }
}
