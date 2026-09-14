package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.ProgramRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun `seeded program matches the four training days`() = runTest {
        val repository = ProgramRepositoryImpl(
            ProgramLocalDataSourceImpl(db),
            TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
        )

        val days = repository.workoutDays().first()

        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), days.map { it.name })
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
            days.map { it.dayOfWeek },
        )
        assertEquals(listOf(18, 14, 18, 16), days.map { it.workingSets })
        assertEquals(
            listOf("Barbell bench press", "Back squat", "Pull-up", "Romanian deadlift"),
            days.map { day -> day.exercises.single { it.isTop }.name },
        )
    }

    @Test
    fun `seeded exercises carry their progression rules`() = runTest {
        val exercises = ProgramLocalDataSourceImpl(db).program().first().toWorkoutDays()
            .flatMap { it.exercises }
            .associateBy { it.name }

        assertEquals(40.0, exercises.getValue("Back squat").startLoadKg)
        assertEquals(5.0, exercises.getValue("Back squat").incrementKg, 0.0)
        assertEquals(Progression.REPS_FIRST, exercises.getValue("DB lateral raise").progression)
        assertEquals(Progression.BODYWEIGHT, exercises.getValue("Pull-up").progression)
        assertEquals(180, exercises.getValue("Barbell bench press").restSeconds)
    }

    @Test
    fun `shake one from seeded foods lands near the program's 520 kcal`() = runTest {
        val foods = db.foodQueries.selectAll().awaitAsList().associateBy { it.name }

        val kcal = listOf("Whole milk" to 250.0, "Oats" to 40.0, "Banana" to 120.0, "Whey protein" to 25.0)
            .sumOf { (name, grams) -> foods.getValue(name).kcal_per_100g * grams / 100 }

        assertEquals(520.0, kcal, 10.0)
    }
}
