package hr.rostanic20.gymbro.navigation

import androidx.compose.runtime.MutableIntState
import androidx.navigation3.runtime.NavKey

class AppNavigator(
    private val tabs: List<NavKey>,
    private val backStacks: Map<NavKey, MutableList<NavKey>>,
    private val currentTabIndex: MutableIntState,
) {
    init {
        require(tabs.isNotEmpty() && tabs.all { backStacks[it]?.firstOrNull() == it }) {
            "Every tab needs a back stack rooted at the tab itself"
        }
    }

    val currentTab: NavKey get() = tabs[currentTabIndex.intValue]

    val isAtTabRootAwayFromHome: Boolean
        get() = currentBackStack.size == 1 && currentTabIndex.intValue != HOME_INDEX

    private val currentBackStack: MutableList<NavKey> get() = backStacks.getValue(currentTab)

    fun backStack(tab: NavKey): List<NavKey> = backStacks.getValue(tab)

    fun selectTab(tab: NavKey) {
        val index = tabs.indexOf(tab)
        require(index >= 0) { "$tab is not a tab" }
        if (index == currentTabIndex.intValue) popToRoot() else currentTabIndex.intValue = index
    }

    fun navigate(destination: NavKey) {
        if (currentBackStack.last() != destination) currentBackStack.add(destination)
    }

    fun goBack(): Boolean = when {
        currentBackStack.size > 1 -> {
            currentBackStack.removeAt(currentBackStack.lastIndex)
            true
        }
        currentTabIndex.intValue != HOME_INDEX -> {
            currentTabIndex.intValue = HOME_INDEX
            true
        }
        else -> false
    }

    private fun popToRoot() {
        while (currentBackStack.size > 1) currentBackStack.removeAt(currentBackStack.lastIndex)
    }

    private companion object {
        const val HOME_INDEX = 0
    }
}
