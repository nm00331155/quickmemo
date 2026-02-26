package com.quickmemo.app.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.presentation.editor.EditorScreen
import com.quickmemo.app.presentation.home.HomeScreen
import com.quickmemo.app.presentation.premium.PremiumScreen
import com.quickmemo.app.presentation.search.SearchScreen
import com.quickmemo.app.presentation.settings.SettingsScreen
import com.quickmemo.app.presentation.todo.TodoScreen
import com.quickmemo.app.presentation.trash.TrashScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMemoNavHost(
    startDestination: String,
    onToggleQuickService: (Boolean) -> Unit,
    onToggleTodoReminder: (Boolean) -> Unit,
    onRequestBiometric: (String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    fun currentRoute(): String? = currentBackStackEntry?.destination?.route

    fun popOrNavigateHome() {
        val popped = navController.popBackStack()
        val nowRoute = currentRoute()
        Log.d(
            TAG,
            "popBackStack result=$popped, now=$nowRoute",
        )
        if (!popped || nowRoute == null) {
            navController.navigate(QuickMemoDestinations.HOME) {
                launchSingleTop = true
            }
        }
    }

    fun navigateSingleTop(route: String) {
        val from = currentRoute()
        Log.d(TAG, "navigate request from=$from to=$route")
        if (from == route) {
            Log.d(TAG, "navigate skipped (duplicate route): $route")
            return
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { controller, destination, _ ->
            Log.d(
                TAG,
                "destination changed: current=${destination.route}, previous=${controller.previousBackStackEntry?.destination?.route}",
            )
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(route = QuickMemoDestinations.HOME) {
            HomeScreen(
                onOpenEditor = { memoId, colorLabel ->
                    navigateSingleTop(
                        QuickMemoDestinations.editorRoute(
                            memoId = memoId ?: 0L,
                            colorLabel = colorLabel ?: 0,
                        )
                    )
                },
                onOpenSettings = {
                    navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 0))
                },
                onRequestUnlockMemo = { memo: Memo, onResult: (Boolean) -> Unit ->
                    if (memo.isLocked) {
                        onRequestBiometric("ロックされたメモを開く") { passed ->
                            onResult(passed)
                        }
                    } else {
                        onResult(true)
                    }
                },
                onOpenTodo = {
                    navigateSingleTop(QuickMemoDestinations.TODO)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = QuickMemoDestinations.EDITOR,
            arguments = listOf(
                navArgument("memoId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("prefillText") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillChecklist") { type = NavType.BoolType; defaultValue = false },
                navArgument("insertToday") { type = NavType.BoolType; defaultValue = false },
                navArgument("colorLabel") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            EditorScreen(
                onNavigateBack = {
                    popOrNavigateHome()
                },
                onOpenTodo = {
                    navigateSingleTop(QuickMemoDestinations.TODO)
                },
                onOpenSettings = {
                    navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 1))
                },
                onRequestBiometric = onRequestBiometric,
            )
        }

        composable(route = QuickMemoDestinations.SEARCH) {
            SearchScreen(
                onBack = { popOrNavigateHome() },
                onOpenEditor = { memoId ->
                    navigateSingleTop(QuickMemoDestinations.editorRoute(memoId = memoId))
                },
                onOpenTodo = { navigateSingleTop(QuickMemoDestinations.TODO) },
                onOpenSettings = { navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 0)) },
                onRequestUnlockMemo = { _, onResult ->
                    onRequestBiometric("ロックされたメモを開く") { passed ->
                        onResult(passed)
                    }
                },
            )
        }

        composable(
            route = QuickMemoDestinations.SETTINGS,
            arguments = listOf(
                navArgument("startTab") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            val startTab = backStackEntry.arguments?.getInt("startTab") ?: 0
            SettingsScreen(
                initialTab = startTab,
                onBack = { popOrNavigateHome() },
                onOpenTrash = { navigateSingleTop(QuickMemoDestinations.TRASH) },
                onOpenPremium = { navigateSingleTop(QuickMemoDestinations.PREMIUM) },
                onOpenTodo = { navigateSingleTop(QuickMemoDestinations.TODO) },
                onToggleQuickService = onToggleQuickService,
                onToggleTodoReminder = onToggleTodoReminder,
            )
        }

        composable(route = QuickMemoDestinations.TRASH) {
            TrashScreen(
                onBack = { popOrNavigateHome() },
                onOpenTodo = { navigateSingleTop(QuickMemoDestinations.TODO) },
                onOpenSettings = { navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 0)) },
            )
        }

        composable(route = QuickMemoDestinations.TODO) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Todo") },
                        navigationIcon = {
                            IconButton(onClick = { popOrNavigateHome() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "back",
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { navigateSingleTop(QuickMemoDestinations.HOME) }) {
                                Icon(
                                    imageVector = Icons.Outlined.StickyNote2,
                                    contentDescription = "memo",
                                )
                            }
                            IconButton(onClick = { navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 2)) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "settings",
                                )
                            }
                        },
                    )
                },
            ) { paddingValues ->
                TodoScreen(
                    paddingValues = paddingValues,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(route = QuickMemoDestinations.PREMIUM) {
            PremiumScreen(
                onBack = { popOrNavigateHome() },
                onOpenTodo = { navigateSingleTop(QuickMemoDestinations.TODO) },
                onOpenSettings = { navigateSingleTop(QuickMemoDestinations.settingsRoute(startTab = 0)) },
            )
        }
    }
}

private const val TAG = "QuickMemoNav"
