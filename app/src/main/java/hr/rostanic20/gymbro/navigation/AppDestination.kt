package hr.rostanic20.gymbro.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Today : AppDestination

    @Serializable
    data object Train : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data class Workout(val dayId: Long) : AppDestination

    @Serializable
    data class Session(val sessionId: Long) : AppDestination

    @Serializable
    data class Meal(val epochDay: Long, val slot: Int) : AppDestination

    @Serializable
    data object Foods : AppDestination

    @Serializable
    data class FoodEdit(val foodId: Long?) : AppDestination
}

val TopLevelDestinations: List<AppDestination> = listOf(
    AppDestination.Today,
    AppDestination.Train,
    AppDestination.Settings,
)
