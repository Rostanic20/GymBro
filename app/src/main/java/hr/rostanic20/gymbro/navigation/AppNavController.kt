package hr.rostanic20.gymbro.navigation

import androidx.navigation3.runtime.NavBackStack

class AppNavController(
    startDestination: AppDestination = AppDestination.Today,
) {
    private val rootDestination = startDestination
    private val backStackState = NavBackStack<AppDestination>(startDestination)

    val backStack: List<AppDestination>
        get() = backStackState

    val currentDestination: AppDestination
        get() = backStackState.lastOrNull() ?: rootDestination

    fun navigate(destination: AppDestination) {
        if (backStackState.lastOrNull() == destination) return
        backStackState.add(destination)
    }

    fun navigateToTop(destination: AppDestination) {
        if (currentDestination == destination) return
        backStackState.clear()
        backStackState.add(destination)
    }

    fun popBackStack(): Boolean {
        if (backStackState.size <= 1) return false
        backStackState.removeAt(backStackState.lastIndex)
        return true
    }
}
