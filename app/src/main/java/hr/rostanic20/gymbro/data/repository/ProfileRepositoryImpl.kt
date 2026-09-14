package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.data.local.ProfileLocalDataSource
import hr.rostanic20.gymbro.data.toDomain
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ProfileRepositoryImpl(
    private val local: ProfileLocalDataSource,
) : ProfileRepository {

    override fun profile(): Flow<Profile> =
        local.profile().map { it.toDomain() }.distinctUntilChanged()

    override suspend fun setProgramStart(date: LocalDate?) {
        local.setProgramStart(date?.toEpochDay())
    }

    override suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int) {
        local.setTargets(maintenanceKcal, surplusKcal, proteinG, fatG)
    }

    override suspend fun setMealReminders(enabled: Boolean) {
        local.setMealReminders(enabled)
    }
}
