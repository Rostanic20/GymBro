package hr.rostanic20.gymbro.util

import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.core.RestTimer
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.model.WorkoutSession
import hr.rostanic20.gymbro.domain.nutritionFor
import hr.rostanic20.gymbro.domain.repository.BodyRepository
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.LocalDate

val defaultProfile = Profile(
    programStart = null,
    maintenanceKcal = 2450,
    surplusKcal = 350,
    kcalAdjustment = 0,
    proteinG = 145,
    fatG = 75,
)

private fun failIf(fail: Boolean) {
    if (fail) throw IOException("disk full")
}

class FakeProfileRepository(initial: Profile = defaultProfile) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    val current: Profile get() = state.value
    var failWrites = false

    override fun profile(): Flow<Profile> = state

    override suspend fun setProgramStart(date: LocalDate?) {
        failIf(failWrites)
        state.update { it.copy(programStart = date) }
    }

    override suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int) {
        failIf(failWrites)
        state.update {
            it.copy(maintenanceKcal = maintenanceKcal, surplusKcal = surplusKcal, proteinG = proteinG, fatG = fatG)
        }
    }

    override suspend fun setMealReminders(enabled: Boolean) {
        failIf(failWrites)
        state.update { it.copy(mealRemindersEnabled = enabled) }
    }
}

class FakeProgramRepository(days: List<WorkoutDay> = emptyList()) : ProgramRepository {
    private val state = MutableStateFlow(days)
    val loadUpdates = mutableListOf<Triple<Long, Double?, Double?>>()
    var failWrites = false

    override fun workoutDays(): Flow<List<WorkoutDay>> = state

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?) {
        failIf(failWrites)
        loadUpdates += Triple(exerciseId, startLoadKg, incrementKg)
    }
}

class FakeSessionRepository : SessionRepository {
    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val loggedSets = MutableStateFlow<List<Pair<Long, LoggedSet>>>(emptyList())
    private var nextId = 1L

    val lastSetsByExercise = mutableMapOf<Long, List<LoggedSet>>()
    val topSetsByExercise = mutableMapOf<Long, List<TopSet>>()
    var failWrites = false

    val allSessions: List<WorkoutSession> get() = sessions.value

    override fun activeSession(): Flow<WorkoutSession?> =
        sessions.map { list -> list.filter { it.isActive }.maxByOrNull { it.startedAtMillis } }

    override fun session(id: Long): Flow<WorkoutSession?> = sessions.map { list -> list.firstOrNull { it.id == id } }

    override fun sessionsOn(date: LocalDate): Flow<List<WorkoutSession>> =
        sessions.map { list -> list.filter { it.date == date } }

    override fun sets(sessionId: Long): Flow<List<LoggedSet>> =
        loggedSets.map { list -> list.filter { it.first == sessionId }.map { it.second } }

    override suspend fun lastSets(exerciseId: Long, excludedSessionId: Long): List<LoggedSet> =
        lastSetsByExercise[exerciseId].orEmpty()

    override suspend fun recentTopSets(exerciseId: Long, limit: Int): List<TopSet> =
        topSetsByExercise[exerciseId].orEmpty().take(limit)

    override suspend fun startSession(dayId: Long, date: LocalDate, isDeload: Boolean, nowMillis: Long): Long {
        failIf(failWrites)
        val id = nextId++
        sessions.update { it + WorkoutSession(id, dayId, date, nowMillis, null, isDeload, null, null) }
        return id
    }

    override suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        slotPosition: Int,
        values: SetValues,
        nowMillis: Long,
    ): Long {
        failIf(failWrites)
        val id = nextId++
        loggedSets.update {
            it + (sessionId to LoggedSet(id, exerciseId, slotPosition, values.loadKg, values.reps, values.rir))
        }
        return id
    }

    override suspend fun updateSet(setId: Long, values: SetValues) {
        failIf(failWrites)
        loggedSets.update { list ->
            list.map { (sessionId, set) ->
                sessionId to if (set.id == setId) set.copy(loadKg = values.loadKg, reps = values.reps, rir = values.rir) else set
            }
        }
    }

    override suspend fun deleteSet(setId: Long) {
        failIf(failWrites)
        loggedSets.update { list -> list.filterNot { it.second.id == setId } }
    }

    override suspend fun setNote(sessionId: Long, note: String) {
        failIf(failWrites)
        updateSession(sessionId) { it.copy(note = note.trim().ifEmpty { null }) }
    }

    override suspend fun setRestEndsAt(sessionId: Long, restEndsAtMillis: Long?) {
        failIf(failWrites)
        updateSession(sessionId) { it.copy(restEndsAtMillis = restEndsAtMillis) }
    }

    override suspend fun finishSession(sessionId: Long, nowMillis: Long) {
        failIf(failWrites)
        updateSession(sessionId) { it.copy(finishedAtMillis = nowMillis, restEndsAtMillis = null) }
    }

    override suspend fun discardSession(sessionId: Long) {
        failIf(failWrites)
        loggedSets.update { list -> list.filterNot { it.first == sessionId } }
        sessions.update { list -> list.filterNot { it.id == sessionId } }
    }

    private fun updateSession(sessionId: Long, change: (WorkoutSession) -> WorkoutSession) {
        sessions.update { list -> list.map { if (it.id == sessionId) change(it) else it } }
    }
}

