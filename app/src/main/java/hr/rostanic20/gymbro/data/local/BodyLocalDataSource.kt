package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.BodyWeightEntity
import hr.rostanic20.gymbro.data.PhotoEntity
import hr.rostanic20.gymbro.data.WaistEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface BodyLocalDataSource {
    fun weightsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<BodyWeightEntity>>
    fun waistsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<WaistEntity>>
    suspend fun upsertWeight(epochDay: Long, weightKg: Double)
    suspend fun deleteWeight(epochDay: Long)
    suspend fun upsertWaist(epochDay: Long, waistCm: Double)
}

class BodyLocalDataSourceImpl(
    db: AppDb,
    dispatchers: DispatcherProvider,
) : BodyLocalDataSource {

    private val query = db.bodyQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun weightsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<BodyWeightEntity>> =
        query.selectBetween(fromEpochDay, toEpochDay).asFlow().map { it.awaitAsList() }

    override fun waistsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<WaistEntity>> =
        query.selectWaistBetween(fromEpochDay, toEpochDay).asFlow().map { it.awaitAsList() }

    override suspend fun upsertWeight(epochDay: Long, weightKg: Double): Unit = withContext(writeContext) {
        query.upsert(epochDay, weightKg)
    }

    override suspend fun deleteWeight(epochDay: Long): Unit = withContext(writeContext) {
        query.deleteForDate(epochDay)
    }

    override suspend fun upsertWaist(epochDay: Long, waistCm: Double): Unit = withContext(writeContext) {
        query.upsertWaist(epochDay, waistCm)
    }
}

interface PhotoLocalDataSource {
    fun photos(): Flow<List<PhotoEntity>>
    suspend fun insertPhoto(epochDay: Long, pose: String, fileName: String, takenAt: Long)
    suspend fun deletePhoto(id: Long)
}

class PhotoLocalDataSourceImpl(
    db: AppDb,
    dispatchers: DispatcherProvider,
) : PhotoLocalDataSource {

    private val query = db.bodyQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun photos(): Flow<List<PhotoEntity>> =
        query.selectPhotos().asFlow().map { it.awaitAsList() }

    override suspend fun insertPhoto(epochDay: Long, pose: String, fileName: String, takenAt: Long): Unit =
        withContext(writeContext) {
            query.insertPhoto(epochDay, pose, fileName, takenAt)
        }

    override suspend fun deletePhoto(id: Long): Unit = withContext(writeContext) {
        query.deletePhoto(id)
    }
}
