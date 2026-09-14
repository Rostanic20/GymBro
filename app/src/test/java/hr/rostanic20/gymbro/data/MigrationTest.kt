package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.BodyLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.FoodLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.PhotoLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.SessionLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.BodyRepositoryImpl
import hr.rostanic20.gymbro.data.repository.FoodRepositoryImpl
import hr.rostanic20.gymbro.data.repository.PhotoRepositoryImpl
import hr.rostanic20.gymbro.data.repository.SessionRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import hr.rostanic20.gymbro.domain.nutrition
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class MigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val monday = LocalDate.of(2026, 9, 14)

    private fun openBaseline(version: Int): Pair<JdbcSqliteDriver, AppDb> {
        val file = folder.newFile("gymbro-v$version.db")
        File("src/main/sqldelight/databases/$version.db").copyTo(file, overwrite = true)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        return driver to AppDb(driver)
    }

    private fun TestScope.dispatchers() = TestDispatcherProvider(StandardTestDispatcher(testScheduler))

    @Test
    fun `a version 1 database gains session tables and keeps its program`() = runTest {
        val (driver, db) = openBaseline(1)
        db.programQueries.updateLoadSettings(startLoadKg = 45.0, incrementKg = 2.5, id = 1)
        val exercisesBefore = db.programQueries.selectProgram().awaitAsList().size

        AppDb.Schema.synchronous().migrate(driver, 1, AppDb.Schema.version)

        assertEquals(exercisesBefore, db.programQueries.selectProgram().awaitAsList().size)
        val sessions = SessionRepositoryImpl(SessionLocalDataSourceImpl(db, dispatchers()), dispatchers())
        val id = sessions.startSession(1, monday, isDeload = false, nowMillis = 1_000)
        sessions.logSet(id, 1, 1, SetValues(45.0, 8, 2), nowMillis = 1_100)
        assertEquals(listOf(8), sessions.sets(id).first().map { it.reps })
        driver.close()
    }

    @Test
    fun `a version 2 database gains food logging and keeps its sessions`() = runTest {
        val (driver, db) = openBaseline(2)
        val sessions = SessionRepositoryImpl(SessionLocalDataSourceImpl(db, dispatchers()), dispatchers())
        val sessionId = sessions.startSession(1, monday, isDeload = false, nowMillis = 1_000)

        AppDb.Schema.synchronous().migrate(driver, 2, AppDb.Schema.version)

        val foods = FoodRepositoryImpl(FoodLocalDataSourceImpl(db, dispatchers()), dispatchers())
        val shake = foods.recipes().first().single { it.name == "Shake #1" }
        assertEquals(520.0, shake.nutrition.kcal, 10.0)
        assertTrue(foods.foods().first().none { it.isFavourite })
        foods.logRecipe(monday, Meal.BREAKFAST_SHAKE, shake, nowMillis = 2_000)
        assertEquals(4, foods.log(monday).first().size)
        assertEquals(sessionId, sessions.session(sessionId).first()?.id)
        driver.close()
    }

    @Test
    fun `a version 3 database gains waist and photos and keeps its weigh-ins`() = runTest {
        val (driver, db) = openBaseline(3)
        val body = BodyRepositoryImpl(BodyLocalDataSourceImpl(db, dispatchers()), dispatchers())
        body.setWeight(monday, 70.2)

        AppDb.Schema.synchronous().migrate(driver, 3, AppDb.Schema.version)

        assertEquals(listOf(BodyWeight(monday, 70.2)), body.weights(monday, monday).first())
        body.setWaist(monday, 76.0)
        assertEquals(listOf(WaistMeasurement(monday, 76.0)), body.waists(monday, monday).first())
        val photos = PhotoRepositoryImpl(PhotoLocalDataSourceImpl(db, dispatchers()), dispatchers())
        photos.addPhoto(monday, PhotoPose.FRONT, "front.jpg", nowMillis = 1_000)
        assertEquals(listOf("front.jpg"), photos.photos().first().map { it.fileName })
        driver.close()
    }
}
