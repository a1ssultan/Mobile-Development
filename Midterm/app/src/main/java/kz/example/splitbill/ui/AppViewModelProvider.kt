package kz.example.splitbill.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.example.splitbill.SplitBillApp
import kz.example.splitbill.ui.balances.BalancesViewModel
import kz.example.splitbill.ui.bill.BillDetailViewModel
import kz.example.splitbill.ui.bills.BillsViewModel
import kz.example.splitbill.ui.editor.BillEditorViewModel
import kz.example.splitbill.ui.editor.ItemEditorViewModel
import kz.example.splitbill.ui.friends.FriendsViewModel

/**
 * Creates every ViewModel with its dependencies.
 * createSavedStateHandle() gives the ViewModel the navigation arguments of its screen (e.g. billId).
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { BillsViewModel(repository()) }
        initializer { BillDetailViewModel(createSavedStateHandle(), repository()) }
        initializer { BillEditorViewModel(createSavedStateHandle(), repository()) }
        initializer { ItemEditorViewModel(createSavedStateHandle(), repository()) }
        initializer { FriendsViewModel(repository()) }
        initializer { BalancesViewModel(repository()) }
    }

    private fun CreationExtras.repository() = (this[APPLICATION_KEY] as SplitBillApp).container.repository
}
