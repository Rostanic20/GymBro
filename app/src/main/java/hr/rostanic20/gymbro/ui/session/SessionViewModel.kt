package hr.rostanic20.gymbro.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.RestTimer
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.DELOAD_EVERY_WEEKS
import hr.rostanic20.gymbro.domain.STALL_SESSIONS
import hr.rostanic20.gymbro.domain.Suggestion
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.model.WorkoutSession
import hr.rostanic20.gymbro.domain.needsDeload
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.domain.suggestNext
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class SlotState(
    val planned: PlannedExercise,
    val exercise: Exercise,
    val sets: List<LoggedSet>,
    val lastSets: List<LoggedSet>,
    val suggestion: Suggestion,
) {
    val options: List<Exercise> get() = listOf(planned.exercise) + planned.alternatives.map { it.exercise }
    val canSwap: Boolean get() = planned.alternatives.isNotEmpty() && sets.isEmpty()
}

data class SessionUiState(
    val session: WorkoutSession,
    val dayName: String,
    val dayEmphasis: String,
    val slots: List<SlotState>,
    val deloadAdvised: Boolean,
)

sealed interface SessionEvent {
    data object Closed : SessionEvent
}

private data class DayContext(val day: WorkoutDay, val week: Int?)

private data class History(val lastSets: Map<Long, List<LoggedSet>>, val deloadAdvised: Boolean)

private const val MILLIS_PER_SECOND = 1_000L
private const val REST_EXTENSION_MILLIS = 30_000L
private const val NOTE_SAVE_DEBOUNCE_MILLIS = 600L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SessionViewModel(
    private val sessionId: Long,
    private val sessionRepository: SessionRepository,
    programRepository: ProgramRepository,
    profileRepository: ProfileRepository,
    private val restTimer: RestTimer,
    private val clock: WallClock,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val _events = Channel<SessionEvent>(Channel.BUFFERED)
    val events: Flow<SessionEvent> = _events.receiveAsFlow()

    private val swaps = MutableStateFlow<Map<Int, Long>>(emptyMap())
    private val noteDrafts = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var restExerciseName: String? = null
    private var closing = false

    private val session: Flow<WorkoutSession> = sessionRepository.session(sessionId).filterNotNull()
    private val sets: Flow<List<LoggedSet>> = sessionRepository.sets(sessionId)

    private val dayContext: Flow<DayContext> =
        combine(session, programRepository.workoutDays(), profileRepository.profile()) { session, days, profile ->
            days.firstOrNull { it.id == session.dayId }?.let { day ->
                DayContext(
                    day = day.forWeek(if (session.isDeload) DELOAD_EVERY_WEEKS else null),
                    week = profile.weekOn(session.date),
                )
            }
        }.filterNotNull().distinctUntilChanged()

    private val selected: Flow<Map<Int, Exercise>> =
        combine(dayContext, sets, swaps) { context, sets, swaps ->
            context.day.exercises.associate { planned ->
                planned.position to selectExercise(planned, sets, swaps[planned.position])
            }
        }.distinctUntilChanged()

    private val history: Flow<History> =
        combine(dayContext, selected) { context, selected -> context to selected }
            .mapLatest { (context, selected) ->
                val lastSets = selected.values.distinctBy { it.id }
                    .associate { it.id to sessionRepository.lastSets(it.id, sessionId) }
                val top = context.day.exercises.firstOrNull { it.isTop }?.exercise
                val deloadAdvised = top != null &&
                    needsDeload(top.loadType, sessionRepository.recentTopSets(top.id, STALL_SESSIONS))
                History(lastSets = lastSets, deloadAdvised = deloadAdvised)
            }

    val state: StateFlow<SessionUiState?> =
        combine(session, dayContext, sets, selected, history) { session, context, sets, selected, history ->
            SessionUiState(
                session = session,
                dayName = context.day.name,
                dayEmphasis = context.day.emphasis,
                slots = context.day.exercises.map { planned ->
                    val exercise = selected[planned.position] ?: planned.exercise
                    val lastSets = history.lastSets[exercise.id].orEmpty()
                    SlotState(
                        planned = planned,
                        exercise = exercise,
                        sets = sets.filter { it.slotPosition == planned.position },
                        lastSets = lastSets,
                        suggestion = suggestNext(planned, exercise, lastSets, context.week, session.isDeload),
                    )
                },
                deloadAdvised = history.deloadAdvised,
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    init {
        noteDrafts
            .debounce(NOTE_SAVE_DEBOUNCE_MILLIS)
            .onEach { note -> launchReporting(_messages) { sessionRepository.setNote(sessionId, note) } }
            .launchIn(viewModelScope)
    }

    fun swap(slotPosition: Int, exerciseId: Long) {
        swaps.update { it + (slotPosition to exerciseId) }
    }

    fun logSet(slot: SlotState, values: SetValues) {
        launchReporting(_messages) {
            val now = clock.nowMillis()
            sessionRepository.logSet(sessionId, slot.exercise.id, slot.planned.position, values, now)
            startRest(now + slot.planned.restSeconds.last * MILLIS_PER_SECOND, slot.exercise.name)
        }
    }

    fun updateSet(setId: Long, values: SetValues) {
        launchReporting(_messages) { sessionRepository.updateSet(setId, values) }
    }

    fun deleteSet(setId: Long) {
        launchReporting(_messages) { sessionRepository.deleteSet(setId) }
    }

    fun extendRest() {
        launchReporting(_messages) {
            val now = clock.nowMillis()
            val current = sessionRepository.session(sessionId).first()?.restEndsAtMillis ?: now
            startRest(maxOf(current, now) + REST_EXTENSION_MILLIS, restExerciseName.orEmpty())
        }
    }

    fun skipRest() {
        launchReporting(_messages) {
            restTimer.cancel()
            sessionRepository.setRestEndsAt(sessionId, null)
        }
    }

    fun onNoteChange(note: String) {
        noteDrafts.tryEmit(note)
    }

    fun finish(note: String) {
        close {
            sessionRepository.setNote(sessionId, note)
            sessionRepository.finishSession(sessionId, clock.nowMillis())
        }
    }

    fun discard() {
        close { sessionRepository.discardSession(sessionId) }
    }

    private fun close(write: suspend () -> Unit) {
        if (closing) return
        closing = true
        launchReporting(_messages) {
            try {
                restTimer.cancel()
                write()
            } catch (e: Exception) {
                closing = false
                throw e
            }
            _events.send(SessionEvent.Closed)
        }
    }

    private suspend fun startRest(endsAtMillis: Long, exerciseName: String) {
        sessionRepository.setRestEndsAt(sessionId, endsAtMillis)
        restExerciseName = exerciseName
        restTimer.schedule(endsAtMillis, exerciseName)
    }
}

private fun selectExercise(planned: PlannedExercise, sets: List<LoggedSet>, swappedId: Long?): Exercise {
    val options = listOf(planned.exercise) + planned.alternatives.map { it.exercise }
    val loggedId = sets.firstOrNull { it.slotPosition == planned.position }?.exerciseId
    return options.firstOrNull { it.id == (loggedId ?: swappedId) } ?: planned.exercise
}
