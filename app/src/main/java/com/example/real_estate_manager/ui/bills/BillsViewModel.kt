package com.example.real_estate_manager.ui.bills

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Временный in-memory стор, без DI, чтобы не ломать проект.
 * Потом заменим на репозиторий/Room.
 */
class BillsViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<BillUi>>(
        // демо-данные
        listOf(
            BillUi(
                title = "Электричество",
                amount = 4893.60,
                dueDate = LocalDate.now().plusDays(3),
                isPaid = false,
                propertyName = "Офис, Бутово Молл"
            ),
            BillUi(
                title = "Вода",
                amount = 1250.00,
                dueDate = LocalDate.now().minusDays(1), // просрочен
                isPaid = false,
                propertyName = "Квартира Тёона"
            ),
            BillUi(
                title = "Интернет",
                amount = 800.00,
                dueDate = LocalDate.now().plusDays(10),
                isPaid = true,
                propertyName = "Офис, Бутово Молл"
            )
        )
    )
    val items: StateFlow<List<BillUi>> = _items

    fun upsert(bill: BillUi) {
        val current = _items.value.toMutableList()
        val idx = current.indexOfFirst { it.id == bill.id }
        if (idx >= 0) current[idx] = bill else current.add(bill)
        _items.value = current
    }

    fun delete(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun markPaid(id: String, paid: Boolean) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(isPaid = paid) else it
        }
    }
}