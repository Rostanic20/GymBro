package hr.rostanic20.gymbro.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.model.WorkoutDay
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

data class WorkoutUiState(
    val day: WorkoutDay?,
    val week: Int?,
)

class WorkoutViewModel(
    private val dayId: Long,
    profileRepository: ProfileRepository,
    private val programRepository: ProgramRepository,
    dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<WorkoutUiState?> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            dates.todayFlow(),
        ) { profile, days, today ->
            val week = profile.weekOn(today)
            WorkoutUiState(day = days.firstOrNull { it.id == dayId }?.forWeek(week), week = week)
        }.stateInWhileSubscribed(viewModelScope, null)

    fun updateLoadSettings(exerciseId: Long, settings: LoadSettings) {
        launchReporting(_messages) {
            programRepository.updateLoadSettings(exerciseId, settings.startLoadKg, settings.incrementKg)
        }
    }
}
