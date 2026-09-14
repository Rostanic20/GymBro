package hr.rostanic20.gymbro.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

data class TrainUiState(
    val days: List<WorkoutDay>,
    val today: DayOfWeek,
)

class TrainViewModel(
    programRepository: ProgramRepository,
    dates: DateProvider,
) : ViewModel() {

    val state: StateFlow<TrainUiState?> = programRepository.workoutDays()
        .map { TrainUiState(days = it, today = dates.today().dayOfWeek) }
        .stateInWhileSubscribed(viewModelScope, null)
}
