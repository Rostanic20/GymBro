package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.DataStore
import hr.rostanic20.gymbro.core.DispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface MealSettingsLocalDataSource {
    fun profile(): Flow<UserProfile>
    suspend fun setTime(slot: Int, time: String)
    suspend fun setEnabled(slot: Int, enabled: Boolean)
}

class MealSettingsLocalDataSourceImpl(
    private val ds: DataStore<UserProfile>,
    dispatchers: DispatcherProvider,
) : MealSettingsLocalDataSource {

    private val writeContext = dispatchers.io + NonCancellable

    override fun profile(): Flow<UserProfile> = ds.data

    override suspend fun setTime(slot: Int, time: String): Unit = withContext(writeContext) {
        ds.updateData { it.copy(mealTimes = it.mealTimes + (slot to time)) }
    }

    override suspend fun setEnabled(slot: Int, enabled: Boolean): Unit = withContext(writeContext) {
        ds.updateData {
            it.copy(disabledMeals = if (enabled) it.disabledMeals - slot else it.disabledMeals + slot)
        }
    }
}
