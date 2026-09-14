package hr.rostanic20.gymbro.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.isDeloadWeek
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.model.WorkoutSession
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow

data class WorkoutUiState(
    val day: WorkoutDay?,
    val week: Int?,
    val activeSession: WorkoutSession?,
    val activeSessionDayName: String?,
    val loggedToday: Boolean,
)

sealed interface WorkoutEvent {
    data class OpenSession(val sessionId: Long) : WorkoutEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val dayId: Long,
    private val profileRepository: ProfileRepository,
    private val programRepository: ProgramRepository,
    private val sessionRepository: SessionRepository,
    private val dates: DateProvider,
    private val clock: WallClock,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    val state: StateFlow<WorkoutUiState?> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            dates.todayFlow(),
            sessionRepository.activeSession(),
            dates.todayFlow().flatMapLatest { sessionRepository.sessionsOn(it) },
        ) { profile, days, today, active, todaysSessions ->
            val week = profile.weekOn(today)
            WorkoutUiState(
                day = days.firstOrNull { it.id == dayId }?.forWeek(week),
                week = week,
                activeSession = active,
                activeSessionDayName = active?.let { session -> days.firstOrNull { it.id == session.dayId }?.name },
                loggedToday = todaysSessions.any { it.dayId == dayId && !it.isActive },
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun startSession() {
        launchReporting(_messages) {
            val active = sessionRepository.activeSession().first()
            val sessionId = when {
                active == null -> {
                    val today = dates.today()
                    val week = profileRepository.profile().first().weekOn(today)
                    sessionRepository.startSession(dayId, today, isDeloadWeek(week), clock.nowMillis())
                }
                active.dayId == dayId -> active.id
                else -> return@launchReporting
            }
            _events.send(WorkoutEvent.OpenSession(sessionId))
        }
    }

    fun updateLoadSettings(exerciseId: Long, settings: LoadSettings) {
        launchReporting(_messages) {
            programRepository.updateLoadSettings(exerciseId, settings.startLoadKg, settings.incrementKg)
        }
    }
}
