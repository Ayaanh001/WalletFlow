package com.hussain.walletflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hussain.walletflow.ui.screens.*
import com.hussain.walletflow.ui.theme.TransactionTrackerTheme
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.util.Calendar

class MainActivity : androidx.fragment.app.FragmentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
                installSplashScreen()
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                setContent { TransactionTrackerTheme { MainScreen() } }
        }
}

sealed class Screen(
        val route: String,
        val title: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector
) {
        object Home : Screen("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home)
        object Passbook : Screen(
                "passbook", "Passbook",
                Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet
        )
        object AddTransaction : Screen("add_transaction", "Add", Icons.Filled.Add, Icons.Filled.Add)
        object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Filled.Settings)
        object ImportPreview : Screen(
                "import_preview", "Import Preview",
                Icons.Filled.FileOpen, Icons.Filled.FileOpen
        )
        object CategoryBreakdown : Screen(
                "category_breakdown", "Breakdown",
                Icons.Filled.FileOpen, Icons.Filled.FileOpen
        )
        object CategoryTransactions : Screen(
                "category_transactions", "Transactions",
                Icons.Filled.FileOpen, Icons.Filled.FileOpen
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val prefsRepository = remember { com.hussain.walletflow.data.UserPreferencesRepository(context) }
        val appLockEnabled by prefsRepository.appLockEnabledFlow.collectAsState(initial = false)
        var isUnlocked by remember { mutableStateOf(false) }

        if (appLockEnabled && !isUnlocked) {
                BiometricLockScreen(onUnlocked = { isUnlocked = true })
                return
        }

        val navController = rememberNavController()
        val viewModel: TransactionViewModel = viewModel()
        val haptic = LocalHapticFeedback.current

        // rememberLazyListState() already uses rememberSaveable internally —
        // scroll positions survive config changes and nav transitions.
        val homeListState = rememberLazyListState()
        val passbookListState = rememberLazyListState()

        var isAnySelectionMode by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Derived flags — avoids string comparisons on every recomposition
        val isOnAddTransaction = currentRoute?.startsWith(Screen.AddTransaction.route) == true
        val isOnSettings = currentRoute == Screen.Settings.route
        val isOnImportPreview = currentRoute == Screen.ImportPreview.route
        val isOnCategoryBreakdown = currentRoute?.startsWith("category_breakdown") == true
        val isOnCategoryTransactions = currentRoute?.startsWith("category_transactions") == true
                || currentRoute?.startsWith("payment_transactions") == true
        val showChrome = !isAnySelectionMode && !isOnAddTransaction && !isOnSettings && !isOnImportPreview && !isOnCategoryBreakdown && !isOnCategoryTransactions

        val tabs = remember { listOf(Screen.Home, Screen.Passbook) }

        Scaffold(
                bottomBar = {
                        AnimatedVisibility(
                                visible = showChrome,
                                enter = slideInVertically(initialOffsetY = { it }),
                                exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                                Surface(
                                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        tonalElevation = 3.dp,
                                        modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                ) {
                                        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp, modifier = Modifier.height(110.dp)) {
                                                val currentDestination = navBackStackEntry?.destination
                                                tabs.forEach { screen ->
                                                        val selected = currentDestination?.hierarchy?.any {
                                                                it.route == screen.route
                                                        } == true

                                                        NavigationBarItem(
                                                                icon = {
                                                                        if (screen == Screen.Home) {
                                                                                Icon(
                                                                                        painter = painterResource(
                                                                                                if (selected) R.drawable.ic_home_filled
                                                                                                else R.drawable.ic_home_outlined
                                                                                        ),
                                                                                        contentDescription = screen.title
                                                                                )
                                                                        } else {
                                                                                Icon(
                                                                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                                                        contentDescription = screen.title
                                                                                )
                                                                        }
                                                                },
                                                                label = { Text(screen.title) },
                                                                selected = selected,
                                                                onClick = {
                                                                        if (!selected) {
                                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                                navController.navigate(screen.route) {
                                                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                                                                saveState = true
                                                                                        }
                                                                                        launchSingleTop = true
                                                                                        restoreState = true
                                                                                }
                                                                        }
                                                                },
                                                                colors = NavigationBarItemDefaults.colors(
                                                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                        )
                                                }
                                        }
                                }
                        }
                },
                floatingActionButton = {
                        if (showChrome) {
                                Box(
                                        modifier = Modifier.shadow(
                                                elevation = 16.dp,
                                                shape = CircleShape,
                                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                                spotColor = Color.Black.copy(alpha = 0.8f)
                                        )
                                ) {
                                        FloatingActionButton(
                                                onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        navController.navigate(Screen.AddTransaction.route) {
                                                                launchSingleTop = true
                                                        }
                                                },
                                                shape = CircleShape,
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                                        }
                                }
                        }
                }
        ) { innerPadding ->
                NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(initialOffsetX = { it / 3 })
                        },
                        exitTransition = {
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutHorizontally(targetOffsetX = { -it / 3 })
                        },
                        popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(initialOffsetX = { -it / 3 })
                        },
                        popExitTransition = {
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutHorizontally(targetOffsetX = { it / 3 })
                        }
                ) {
                        composable(Screen.Home.route) {
                                Surface(modifier = Modifier.padding(innerPadding)) {
                                        HomeScreen(
                                                viewModel = viewModel,
                                                listState = homeListState,
                                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                                onEditTransaction = { id ->
                                                        navController.navigate(
                                                                Screen.AddTransaction.route + "?transactionId=$id"
                                                        ) { launchSingleTop = true }
                                                },
                                                onSelectionModeChanged = { isAnySelectionMode = it },
                                                onIncomeClick = { year, month ->
                                                        navController.navigate("category_breakdown/false/$year/$month")
                                                },
                                                onExpenseClick = { year, month ->
                                                        navController.navigate("category_breakdown/true/$year/$month")
                                                }
                                        )
                                }
                        }
                        composable(Screen.Passbook.route) {
                                Surface(modifier = Modifier.padding(innerPadding)) {
                                        PassbookScreen(
                                                viewModel = viewModel,
                                                listState = passbookListState,
                                                onEditTransaction = { id ->
                                                        navController.navigate(
                                                                Screen.AddTransaction.route + "?transactionId=$id"
                                                        ) { launchSingleTop = true }
                                                },
                                                onSelectionModeChanged = { isAnySelectionMode = it }
                                        )
                                }
                        }
                        composable(
                                route = Screen.AddTransaction.route + "?transactionId={transactionId}&transactionType={transactionType}",
                                arguments = listOf(
                                        navArgument("transactionId") {
                                                type = NavType.LongType
                                                defaultValue = -1L
                                        },
                                        navArgument("transactionType") {
                                                type = NavType.StringType
                                                defaultValue = "EXPENSE"
                                        }
                                )
                        ) { backStackEntry ->
                                val transactionId = backStackEntry.arguments
                                        ?.getLong("transactionId")
                                        ?.takeIf { it != -1L }
                                val transactionType = backStackEntry.arguments
                                        ?.getString("transactionType") ?: "EXPENSE"
                                AddTransactionScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        transactionId = transactionId,
                                        initialTransactionType = transactionType
                                )
                        }
                        composable(Screen.Settings.route) {
                                SettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        viewModel = viewModel,
                                        onNavigateToImport = {
                                                navController.navigate(Screen.ImportPreview.route) {
                                                        launchSingleTop = true
                                                }
                                        }
                                )
                        }
                        composable(Screen.ImportPreview.route) {
                                val pendingImport by viewModel.pendingImport.collectAsState()
                                ImportPreviewScreen(
                                        viewModel = viewModel,
                                        parsedTransactions = pendingImport,
                                        initialTargetYear = Calendar.getInstance().get(Calendar.YEAR),
                                        initialTargetMonth = Calendar.getInstance().get(Calendar.MONTH),
                                        onImportDone = { navController.popBackStack() },
                                        onBack = {
                                                viewModel.clearPendingImport()
                                                navController.popBackStack()
                                        }
                                )
                        }
                        composable(
                                route = "category_breakdown/{isExpense}/{year}/{month}",
                                arguments = listOf(
                                        androidx.navigation.navArgument("isExpense") { type = NavType.BoolType },
                                        androidx.navigation.navArgument("year") { type = NavType.IntType },
                                        androidx.navigation.navArgument("month") { type = NavType.IntType }
                                )
                        ) { backStackEntry ->
                                val isExpense = backStackEntry.arguments?.getBoolean("isExpense") ?: true
                                val year = backStackEntry.arguments?.getInt("year") ?: Calendar.getInstance().get(Calendar.YEAR)
                                val month = backStackEntry.arguments?.getInt("month") ?: Calendar.getInstance().get(Calendar.MONTH)
                                CategoryBreakdownScreen(
                                        viewModel = viewModel,
                                        isExpense = isExpense,
                                        initialYear = year,
                                        initialMonth = month,
                                        onBack = { navController.popBackStack() },
                                        onCategoryClick = { category, y, m ->
                                                val encoded = java.net.URLEncoder.encode(category, "UTF-8")
                                                navController.navigate("category_transactions/$encoded/$y/$m")
                                        },
                                        onAddTransaction = {
                                                val typeParam = if (isExpense) "EXPENSE" else "INCOME"
                                                navController.navigate(
                                                        Screen.AddTransaction.route + "?transactionType=$typeParam"
                                                ) {
                                                        launchSingleTop = true
                                                }
                                        },
                                        onPaymentMethodClick = { method, y, m ->
                                                val encoded = java.net.URLEncoder.encode(method, "UTF-8")
                                                navController.navigate("payment_transactions/$encoded/$y/$m")
                                        },
                                )
                        }
                        composable(
                                route = "category_transactions/{category}/{year}/{month}",
                                arguments = listOf(
                                        androidx.navigation.navArgument("category") { type = NavType.StringType },
                                        androidx.navigation.navArgument("year") { type = NavType.IntType },
                                        androidx.navigation.navArgument("month") { type = NavType.IntType }
                                )
                        ) { backStackEntry ->
                                val category = backStackEntry.arguments?.getString("category")
                                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                                val year = backStackEntry.arguments?.getInt("year") ?: Calendar.getInstance().get(Calendar.YEAR)
                                val month = backStackEntry.arguments?.getInt("month") ?: Calendar.getInstance().get(Calendar.MONTH)
                                CategoryTransactionsScreen(
                                        viewModel = viewModel,
                                        category = category,
                                        initialYear = year,
                                        initialMonth = month,
                                        onBack = { navController.popBackStack() },
                                        onEditTransaction = { id ->
                                                navController.navigate(
                                                        Screen.AddTransaction.route + "?transactionId=$id"
                                                ) { launchSingleTop = true }
                                        }
                                )
                        }

                        composable(
                                route = "payment_transactions/{method}/{year}/{month}",
                                arguments = listOf(
                                        navArgument("method") { type = NavType.StringType },
                                        navArgument("year")   { type = NavType.IntType },
                                        navArgument("month")  { type = NavType.IntType }
                                )
                        ) { backStackEntry ->
                                val method = backStackEntry.arguments?.getString("method")
                                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                                val year  = backStackEntry.arguments?.getInt("year")  ?: Calendar.getInstance().get(Calendar.YEAR)
                                val month = backStackEntry.arguments?.getInt("month") ?: Calendar.getInstance().get(Calendar.MONTH)
                                CategoryTransactionsScreen(
                                        viewModel = viewModel,
                                        category = method,
                                        initialYear = year,
                                        initialMonth = month,
                                        filterByPaymentMethod = true,   // ← key difference
                                        onBack = { navController.popBackStack() },
                                        onEditTransaction = { id ->
                                                navController.navigate(Screen.AddTransaction.route + "?transactionId=$id") {
                                                        launchSingleTop = true
                                                }
                                        }
                                )
                        }
                }
        }
}


