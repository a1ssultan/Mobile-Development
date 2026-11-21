package kz.example.splitbill.model

data class Balance(val person: Friend, val amount: Long) // > 0: should receive, < 0: should pay

data class Transfer(val from: Friend, val to: Friend, val amount: Long)

object Balances {
    /** Net position of every person across all unpaid debts. */
    fun net(debts: List<Debt>): List<Balance> {
        val totals = linkedMapOf<Long, Long>()
        val people = mutableMapOf<Long, Friend>()
        for (debt in debts.filterNot { it.paid }) {
            people[debt.from.id] = debt.from
            people[debt.to.id] = debt.to
            totals[debt.from.id] = (totals[debt.from.id] ?: 0) - debt.amount
            totals[debt.to.id] = (totals[debt.to.id] ?: 0) + debt.amount
        }
        return totals.filterValues { it != 0L }
            .map { (id, amount) -> Balance(people.getValue(id), amount) }
            .sortedByDescending { it.amount }
    }

    /**
     * Greedy debt simplification: the biggest debtor pays the biggest creditor until everyone is even.
     * Produces at most n - 1 transfers instead of one transfer per bill.
     */
    fun simplify(balances: List<Balance>): List<Transfer> {
        val creditors = balances.filter { it.amount > 0 }.map { it.person to it.amount }.toMutableList()
        val debtors = balances.filter { it.amount < 0 }.map { it.person to -it.amount }.toMutableList()
        val transfers = mutableListOf<Transfer>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            creditors.sortByDescending { it.second }
            debtors.sortByDescending { it.second }
            val (creditor, credit) = creditors[0]
            val (debtor, debt) = debtors[0]
            val amount = minOf(credit, debt)
            transfers += Transfer(debtor, creditor, amount)
            if (credit == amount) creditors.removeAt(0) else creditors[0] = creditor to credit - amount
            if (debt == amount) debtors.removeAt(0) else debtors[0] = debtor to debt - amount
        }
        return transfers
    }
}
