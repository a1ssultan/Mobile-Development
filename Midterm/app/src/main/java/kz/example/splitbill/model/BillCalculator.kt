package kz.example.splitbill.model

object BillCalculator {
    /**
     * Splits items and service between participants in whole tenge.
     * The sum of all shares always equals the sum of items plus the rounded service charge.
     */
    fun calculate(people: List<Friend>, items: List<BillItem>, servicePercent: Int): List<Share> {
        if (people.isEmpty()) return emptyList()
        require(servicePercent in 0..100)
        val ids = people.map { it.id }
        val totals = ids.associateWith { 0L }.toMutableMap()
        for (item in items) {
            require(item.amount >= 0)
            // People removed from the bill no longer pay; if nobody is left the item goes to everyone.
            val payers = ids.filter { it in item.sharedBy }.ifEmpty { ids }
            splitEvenly(item.amount, payers).forEach { (id, part) -> totals[id] = totals.getValue(id) + part }
        }
        val subtotal = totals.values.sum()
        val serviceTotal = (subtotal * servicePercent + 50) / 100 // rounded to nearest tenge
        val service = splitProportionally(serviceTotal, totals)
        return people.map { Share(it, totals.getValue(it.id), service.getValue(it.id)) }
    }

    /** Equal parts; the first (amount % n) people pay one extra tenge. */
    fun splitEvenly(amount: Long, ids: List<Long>): Map<Long, Long> {
        val each = amount / ids.size
        val remainder = amount % ids.size
        return ids.withIndex().associate { (index, id) -> id to each + if (index < remainder) 1 else 0 }
    }

    /** Largest remainder method: rounding leftovers go to those with the biggest fractional parts. */
    fun splitProportionally(amount: Long, weights: Map<Long, Long>): Map<Long, Long> {
        val sum = weights.values.sum()
        if (sum == 0L) return weights.mapValues { 0L }
        val result = weights.mapValues { (_, w) -> amount * w / sum }.toMutableMap()
        var left = amount - result.values.sum()
        val byFraction = weights.entries.sortedByDescending { (_, w) -> amount * w % sum }
        for ((id, _) in byFraction) {
            if (left == 0L) break
            result[id] = result.getValue(id) + 1
            left--
        }
        return result
    }
}
