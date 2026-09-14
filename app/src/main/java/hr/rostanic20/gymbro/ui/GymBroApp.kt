package hr.rostanic20.gymbro.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.navigation.AppDestination
import hr.rostanic20.gymbro.navigation.AppNavController
import hr.rostanic20.gymbro.ui.today.TodayScreen
import hr.rostanic20.gymbro.ui.train.TrainScreen

private data class Tab(
    val destination: AppDestination,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab(AppDestination.Today, R.string.tab_today, Icons.Outlined.Today),
    Tab(AppDestination.Train, R.string.tab_train, Icons.Outlined.FitnessCenter),
    Tab(AppDestination.Body, R.string.tab_body, Icons.Outlined.MonitorWeight),
    Tab(AppDestination.Progress, R.string.tab_progress, Icons.Outlined.Insights),
)

@Composable
fun GymBroApp(navController: AppNavController) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                val root = navController.backStack.first()
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = root == tab.destination,
                        onClick = { navController.navigateToTop(tab.destination) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = navController.backStack,
            onBack = { navController.popBackStack() },
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppDestination.Today> {
                    TodayScreen(onOpenProgram = { navController.navigateToTop(AppDestination.Train) })
                }
                entry<AppDestination.Train> {
                    TrainScreen()
                }
                entry<AppDestination.Body> {
                    PlaceholderScreen(text = stringResource(R.string.body_placeholder))
                }
                entry<AppDestination.Progress> {
                    PlaceholderScreen(text = stringResource(R.string.progress_placeholder))
                }
            },
        )
    }
}
