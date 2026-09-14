package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.DataStore
import hr.rostanic20.gymbro.data.repository.ProfileRepositoryImpl
import hr.rostanic20.gymbro.domain.CALORIE_ADJUSTMENT_RANGE
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProfileAdjustmentTest {

    private class InMemoryDataStore(initial: UserProfile) : DataStore<UserProfile> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UserProfile> = state

        override suspend fun updateData(transform: suspend (t: UserProfile) -> UserProfile): UserProfile {
            state.value = transform(state.value)
            return state.value
        }
    }

    private val today = LocalDate.of(2026, 10, 19)

    private fun TestScope.repository(initial: UserProfile = UserProfile()): ProfileRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return ProfileRepositoryImpl(ProfileLocalDataSourceImpl(InMemoryDataStore(initial), dispatchers))
    }

    @Test
    fun `an adjustment adds to the running total and remembers the day`() = runTest {
        val repository = repository(UserProfile(kcalAdjustment = 150))

        repository.adjustCalories(-150, today)

        val profile = repository.profile().first()
        assertEquals(0, profile.kcalAdjustment)
        assertEquals(today, profile.lastCalorieAdjustment)
    }

    @Test
    fun `a second tap on the same day does not adjust twice`() = runTest {
        val repository = repository()

        repository.adjustCalories(150, today)
        repository.adjustCalories(150, today)

        assertEquals(150, repository.profile().first().kcalAdjustment)
    }

    @Test
    fun `the running adjustment is capped`() = runTest {
        val repository = repository(UserProfile(kcalAdjustment = CALORIE_ADJUSTMENT_RANGE.last))

        repository.adjustCalories(150, today)

        assertEquals(CALORIE_ADJUSTMENT_RANGE.last, repository.profile().first().kcalAdjustment)
    }
}
