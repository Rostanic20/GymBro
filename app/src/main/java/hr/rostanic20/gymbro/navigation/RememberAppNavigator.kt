package hr.rostanic20.gymbro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
fun rememberAppNavigator(): AppNavigator {
    val backStacks: Map<NavKey, MutableList<NavKey>> = TopLevelDestinations.associate { tab ->
        tab to key(tab) { rememberNavBackStack(tab) }
    }
    val currentTabIndex = rememberSaveable { mutableIntStateOf(0) }
    return remember(backStacks.values.toList(), currentTabIndex) {
        AppNavigator(TopLevelDestinations, backStacks, currentTabIndex)
    }
}
