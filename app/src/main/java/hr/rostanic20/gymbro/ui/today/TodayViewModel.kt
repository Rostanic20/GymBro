package hr.rostanic20.gymbro.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.averageForWeekEnding
import hr.rostanic20.gymbro.domain.forDate
import hr.rostanic20.gymbro.domain.forWeek
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.domain.programStartFor
import hr.rostanic20.gymbro.domain.repository.BodyRepository
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.domain.total
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.domain.weeklyChange
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class WorkoutStatus { NOT_STARTED, IN_PROGRESS, LOGGED }

data class TodayUiState(
    val date: LocalDate,
    val programStart: LocalDate?,
    val week: Int?,
    val workout: WorkoutDay?,
    val workoutStatus: WorkoutStatus,
    val targets: NutritionTargets,
    val suggestedStart: LocalDate,
    val isToday: Boolean = true,
    val mealSettings: MealSettings = MealSettings(),
    val loggedSessionId: Long? = null,
    val eaten: Nutrition = Nutrition.ZERO,
    val eatenByMeal: Map<Meal, Nutrition> = emptyMap(),
    val weightTodayKg: Double? = null,
    val weekAverageKg: Double? = null,
    val weeklyChangeKg: Double? = null,
)

private const val WEIGHT_HISTORY_DAYS = 13L
private const val MAX_DAYS_BACK = 365L

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val profileRepository: ProfileRepository,
    programRepository: ProgramRepository,
    sessionRepository: SessionRepository,
    foodRepository: FoodRepository,
    private val bodyRepository: BodyRepository,
    mealSettingsRepository: MealSettingsRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val daysBack = MutableStateFlow(0L)

    private val selectedDate: Flow<LocalDate> =
        combine(dates.todayFlow(), daysBack) { today, back -> today.minusDays(back) }.distinctUntilChanged()

    private val plan: Flow<TodayUiState> =
        combine(
            profileRepository.profile(),
            programRepository.workoutDays(),
            selectedDate,
            sessionRepository.activeSession(),
            selectedDate.flatMapLatest { sessionRepository.sessionsOn(it) },
        ) { profile, days, date, active, sessions ->
            val week = profile.weekOn(date)
            val workout = days.forDate(date)?.forWeek(week)
            val logged = sessions.firstOrNull { it.dayId == workout?.id && !it.isActive }
            TodayUiState(
                date = date,
                programStart = profile.programStart,
                week = week,
                workout = workout,
                workoutStatus = when {
                    workout == null -> WorkoutStatus.NOT_STARTED
                    active?.dayId == workout.id -> WorkoutStatus.IN_PROGRESS
                    logged != null -> WorkoutStatus.LOGGED
                    else -> WorkoutStatus.NOT_STARTED
                },
                targets = profile.nutritionTargets(week),
                suggestedStart = programStartFor(date),
                loggedSessionId = logged?.id,
            )
        }

    val state: StateFlow<TodayUiState?> =
        combine(
            plan,
            selectedDate.flatMapLatest { foodRepository.log(it) },
            selectedDate.flatMapLatest { bodyRepository.weights(it.minusDays(WEIGHT_HISTORY_DAYS), it) },
            dates.todayFlow(),
            mealSettingsRepository.settings(),
        ) { plan, log, weights, today, mealSettings ->
            plan.copy(
                isToday = plan.date == today,
                mealSettings = mealSettings,
                eaten = log.total(),
                eatenByMeal = log.groupBy { it.meal }.mapValues { (_, entries) -> entries.total() },
                weightTodayKg = weights.firstOrNull { it.date == plan.date }?.weightKg,
                weekAverageKg = weights.averageForWeekEnding(plan.date),
                weeklyChangeKg = weights.weeklyChange(plan.date),
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun showPreviousDay() {
        daysBack.update { (it + 1).coerceAtMost(MAX_DAYS_BACK) }
    }

    fun showNextDay() {
        daysBack.update { (it - 1).coerceAtLeast(0) }
    }

    fun showToday() {
        daysBack.value = 0
    }

    fun showDate(date: LocalDate) {
        daysBack.value = ChronoUnit.DAYS.between(date, dates.today()).coerceIn(0, MAX_DAYS_BACK)
    }

    fun startProgram() {
        launchReporting(_messages) { profileRepository.setProgramStart(programStartFor(dates.today())) }
    }

    fun saveWeight(weightKg: Double) {
        val date = dates.today().minusDays(daysBack.value)
        launchReporting(_messages) { bodyRepository.setWeight(date, weightKg) }
    }
}
