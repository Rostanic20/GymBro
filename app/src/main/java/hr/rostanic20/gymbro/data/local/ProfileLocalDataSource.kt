package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.DataStore
import hr.rostanic20.gymbro.core.DispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ProfileLocalDataSource {
    fun profile(): Flow<UserProfile>
    suspend fun setProgramStart(epochDay: Long)
}

class ProfileLocalDataSourceImpl(
    private val ds: DataStore<UserProfile>,
    dispatchers: DispatcherProvider,
) : ProfileLocalDataSource {

    private val writeContext = dispatchers.io + NonCancellable

    override fun profile(): Flow<UserProfile> = ds.data

    override suspend fun setProgramStart(epochDay: Long): Unit = withContext(writeContext) {
        ds.updateData { it.copy(programStartEpochDay = epochDay) }
    }
}
