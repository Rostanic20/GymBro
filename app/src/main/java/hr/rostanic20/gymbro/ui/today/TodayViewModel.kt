package hr.rostanic20.gymbro.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.safeLaunch
import hr.rostanic20.gymbro.domain.forDate
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.domain.programWeek
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate,
    val programStarted: Boolean,
    val week: Int?,
    val workout: WorkoutDay?,
    val targets: NutritionTargets,
)

class TodayViewModel(
    private val profileRepository: ProfileRepository,
    programRepository: ProgramRepository,
    private val dates: DateProvider,
) : ViewModel() {

    val state: StateFlow<TodayUiState?> =
        combine(profileRepository.profile(), programRepository.workoutDays()) { profile, days ->
            val today = dates.today()
            val week = profile.programStart?.let { programWeek(it, today) }
            TodayUiState(
                date = today,
                programStarted = profile.programStart != null,
                week = week,
                workout = days.forDate(today),
                targets = profile.nutritionTargets(week),
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun startProgram() {
        safeLaunch { profileRepository.setProgramStart(dates.today()) }
    }
}
