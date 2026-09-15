package hr.rostanic20.gymbro.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.navigation.AppDestination
import hr.rostanic20.gymbro.navigation.AppNavigator
import hr.rostanic20.gymbro.navigation.rememberAppNavigator
import hr.rostanic20.gymbro.ui.body.BodyScreen
import hr.rostanic20.gymbro.ui.foods.FoodEditScreen
import hr.rostanic20.gymbro.ui.foods.FoodsScreen
import hr.rostanic20.gymbro.ui.history.HistoryScreen
import hr.rostanic20.gymbro.ui.meal.MealScreen
import hr.rostanic20.gymbro.ui.session.SessionScreen
import hr.rostanic20.gymbro.ui.settings.SettingsScreen
import hr.rostanic20.gymbro.ui.today.TodayScreen
import hr.rostanic20.gymbro.ui.train.TrainScreen
import hr.rostanic20.gymbro.ui.workout.WorkoutScreen

private data class Tab(
    val destination: AppDestination,
    @param:StringRes val label: Int,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab(AppDestination.Today, R.string.tab_today, Icons.Outlined.Today),
    Tab(AppDestination.Train, R.string.tab_train, Icons.Outlined.FitnessCenter),
    Tab(AppDestination.Body, R.string.tab_body, Icons.Outlined.MonitorWeight),
    Tab(AppDestination.Settings, R.string.tab_settings, Icons.Outlined.Settings),
)

@Composable
fun GymBroApp() {
    val navigator = rememberAppNavigator()
    val snackbarHostState = remember { SnackbarHostState() }
    val entriesByTab: Map<NavKey, List<NavEntry<NavKey>>> = tabs.associate { tab ->
        tab.destination to key(tab.destination) {
            rememberTabEntries(navigator.backStack(tab.destination), navigator)
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = navigator.currentTab == tab.destination,
                            onClick = { navigator.selectTab(tab.destination) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavDisplay(
                entries = entriesByTab.getValue(navigator.currentTab),
                modifier = Modifier.padding(innerPadding),
                onBack = { navigator.goBack() },
            )
            BackHandler(enabled = navigator.isAtTabRootAwayFromHome) { navigator.goBack() }
        }
    }
}

@Composable
private fun rememberTabEntries(backStack: List<NavKey>, navigator: AppNavigator): List<NavEntry<NavKey>> =
    rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.Today> {
                TodayScreen(
                    onOpenWorkout = { navigator.navigate(AppDestination.Workout(it)) },
                    onOpenSession = { navigator.navigate(AppDestination.Session(it)) },
                    onOpenMeal = { epochDay, slot -> navigator.navigate(AppDestination.Meal(epochDay, slot)) },
                )
            }
            entry<AppDestination.Train> {
                TrainScreen(onOpenHistory = { navigator.navigate(AppDestination.History) })
            }
            entry<AppDestination.Body> {
                BodyScreen()
            }
            entry<AppDestination.Settings> {
                SettingsScreen(onOpenFoods = { navigator.navigate(AppDestination.Foods) })
            }
            entry<AppDestination.Workout> { destination ->
                WorkoutScreen(
                    dayId = destination.dayId,
                    onBack = { navigator.goBack() },
                    onOpenSession = { navigator.navigate(AppDestination.Session(it)) },
                )
            }
            entry<AppDestination.Session> { destination ->
                SessionScreen(sessionId = destination.sessionId, onClose = { navigator.goBack() })
            }
            entry<AppDestination.Meal> { destination ->
                MealScreen(epochDay = destination.epochDay, slot = destination.slot, onBack = { navigator.goBack() })
            }
            entry<AppDestination.History> {
                HistoryScreen(
                    onBack = { navigator.goBack() },
                    onOpenSession = { navigator.navigate(AppDestination.Session(it)) },
                )
            }
            entry<AppDestination.Foods> {
                FoodsScreen(
                    onBack = { navigator.goBack() },
                    onEditFood = { navigator.navigate(AppDestination.FoodEdit(it)) },
                )
            }
            entry<AppDestination.FoodEdit> { destination ->
                FoodEditScreen(foodId = destination.foodId, onDone = { navigator.goBack() })
            }
        },
    )
