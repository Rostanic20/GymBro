package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.DataStore
import hr.rostanic20.gymbro.core.DispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ProfileLocalDataSource {
    fun profile(): Flow<UserProfile>
    suspend fun setProgramStart(epochDay: Long?)
    suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int)
    suspend fun setMealReminders(enabled: Boolean)
    suspend fun adjustCalories(deltaKcal: Int, epochDay: Long, allowed: IntRange)
}

class ProfileLocalDataSourceImpl(
    private val ds: DataStore<UserProfile>,
    dispatchers: DispatcherProvider,
) : ProfileLocalDataSource {

    private val writeContext = dispatchers.io + NonCancellable

    override fun profile(): Flow<UserProfile> = ds.data

    override suspend fun setProgramStart(epochDay: Long?): Unit = withContext(writeContext) {
        ds.updateData { it.copy(programStartEpochDay = epochDay) }
    }

    override suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int): Unit =
        withContext(writeContext) {
            ds.updateData {
                it.copy(maintenanceKcal = maintenanceKcal, surplusKcal = surplusKcal, proteinG = proteinG, fatG = fatG)
            }
        }

    override suspend fun setMealReminders(enabled: Boolean): Unit = withContext(writeContext) {
        ds.updateData { it.copy(mealRemindersEnabled = enabled) }
    }

    override suspend fun adjustCalories(deltaKcal: Int, epochDay: Long, allowed: IntRange): Unit =
        withContext(writeContext) {
            ds.updateData {
                if (it.lastCalorieAdjustmentEpochDay == epochDay) {
                    it
                } else {
                    it.copy(
                        kcalAdjustment = (it.kcalAdjustment + deltaKcal).coerceIn(allowed),
                        lastCalorieAdjustmentEpochDay = epochDay,
                    )
                }
            }
        }
}
