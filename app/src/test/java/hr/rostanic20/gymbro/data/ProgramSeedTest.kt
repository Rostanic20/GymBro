package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.ProgramRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ProgramSeedTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        AppDb.Schema.synchronous().create(it)
    }
    private val db = AppDb(driver)

    @After
    fun tearDown() {
        driver.close()
    }

    private fun TestScope.repository(): ProgramRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return ProgramRepositoryImpl(ProgramLocalDataSourceImpl(db, dispatchers), dispatchers)
    }

    private suspend fun ProgramRepositoryImpl.exercisesByName(): Map<String, PlannedExercise> =
        workoutDays().first().flatMap { it.exercises }.associateBy { it.exercise.name }

    @Test
    fun `seeded program matches the four training days`() = runTest {
        val days = repository().workoutDays().first()

        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), days.map { it.name })
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
            days.map { it.dayOfWeek },
        )
        assertEquals(listOf(18, 14, 18, 16), days.map { it.workingSets })
        assertEquals(
            listOf("Barbell bench press", "Back squat", "Pull-up", "Romanian deadlift"),
            days.map { day -> day.exercises.single { it.isTop }.exercise.name },
        )
    }

    @Test
    fun `exercises carry load type, progression and rest range`() = runTest {
        val exercises = repository().exercisesByName()

        val squat = exercises.getValue("Back squat").exercise
        assertEquals(LoadType.WEIGHT, squat.loadType)
        assertEquals(40.0, squat.startLoadKg)
        assertEquals(5.0, squat.incrementKg)
        assertEquals(20.0, squat.barWeightKg)
        assertEquals(LoadType.ASSISTANCE, exercises.getValue("Pull-up").exercise.loadType)
        assertEquals(Progression.REPS_FIRST, exercises.getValue("DB lateral raise").exercise.progression)
        assertEquals(LoadType.BODYWEIGHT, exercises.getValue("Hanging leg raise").exercise.loadType)
        assertNull(exercises.getValue("Hanging leg raise").exercise.incrementKg)
        assertEquals(180..180, exercises.getValue("Barbell bench press").restSeconds)
        assertEquals(120..180, exercises.getValue("Leg press").restSeconds)
    }

    @Test
    fun `either-or slots are separate exercises linked as alternatives`() = runTest {
        val exercises = repository().exercisesByName()

        assertTrue(exercises.keys.none { " or " in it })
        assertEquals(listOf("Bulgarian split squat"), exercises.getValue("Hack squat").alternatives.map { it.exercise.name })
        assertEquals(listOf("Cable fly", "Dumbbell fly"), exercises.getValue("Pec deck").alternatives.map { it.exercise.name })
        assertEquals(
            listOf("Band-assisted pull-up", "Lat pulldown, wide grip"),
            exercises.getValue("Pull-up").alternatives.map { it.exercise.name },
        )
    }

    @Test
    fun `load settings update is saved and emitted`() = runTest {
        val repository = repository()
        val legPress = repository.exercisesByName().getValue("Leg press").exercise

        repository.updateLoadSettings(legPress.id, startLoadKg = 80.0, incrementKg = 10.0)

        val updated = repository.exercisesByName().getValue("Leg press").exercise
        assertEquals(80.0, updated.startLoadKg)
        assertEquals(10.0, updated.incrementKg)
    }

    @Test
    fun `shake one from seeded foods lands near the program's 520 kcal`() = runTest {
        val foods = db.foodQueries.selectAll().awaitAsList().associateBy { it.name }

        val kcal = listOf("Whole milk" to 250.0, "Oats" to 40.0, "Banana" to 120.0, "Whey protein" to 25.0)
            .sumOf { (name, grams) -> foods.getValue(name).kcal_per_100g * grams / 100 }

        assertEquals(520.0, kcal, 10.0)
    }
}
