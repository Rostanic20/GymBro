package hr.rostanic20.gymbro.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    private data object Home : NavKey
    private data object Train : NavKey
    private data object Settings : NavKey
    private data class Detail(val id: Int) : NavKey

    private val stacks: Map<NavKey, MutableList<NavKey>> = mapOf(
        Home to mutableListOf(Home),
        Train to mutableListOf(Train),
        Settings to mutableListOf(Settings),
    )
    private val navigator = AppNavigator(listOf(Home, Train, Settings), stacks, mutableIntStateOf(0))

    @Test
    fun `starts on the home tab`() {
        assertEquals(Home, navigator.currentTab)
        assertFalse(navigator.isAtTabRootAwayFromHome)
    }

    @Test
    fun `navigate pushes onto the current tab only`() {
        navigator.navigate(Detail(1))

        assertEquals(listOf(Home, Detail(1)), navigator.backStack(Home))
        assertEquals(listOf(Train), navigator.backStack(Train))
    }

    @Test
    fun `navigating to the screen already on top does not stack it twice`() {
        navigator.navigate(Detail(1))
        navigator.navigate(Detail(1))

        assertEquals(listOf(Home, Detail(1)), navigator.backStack(Home))
    }

    @Test
    fun `switching tabs keeps every tab's stack`() {
        navigator.navigate(Detail(1))
        navigator.selectTab(Train)
        navigator.navigate(Detail(2))
        navigator.selectTab(Home)

        assertEquals(Home, navigator.currentTab)
        assertEquals(listOf(Home, Detail(1)), navigator.backStack(Home))
        assertEquals(listOf(Train, Detail(2)), navigator.backStack(Train))
    }

    @Test
    fun `back pops inside the current tab first`() {
        navigator.selectTab(Train)
        navigator.navigate(Detail(2))

        assertTrue(navigator.goBack())

        assertEquals(Train, navigator.currentTab)
        assertEquals(listOf(Train), navigator.backStack(Train))
    }

    @Test
    fun `back at another tab's root returns home without clearing that tab`() {
        navigator.navigate(Detail(1))
        navigator.selectTab(Settings)

        assertTrue(navigator.isAtTabRootAwayFromHome)
        assertTrue(navigator.goBack())

        assertEquals(Home, navigator.currentTab)
        assertEquals(listOf(Home, Detail(1)), navigator.backStack(Home))
    }

    @Test
    fun `back at the home root is left to the system`() {
        assertFalse(navigator.goBack())
        assertEquals(Home, navigator.currentTab)
    }

    @Test
    fun `reselecting the current tab pops it to its root`() {
        navigator.navigate(Detail(1))
        navigator.navigate(Detail(2))

        navigator.selectTab(Home)

        assertEquals(listOf(Home), navigator.backStack(Home))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a tab without its own rooted stack is rejected`() {
        AppNavigator(listOf(Home, Train), mapOf(Home to mutableListOf(Home)), mutableIntStateOf(0))
    }
}
