package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.BodyLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.BodyRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BodyRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        AppDb.Schema.synchronous().create(it)
    }
    private val db = AppDb(driver)
    private val monday = LocalDate.of(2026, 9, 14)

    @After
    fun tearDown() {
        driver.close()
    }

    private fun TestScope.repository(): BodyRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return BodyRepositoryImpl(BodyLocalDataSourceImpl(db, dispatchers), dispatchers)
    }

    @Test
    fun `a second weigh-in on the same day replaces the first`() = runTest {
        val repository = repository()

        repository.setWeight(monday, 70.4)
        repository.setWeight(monday, 70.1)

        assertEquals(listOf(BodyWeight(monday, 70.1)), repository.weights(monday, monday).first())
    }

    @Test
    fun `weights come back for the range in date order`() = runTest {
        val repository = repository()
        repository.setWeight(monday.plusDays(2), 70.3)
        repository.setWeight(monday, 70.0)
        repository.setWeight(monday.plusDays(9), 71.0)

        assertEquals(
            listOf(BodyWeight(monday, 70.0), BodyWeight(monday.plusDays(2), 70.3)),
            repository.weights(monday, monday.plusDays(6)).first(),
        )
    }

    @Test
    fun `clearing removes only that day`() = runTest {
        val repository = repository()
        repository.setWeight(monday, 70.0)
        repository.setWeight(monday.plusDays(1), 70.2)

        repository.clearWeight(monday)

        assertEquals(listOf(BodyWeight(monday.plusDays(1), 70.2)), repository.weights(monday, monday.plusDays(1)).first())
    }
}
