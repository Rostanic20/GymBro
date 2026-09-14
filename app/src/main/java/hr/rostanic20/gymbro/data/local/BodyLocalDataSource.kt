package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.BodyWeightEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface BodyLocalDataSource {
    fun weightsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<BodyWeightEntity>>
    suspend fun upsertWeight(epochDay: Long, weightKg: Double)
    suspend fun deleteWeight(epochDay: Long)
}

class BodyLocalDataSourceImpl(
    db: AppDb,
    dispatchers: DispatcherProvider,
) : BodyLocalDataSource {

    private val query = db.bodyQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun weightsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<BodyWeightEntity>> =
        query.selectBetween(fromEpochDay, toEpochDay).asFlow().map { it.awaitAsList() }

    override suspend fun upsertWeight(epochDay: Long, weightKg: Double): Unit = withContext(writeContext) {
        query.upsert(epochDay, weightKg)
    }

    override suspend fun deleteWeight(epochDay: Long): Unit = withContext(writeContext) {
        query.deleteForDate(epochDay)
    }
}
