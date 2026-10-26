package kz.example.splitbill.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.example.splitbill.ui.balances.BalancesScreen
import kz.example.splitbill.ui.bill.BillDetailScreen
import kz.example.splitbill.ui.bill.SettleScreen
import kz.example.splitbill.ui.bills.BillsScreen
import kz.example.splitbill.ui.editor.BillEditorScreen
import kz.example.splitbill.ui.editor.ItemEditorScreen
import kz.example.splitbill.ui.friends.FriendsScreen

private data class Tab(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(BillsRoute, "Счета", Icons.Filled.Receipt),
    Tab(FriendsRoute, "Друзья", Icons.Filled.Groups),
    Tab(BalancesRoute, "Балансы", Icons.Filled.AccountBalanceWallet)
)

@Composable
fun SplitBillNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // The bottom bar is visible only on the three top-level screens.
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                // Keep a single copy of each tab and remember its scroll/state when switching.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BillsRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable<BillsRoute> {
                BillsScreen(
                    onBillClick = { navController.navigate(BillDetailRoute(it)) },
                    onNewBill = { navController.navigate(BillEditorRoute()) }
                )
            }
            composable<FriendsRoute> { FriendsScreen() }
            composable<BalancesRoute> {
                BalancesScreen(onBillClick = { navController.navigate(SettleRoute(it)) })
            }
            composable<BillDetailRoute> {
                BillDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { billId -> navController.navigate(BillEditorRoute(billId)) },
                    onAddItem = { billId -> navController.navigate(ItemEditorRoute(billId)) },
                    onEditItem = { billId, itemId -> navController.navigate(ItemEditorRoute(billId, itemId)) },
                    onSettle = { billId -> navController.navigate(SettleRoute(billId)) }
                )
            }
            composable<BillEditorRoute> {
                BillEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { billId, isNew ->
                        if (isNew) {
                            // Replace the editor with the new bill, so "back" returns to the list.
                            navController.navigate(BillDetailRoute(billId)) {
                                popUpTo<BillEditorRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable<ItemEditorRoute> {
                ItemEditorScreen(onDone = { navController.popBackStack() })
            }
            composable<SettleRoute> {
                SettleScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
