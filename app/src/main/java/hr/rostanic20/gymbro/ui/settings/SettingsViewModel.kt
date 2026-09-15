package hr.rostanic20.gymbro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.programStartFor
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import java.time.LocalTime

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val mealSettingsRepository: MealSettingsRepository,
    private val mealReminders: MealReminderScheduler,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val profile: StateFlow<Profile?> =
        profileRepository.profile().stateInWhileSubscribed(viewModelScope, null)

    val mealSettings: StateFlow<MealSettings> =
        mealSettingsRepository.settings().stateInWhileSubscribed(viewModelScope, MealSettings())

    fun changeProgramStart(date: LocalDate) {
        launchReporting(_messages) { profileRepository.setProgramStart(programStartFor(date)) }
    }

    fun resetProgram() {
        launchReporting(_messages) { profileRepository.setProgramStart(null) }
    }

    fun saveTargets(targets: Targets) {
        launchReporting(_messages) {
            profileRepository.setTargets(targets.maintenanceKcal, targets.surplusKcal, targets.proteinG, targets.fatG)
            _messages.send(UserMessage.TargetsSaved)
        }
    }

    fun setMealReminders(enabled: Boolean) {
        launchReporting(_messages) {
            profileRepository.setMealReminders(enabled)
            mealReminders.reschedule(enabled)
        }
    }

    fun setMealTime(meal: Meal, time: LocalTime) {
        launchReporting(_messages) {
            mealSettingsRepository.setTime(meal, time)
            rescheduleIfOn()
        }
    }

    fun setMealEnabled(meal: Meal, enabled: Boolean) {
        launchReporting(_messages) {
            mealSettingsRepository.setEnabled(meal, enabled)
            rescheduleIfOn()
        }
    }

    private suspend fun rescheduleIfOn() {
        mealReminders.reschedule(profileRepository.profile().first().mealRemindersEnabled)
    }
}
