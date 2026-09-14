package hr.rostanic20.gymbro.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.TopSetPoint
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.nextFrom
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import hr.rostanic20.gymbro.ui.workout.LoadSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.DayOfWeek

data class LiftHistory(
    val exercise: Exercise,
    val points: List<TopSetPoint>,
)

data class TrainUiState(
    val days: List<WorkoutDay>,
    val today: DayOfWeek,
    val week: Int?,
    val defaultExpandedDayId: Long?,
    val liftHistory: List<LiftHistory> = emptyList(),
)

private const val LIFT_HISTORY_SESSIONS = 12

@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModel(
    profileRepository: ProfileRepository,
    private val programRepository: ProgramRepository,
    sessionRepository: SessionRepository,
    dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val liftHistory: Flow<List<LiftHistory>> =
        programRepository.workoutDays()
            .map { days ->
                days.flatMap { day -> day.exercises.filter { it.isTop }.map { it.exercise } }.distinctBy { it.id }
            }
            .distinctUntilChanged()
            .flatMapLatest { lifts ->
                if (lifts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        lifts.map { lift ->
                            sessionRepository.topSetHistory(lift.id, LIFT_HISTORY_SESSIONS).map { LiftHistory(lift, it) }
                        },
                    ) { it.toList() }
                }
            }

    val state: StateFlow<TrainUiState?> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            dates.todayFlow(),
            liftHistory,
        ) { profile, days, today, history ->
            val week = profile.weekOn(today)
            TrainUiState(
                days = days.map { it.forWeek(week) },
                today = today.dayOfWeek,
                week = week,
                defaultExpandedDayId = days.nextFrom(today)?.id,
                liftHistory = history,
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun updateLoadSettings(exerciseId: Long, settings: LoadSettings) {
        launchReporting(_messages) {
            programRepository.updateLoadSettings(exerciseId, settings.startLoadKg, settings.incrementKg)
        }
    }
}
