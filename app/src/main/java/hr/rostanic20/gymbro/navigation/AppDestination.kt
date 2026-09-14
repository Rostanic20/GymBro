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
}

val TopLevelDestinations: List<AppDestination> = listOf(
    AppDestination.Today,
    AppDestination.Train,
    AppDestination.Settings,
)
