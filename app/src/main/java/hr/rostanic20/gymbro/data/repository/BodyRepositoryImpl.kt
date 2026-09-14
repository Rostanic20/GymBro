package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.BodyLocalDataSource
import hr.rostanic20.gymbro.data.toDomain
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.repository.BodyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BodyRepositoryImpl(
    private val local: BodyLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : BodyRepository {

    override fun weights(from: LocalDate, to: LocalDate): Flow<List<BodyWeight>> =
        local.weightsBetween(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun setWeight(date: LocalDate, weightKg: Double) {
        local.upsertWeight(date.toEpochDay(), weightKg)
    }

    override suspend fun clearWeight(date: LocalDate) {
        local.deleteWeight(date.toEpochDay())
    }
}
