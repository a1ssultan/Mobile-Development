package kz.example.splitbill.model

data class Person(val id: Int, val name: String)

// personId = null means the item is shared by everyone.
data class BillItem(val id: Int, val title: String, val amount: Long, val personId: Int?)

data class Share(val person: Person, val itemsTotal: Long, val service: Long) {
    val total: Long get() = itemsTotal + service
}

object BillCalculator {
    /** Divides integer tenge without losing the remainder. */
    fun calculate(people: List<Person>, items: List<BillItem>, servicePercent: Int): List<Share> {
        if (people.isEmpty()) return emptyList()
        require(servicePercent in 0..100)
        val totals = people.associate { it.id to 0L }.toMutableMap()
        for (item in items) {
            require(item.amount >= 0)
            if (item.personId == null) {
                val each = item.amount / people.size
                val remainder = item.amount % people.size
                people.forEachIndexed { index, person ->
                    totals[person.id] = totals.getValue(person.id) + each + if (index < remainder) 1 else 0
                }
            } else if (item.personId in totals) {
                totals[item.personId] = totals.getValue(item.personId) + item.amount
            }
        }
        val subtotal = totals.values.sum()
        val serviceTotal = (subtotal * servicePercent + 50) / 100 // rounded to nearest tenge
        val serviceShares = people.associate { it.id to 0L }.toMutableMap()
        if (subtotal > 0) {
            var distributed = 0L
            people.forEach { person ->
                val amount = serviceTotal * totals.getValue(person.id) / subtotal
                serviceShares[person.id] = amount
                distributed += amount
            }
            var index = 0
            while (distributed < serviceTotal) {
                val id = people[index % people.size].id
                serviceShares[id] = serviceShares.getValue(id) + 1
                distributed++
                index++
            }
        }
        return people.map { Share(it, totals.getValue(it.id), serviceShares.getValue(it.id)) }
    }
}
