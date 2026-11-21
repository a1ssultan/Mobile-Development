package kz.example.splitbill.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillCalculatorTest {
    private val a = Friend(1, "A", isMe = true)
    private val b = Friend(2, "B")
    private val c = Friend(3, "C")

    @Test fun sharedItemRemainderIsNotLost() {
        val result = BillCalculator.calculate(listOf(a, b, c), listOf(BillItem(1, "Tea", 100, 1)), 0)
        assertEquals(listOf(34L, 33L, 33L), result.map { it.total })
    }

    @Test fun serviceIsAllocatedAndTotalsMatch() {
        val items = listOf(BillItem(1, "A food", 1000, 1, setOf(1)), BillItem(2, "B food", 2000, 1, setOf(2)))
        val result = BillCalculator.calculate(listOf(a, b), items, 10)
        assertEquals(listOf(1100L, 2200L), result.map { it.total })
    }

    @Test fun itemSharedBySubsetIsSplitOnlyBetweenThem() {
        val items = listOf(BillItem(1, "Wine", 1500, 2, setOf(1, 2)))
        val result = BillCalculator.calculate(listOf(a, b, c), items, 0)
        assertEquals(listOf(1500L, 1500L, 0L), result.map { it.total })
    }

    @Test fun itemOfRemovedPeopleGoesToEveryone() {
        val items = listOf(BillItem(1, "Cake", 900, 1, setOf(99)))
        val result = BillCalculator.calculate(listOf(a, b, c), items, 0)
        assertEquals(listOf(300L, 300L, 300L), result.map { it.total })
    }

    @Test fun serviceRoundingNeverGoesToSomeoneWhoOrderedNothing() {
        val items = listOf(BillItem(1, "X", 1001, 1, setOf(2)), BillItem(2, "Y", 1001, 1, setOf(3)))
        val result = BillCalculator.calculate(listOf(a, b, c), items, 15)
        assertEquals(0L, result[0].service)
        assertEquals((2002L * 15 + 50) / 100, result.sumOf { it.service })
    }

    @Test fun debtsGoToPayerAndSettleWhenAllPaid() {
        val items = listOf(BillItem(1, "Pizza", 3000, 1))
        val bill = Bill(1, "Dinner", "", 0, 0, payer = a, participants = listOf(a, b, c), items = items, paidBack = setOf(2))
        assertEquals(listOf(b to 1000L, c to 1000L), bill.debts.map { it.from to it.amount })
        assertEquals(1000L, bill.outstandingAmount)
        assertFalse(bill.isSettled)
        assertTrue(bill.copy(paidBack = setOf(2, 3)).isSettled)
    }
}
