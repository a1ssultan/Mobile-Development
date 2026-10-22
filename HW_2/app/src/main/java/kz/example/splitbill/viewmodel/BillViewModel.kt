package kz.example.splitbill.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kz.example.splitbill.model.BillCalculator
import kz.example.splitbill.model.BillItem
import kz.example.splitbill.model.Person

class BillViewModel : ViewModel() {
    val people = mutableStateListOf(Person(1, "Али"), Person(2, "Дана"), Person(3, "Амир"))
    val items = mutableStateListOf(
        BillItem(1, "Бургер", 3500, 1),
        BillItem(2, "Паста", 4200, 2),
        BillItem(3, "Кофе", 1600, 3),
        BillItem(4, "Пицца на всех", 6000, null)
    )
    var servicePercent by mutableIntStateOf(10)
        private set
    private var nextPersonId = 4
    private var nextItemId = 5

    val shares get() = BillCalculator.calculate(people, items, servicePercent)
    val subtotal get() = items.sumOf { it.amount }
    val serviceTotal get() = (subtotal * servicePercent + 50) / 100
    val grandTotal get() = subtotal + serviceTotal

    fun addPerson(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        people.add(Person(nextPersonId++, clean))
        return true
    }

    fun addItem(title: String, amountText: String, personId: Int?): Boolean {
        val amount = amountText.trim().toLongOrNull()
        if (title.isBlank() || amount == null || amount <= 0 || amount > 100_000_000) return false
        if (people.isEmpty() || (personId != null && people.none { it.id == personId })) return false
        items.add(BillItem(nextItemId++, title.trim(), amount, personId))
        return true
    }

    fun removeItem(id: Int) { items.removeAll { it.id == id } }
    fun removePerson(id: Int) {
        people.removeAll { it.id == id }
        items.removeAll { it.personId == id }
    }
    fun updateServicePercent(percent: Int) {
        servicePercent = percent.coerceIn(0, 15)
    }
}
