package hr.rostanic20.gymbro.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.domain.model.SessionSummary
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

private const val HISTORY_LIMIT = 100

data class HistoryItem(
    val session: SessionSummary,
    val dayName: String?,
    val emphasis: String?,
)

class HistoryViewModel(
    sessionRepository: SessionRepository,
    programRepository: ProgramRepository,
) : ViewModel() {

    val state: StateFlow<List<HistoryItem>?> =
        combine(
            sessionRepository.recentSessions(HISTORY_LIMIT),
            programRepository.workoutDays(),
        ) { sessions, days ->
            val daysById = days.associateBy { it.id }
            sessions.map { session ->
                val day = daysById[session.dayId]
                HistoryItem(session = session, dayName = day?.name, emphasis = day?.emphasis)
            }
        }.stateInWhileSubscribed(viewModelScope, null)
}
