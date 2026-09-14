package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.data.ProgramRowEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProgramLocalDataSource {
    fun program(): Flow<List<ProgramRowEntity>>
}

class ProgramLocalDataSourceImpl(db: AppDb) : ProgramLocalDataSource {

    private val query = db.programQueries

    override fun program(): Flow<List<ProgramRowEntity>> =
        query.selectProgram().asFlow().map { it.awaitAsList() }
}
