package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.BodyWeight
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BodyRepository {
    fun weights(from: LocalDate, to: LocalDate): Flow<List<BodyWeight>>
    suspend fun setWeight(date: LocalDate, weightKg: Double)
    suspend fun clearWeight(date: LocalDate)
}
