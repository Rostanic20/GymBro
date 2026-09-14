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
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate

enum class WorkoutStatus { NOT_STARTED, IN_PROGRESS, LOGGED }

data class TodayUiState(
    val date: LocalDate,
    val programStart: LocalDate?,
    val week: Int?,
    val workout: WorkoutDay?,
    val workoutStatus: WorkoutStatus,
    val targets: NutritionTargets,
    val suggestedStart: LocalDate,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val profileRepository: ProfileRepository,
    programRepository: ProgramRepository,
    sessionRepository: SessionRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<TodayUiState?> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            dates.todayFlow(),
            sessionRepository.activeSession(),
            dates.todayFlow().flatMapLatest { sessionRepository.sessionsOn(it) },
        ) { profile, days, today, active, todaysSessions ->
            val week = profile.weekOn(today)
            val workout = days.forDate(today)?.forWeek(week)
            TodayUiState(
                date = today,
                programStart = profile.programStart,
                week = week,
                workout = workout,
                workoutStatus = when {
                    workout == null -> WorkoutStatus.NOT_STARTED
                    active?.dayId == workout.id -> WorkoutStatus.IN_PROGRESS
                    todaysSessions.any { it.dayId == workout.id && !it.isActive } -> WorkoutStatus.LOGGED
                    else -> WorkoutStatus.NOT_STARTED
                },
                targets = profile.nutritionTargets(week),
                suggestedStart = programStartFor(today),
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun startProgram() {
        launchReporting(_messages) { profileRepository.setProgramStart(programStartFor(dates.today())) }
    }
}
