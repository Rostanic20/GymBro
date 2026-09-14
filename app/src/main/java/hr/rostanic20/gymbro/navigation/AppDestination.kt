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
    data object Body : AppDestination

    @Serializable
    data object Progress : AppDestination
}