class FakeFoodRepository(
    foods: List<Food> = emptyList(),
    recipes: List<Recipe> = emptyList(),
) : FoodRepository {
    private val foodsState = MutableStateFlow(foods)
    private val recipesState = MutableStateFlow(recipes)
    private val entries = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    private var nextId = 1_000L
    var failWrites = false

    val allEntries: List<FoodLogEntry> get() = entries.value

    override fun foods(): Flow<List<Food>> = foodsState

    override fun recipes(): Flow<List<Recipe>> = recipesState

    override fun log(date: LocalDate): Flow<List<FoodLogEntry>> = entries.map { list -> list.filter { it.date == date } }

    override suspend fun food(id: Long): Food? = foodsState.value.firstOrNull { it.id == id }

    override suspend fun saveFood(draft: FoodDraft): Long {
        failIf(failWrites)
        val id = draft.id ?: nextId++
        val food = Food(id, draft.name, draft.per100g, draft.unitName, draft.unitGrams, isFavourite = false)
        foodsState.update { list -> list.filterNot { it.id == id } + food }
        return id
    }

    override suspend fun setFavourite(foodId: Long, favourite: Boolean) {
        failIf(failWrites)
        foodsState.update { list -> list.map { if (it.id == foodId) it.copy(isFavourite = favourite) else it } }
    }

    override suspend fun logFood(date: LocalDate, meal: Meal, food: Food, grams: Double, nowMillis: Long) {
        failIf(failWrites)
        add(FoodLogEntry(nextId++, date, meal, food.id, food.name, grams, food.nutritionFor(grams)))
    }

    override suspend fun logRecipe(date: LocalDate, meal: Meal, recipe: Recipe, nowMillis: Long) {
        failIf(failWrites)
        recipe.items.forEach { item ->
            add(FoodLogEntry(nextId++, date, meal, item.food.id, item.food.name, item.grams, item.food.nutritionFor(item.grams)))
        }
    }

    override suspend fun logEstimate(date: LocalDate, meal: Meal, name: String, nutrition: Nutrition, nowMillis: Long) {
        failIf(failWrites)
        add(FoodLogEntry(nextId++, date, meal, null, name, null, nutrition))
    }

    override suspend fun repeatMeal(from: LocalDate, to: LocalDate, meal: Meal, nowMillis: Long): Int {
        failIf(failWrites)
        val previous = entries.value.filter { it.date == from && it.meal == meal }
        previous.forEach { add(it.copy(id = nextId++, date = to)) }
        return previous.size
    }

    override suspend fun deleteEntry(entryId: Long) {
        failIf(failWrites)
        entries.update { list -> list.filterNot { it.id == entryId } }
    }

    private fun add(entry: FoodLogEntry) {
        entries.update { it + entry }
    }
}

class FakeBodyRepository : BodyRepository {
    private val state = MutableStateFlow<List<BodyWeight>>(emptyList())
    var failWrites = false

    override fun weights(from: LocalDate, to: LocalDate): Flow<List<BodyWeight>> =
        state.map { list -> list.filter { it.date in from..to }.sortedBy { it.date } }

    override suspend fun setWeight(date: LocalDate, weightKg: Double) {
        failIf(failWrites)
        state.update { list -> list.filterNot { it.date == date } + BodyWeight(date, weightKg) }
    }

    override suspend fun clearWeight(date: LocalDate) {
        failIf(failWrites)
        state.update { list -> list.filterNot { it.date == date } }
    }
}

class FakeRestTimer : RestTimer {
    var scheduled: Pair<Long, String>? = null
    var cancelCount = 0

    override fun schedule(endsAtMillis: Long, exerciseName: String) {
        scheduled = endsAtMillis to exerciseName
    }

    override fun cancel() {
        cancelCount++
        scheduled = null
    }
}

class FakeMealReminderScheduler : MealReminderScheduler {
    val calls = mutableListOf<Boolean>()

    override fun reschedule(enabled: Boolean) {
        calls += enabled
    }
}

class FakeClock(var now: Long) : WallClock {
    override fun nowMillis(): Long = now
}

class FakeDateProvider(date: LocalDate) : DateProvider {
    private val state = MutableStateFlow(date)

    var date: LocalDate
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun today(): LocalDate = state.value

    override fun todayFlow(): Flow<LocalDate> = state
}
