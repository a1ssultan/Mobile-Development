package kz.example.splitbill.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalancesTest {
    private val me = Friend(1, "Me", isMe = true)
    private val ali = Friend(2, "Ali")
    private val dana = Friend(3, "Dana")

    @Test fun paidDebtsAreIgnoredAndOppositeDebtsCancelOut() {
        val debts = listOf(
            Debt(1, "Dinner", ali, me, 5000, paid = false),
            Debt(2, "Cinema", me, ali, 2000, paid = false),
            Debt(1, "Dinner", dana, me, 4000, paid = true)
        )
        val net = Balances.net(debts).associate { it.person.id to it.amount }
        assertEquals(mapOf(1L to 3000L, 2L to -3000L), net)
    }

    @Test fun simplifyNeedsFewerTransfers() {
        // Ali owes Me 1000 and Dana owes Ali 1000: Dana can pay Me directly.
        val debts = listOf(Debt(1, "A", ali, me, 1000, false), Debt(2, "B", dana, ali, 1000, false))
        val transfers = Balances.simplify(Balances.net(debts))
        assertEquals(listOf(Transfer(dana, me, 1000)), transfers)
    }
}
