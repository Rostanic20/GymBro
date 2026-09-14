package hr.rostanic20.gymbro.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek

data class TrainUiState(
    val days: List<WorkoutDay>,
    val today: DayOfWeek,
)

class TrainViewModel(
    programRepository: ProgramRepository,
    dates: DateProvider,
) : ViewModel() {

    val state: StateFlow<TrainUiState?> =
        combine(programRepository.workoutDays(), dates.todayFlow()) { days, today ->
            TrainUiState(days = days, today = today.dayOfWeek)
        }.stateInWhileSubscribed(viewModelScope, null)
}
