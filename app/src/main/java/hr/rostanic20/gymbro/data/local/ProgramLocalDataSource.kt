package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.AlternativeRowEntity
import hr.rostanic20.gymbro.data.ProgramRowEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ProgramLocalDataSource {
    fun program(): Flow<List<ProgramRowEntity>>
    fun alternatives(): Flow<List<AlternativeRowEntity>>
    suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?)
}

class ProgramLocalDataSourceImpl(
    db: AppDb,
    private val dispatchers: DispatcherProvider,
) : ProgramLocalDataSource {

    private val query = db.programQueries

    override fun program(): Flow<List<ProgramRowEntity>> =
        query.selectProgram().asFlow().map { it.awaitAsList() }

    override fun alternatives(): Flow<List<AlternativeRowEntity>> =
        query.selectAlternatives().asFlow().map { it.awaitAsList() }

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?): Unit =
        withContext(dispatchers.io + NonCancellable) {
            query.updateLoadSettings(startLoadKg = startLoadKg, incrementKg = incrementKg, id = exerciseId)
        }
}
