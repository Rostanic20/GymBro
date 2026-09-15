package hr.rostanic20.gymbro.ui.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow

private val DEFAULT_REPS = 8..12
private const val DEFAULT_SETS = 3

data class EditDayUiState(
    val day: WorkoutDay?,
    val addable: List<Exercise>,
)

class EditDayViewModel(
    private val dayId: Long,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<EditDayUiState?> =
        combine(programRepository.editableDays(), programRepository.exercises()) { days, exercises ->
            val day = days.firstOrNull { it.id == dayId }
            val alreadyIn = day?.exercises.orEmpty().map { it.exercise.id }.toSet()
            EditDayUiState(day = day, addable = exercises.filterNot { it.id in alreadyIn })
        }.stateInWhileSubscribed(viewModelScope, null)

    fun savePrescription(position: Int, prescription: Prescription) {
        launchReporting(_messages) {
            programRepository.updatePrescription(dayId, position, prescription.sets, prescription.reps)
        }
    }

    fun setHidden(position: Int, hidden: Boolean) {
        launchReporting(_messages) { programRepository.setExerciseHidden(dayId, position, hidden) }
    }

    fun addExercise(exerciseId: Long) {
        launchReporting(_messages) { programRepository.addExercise(dayId, exerciseId, DEFAULT_SETS, DEFAULT_REPS) }
    }
}
