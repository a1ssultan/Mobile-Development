package kz.example.splitbill.model

data class Friend(val id: Long, val name: String, val isMe: Boolean = false)

/** sharedBy is empty when the item is split between all participants of the bill. */
data class BillItem(
    val id: Long,
    val title: String,
    val price: Long,
    val quantity: Int,
    val sharedBy: Set<Long> = emptySet()
) {
    val amount: Long get() = price * quantity
}

data class Share(val person: Friend, val itemsTotal: Long, val service: Long) {
    val total: Long get() = itemsTotal + service
}

/** "from" owes "to" money for a bill; paid = "from" has already returned it. */
data class Debt(val billId: Long, val billTitle: String, val from: Friend, val to: Friend, val amount: Long, val paid: Boolean)

data class Bill(
    val id: Long,
    val title: String,
    val place: String,
    val createdAt: Long,
    val servicePercent: Int,
    val payer: Friend?,
    val participants: List<Friend>,
    val items: List<BillItem>,
    val paidBack: Set<Long>
) {
    val shares: List<Share> = BillCalculator.calculate(participants, items, servicePercent)
    val subtotal: Long = shares.sumOf { it.itemsTotal }
    val serviceTotal: Long = shares.sumOf { it.service }
    val total: Long = subtotal + serviceTotal

    val debts: List<Debt> = if (payer == null) emptyList() else shares
        .filter { it.person.id != payer.id && it.total > 0 }
        .map { Debt(id, title, it.person, payer, it.total, it.person.id in paidBack) }

    val returnedAmount: Long = debts.filter { it.paid }.sumOf { it.amount }
    val outstandingAmount: Long = debts.filterNot { it.paid }.sumOf { it.amount }
    val isSettled: Boolean = payer != null && items.isNotEmpty() && debts.all { it.paid }
}
