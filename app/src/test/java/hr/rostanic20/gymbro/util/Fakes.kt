package hr.rostanic20.gymbro.util

import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.LocalDate

val defaultProfile = Profile(
    programStart = null,
    maintenanceKcal = 2450,
    surplusKcal = 350,
    kcalAdjustment = 0,
    proteinG = 145,
    fatG = 75,
)

class FakeProfileRepository(initial: Profile = defaultProfile) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    val current: Profile get() = state.value
    var failWrites = false

    override fun profile(): Flow<Profile> = state

    override suspend fun setProgramStart(date: LocalDate?) {
        if (failWrites) throw IOException("disk full")
        state.update { it.copy(programStart = date) }
    }

    override suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int) {
        if (failWrites) throw IOException("disk full")
        state.update {
            it.copy(maintenanceKcal = maintenanceKcal, surplusKcal = surplusKcal, proteinG = proteinG, fatG = fatG)
        }
    }
}

class FakeProgramRepository(days: List<WorkoutDay> = emptyList()) : ProgramRepository {
    private val state = MutableStateFlow(days)
    val loadUpdates = mutableListOf<Triple<Long, Double?, Double?>>()
    var failWrites = false

    override fun workoutDays(): Flow<List<WorkoutDay>> = state

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?) {
        if (failWrites) throw IOException("disk full")
        loadUpdates += Triple(exerciseId, startLoadKg, incrementKg)
    }
}

class FakeDateProvider(date: LocalDate) : DateProvider {
    private val state = MutableStateFlow(date)

    var date: LocalDate
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun today(): LocalDate = state.value

    override fun todayFlow(): Flow<LocalDate> = state
}
