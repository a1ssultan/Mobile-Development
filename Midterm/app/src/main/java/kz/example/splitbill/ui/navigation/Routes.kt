package kz.example.splitbill.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation: every destination is a @Serializable class, its fields are the arguments.

@Serializable object BillsRoute
@Serializable object FriendsRoute
@Serializable object BalancesRoute

@Serializable data class BillDetailRoute(val billId: Long)

/** billId = 0 creates a new bill. */
@Serializable data class BillEditorRoute(val billId: Long = 0)

/** itemId = 0 adds a new item. */
@Serializable data class ItemEditorRoute(val billId: Long, val itemId: Long = 0)

@Serializable data class SettleRoute(val billId: Long)
