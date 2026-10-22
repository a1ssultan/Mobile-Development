package kz.example.splitbill.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BillCalculatorTest {
    @Test fun sharedItemRemainderIsNotLost() {
        val people = listOf(Person(1, "A"), Person(2, "B"), Person(3, "C"))
        val result = BillCalculator.calculate(people, listOf(BillItem(1, "Tea", 100, null)), 0)
        assertEquals(listOf(34L, 33L, 33L), result.map { it.total })
        assertEquals(100L, result.sumOf { it.total })
    }

    @Test fun serviceIsAllocatedAndTotalsMatch() {
        val people = listOf(Person(1, "A"), Person(2, "B"))
        val result = BillCalculator.calculate(people, listOf(BillItem(1, "A's food", 1000, 1), BillItem(2, "B's food", 2000, 2)), 10)
        assertEquals(listOf(1100L, 2200L), result.map { it.total })
        assertEquals(3300L, result.sumOf { it.total })
    }
}
