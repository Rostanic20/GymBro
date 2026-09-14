package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.BodyLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.PhotoLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.BodyRepositoryImpl
import hr.rostanic20.gymbro.data.repository.PhotoRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun TestScope.photos(): PhotoRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return PhotoRepositoryImpl(PhotoLocalDataSourceImpl(db, dispatchers), dispatchers)
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

    @Test
    fun `waist measurements replace the same day and come back in order`() = runTest {
        val repository = repository()
        repository.setWaist(monday.plusDays(7), 76.5)
        repository.setWaist(monday, 76.0)
        repository.setWaist(monday, 75.8)

        assertEquals(
            listOf(WaistMeasurement(monday, 75.8), WaistMeasurement(monday.plusDays(7), 76.5)),
            repository.waists(monday, monday.plusDays(7)).first(),
        )
    }

    @Test
    fun `photos come back newest first and can be deleted`() = runTest {
        val photos = photos()
        photos.addPhoto(monday, PhotoPose.FRONT, "front-1.jpg", nowMillis = 1_000)
        photos.addPhoto(monday.plusDays(28), PhotoPose.SIDE, "side-2.jpg", nowMillis = 2_000)

        val stored = photos.photos().first()
        assertEquals(listOf("side-2.jpg", "front-1.jpg"), stored.map { it.fileName })
        assertEquals(PhotoPose.SIDE, stored.first().pose)

        photos.deletePhoto(stored.first().id)

        assertEquals(listOf("front-1.jpg"), photos.photos().first().map { it.fileName })
        assertTrue(photos.photos().first().all { it.date == monday })
    }
}
