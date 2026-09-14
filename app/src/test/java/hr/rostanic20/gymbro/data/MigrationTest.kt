package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.SessionLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.SessionRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class MigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a version 1 database gains session tables and keeps its program`() = runTest {
        val file = folder.newFile("gymbro-v1.db")
        File("src/main/sqldelight/databases/1.db").copyTo(file, overwrite = true)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        val db = AppDb(driver)
        db.programQueries.updateLoadSettings(startLoadKg = 45.0, incrementKg = 2.5, id = 1)
        val exercisesBefore = db.programQueries.selectProgram().awaitAsList().size

        AppDb.Schema.synchronous().migrate(driver, 1, AppDb.Schema.version)

        assertEquals(exercisesBefore, db.programQueries.selectProgram().awaitAsList().size)
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val sessions = SessionRepositoryImpl(SessionLocalDataSourceImpl(db, dispatchers), dispatchers)
        val id = sessions.startSession(1, LocalDate.of(2026, 9, 14), isDeload = false, nowMillis = 1_000)
        sessions.logSet(id, 1, 1, SetValues(45.0, 8, 2), nowMillis = 1_100)
        assertEquals(listOf(8), sessions.sets(id).first().map { it.reps })
        driver.close()
    }
}
