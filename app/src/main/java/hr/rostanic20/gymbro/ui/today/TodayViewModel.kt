package hr.rostanic20.gymbro.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.forDate
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.domain.programStartFor
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate,
    val programStart: LocalDate?,
    val week: Int?,
    val workout: WorkoutDay?,
    val targets: NutritionTargets,
    val suggestedStart: LocalDate,
)

class TodayViewModel(
    private val profileRepository: ProfileRepository,
    programRepository: ProgramRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<TodayUiState?> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            dates.todayFlow(),
        ) { profile, days, today ->
            val week = profile.weekOn(today)
            TodayUiState(
                date = today,
                programStart = profile.programStart,
                week = week,
                workout = days.forDate(today)?.forWeek(week),
                targets = profile.nutritionTargets(week),
                suggestedStart = programStartFor(today),
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun startProgram() {
        launchReporting(_messages) { profileRepository.setProgramStart(programStartFor(dates.today())) }
    }
}
