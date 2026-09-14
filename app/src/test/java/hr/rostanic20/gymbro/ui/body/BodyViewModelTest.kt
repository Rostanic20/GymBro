package hr.rostanic20.gymbro.ui.body

import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.core.PhotoTarget
import hr.rostanic20.gymbro.domain.WeeklyCheck
import hr.rostanic20.gymbro.domain.calorieChange
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeBodyRepository
import hr.rostanic20.gymbro.util.FakeClock
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakePhotoRepository
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.defaultProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModelTest {

    private class FakePhotoStorage : PhotoStorage {
        val deleted = mutableListOf<String>()
        val imported = mutableListOf<String>()
        private var counter = 0

        override fun newCaptureTarget(): PhotoTarget {
            counter++
            return PhotoTarget("capture-$counter.jpg", "content://photos/capture-$counter.jpg")
        }

        override suspend fun importFrom(sourceUri: String): String {
            imported += sourceUri
            counter++
            return "import-$counter.jpg"
        }

        override fun delete(fileName: String) {
            deleted += fileName
        }

        override fun fileFor(fileName: String): File = File(fileName)
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 19)
    private val profiles = FakeProfileRepository(defaultProfile.copy(programStart = today.minusWeeks(6)))
    private val body = FakeBodyRepository()
    private val photos = FakePhotoRepository()
    private val storage = FakePhotoStorage()
    private val dates = FakeDateProvider(today)
    private val viewModel by lazy { BodyViewModel(profiles, body, photos, storage, dates, FakeClock(5_000)) }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `two flat weeks offer more calories and applying them starts the re-check wait`() = runTest {
        (0L..20L).forEach { body.setWeight(today.minusDays(it), 70.0) }
        collectState()
        val check = viewModel.state.value!!.check
        assertTrue(check is WeeklyCheck.EatMore)

        viewModel.applyCalorieChange(check.calorieChange)
        advanceUntilIdle()

        assertEquals(150, profiles.current.kcalAdjustment)
        assertEquals(today, profiles.current.lastCalorieAdjustment)
        assertTrue(viewModel.state.value!!.check is WeeklyCheck.RecentlyAdjusted)
    }

    @Test
    fun `a waist measurement is saved for today and compared with four weeks ago`() = runTest {
        body.setWaist(today.minusDays(28), 76.0)
        collectState()

        viewModel.saveWaist(76.4)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(76.4, state.waistTodayCm!!, 0.001)
        assertEquals(today, state.latestWaist?.date)
        assertEquals(0.4, state.waistGainCm!!, 0.001)
    }

    @Test
    fun `recent weigh-ins are listed newest first`() = runTest {
        body.setWeight(today.minusDays(20), 69.0)
        body.setWeight(today.minusDays(1), 70.1)
        body.setWeight(today, 70.2)
        collectState()

        assertEquals(listOf(today, today.minusDays(1)), viewModel.state.value!!.recentWeights.map { it.date })
    }

    @Test
    fun `a captured photo is recorded and a cancelled capture is cleaned up`() = runTest {
        collectState()
        val kept = viewModel.newCaptureTarget()
        val cancelled = viewModel.newCaptureTarget()

        viewModel.onPhotoCaptured(PhotoPose.FRONT, kept.fileName, success = true)
        viewModel.onPhotoCaptured(PhotoPose.SIDE, cancelled.fileName, success = false)
        advanceUntilIdle()

        assertEquals(listOf(kept.fileName), photos.all.map { it.fileName })
        assertEquals(listOf(cancelled.fileName), storage.deleted)
        assertEquals(listOf(kept.fileName), viewModel.state.value!!.photosByPose[PhotoPose.FRONT]?.map { it.fileName })
    }

    @Test
    fun `an imported photo is copied and then recorded`() = runTest {
        collectState()

        viewModel.importPhoto(PhotoPose.BACK, "content://media/42")
        advanceUntilIdle()

        assertEquals(listOf("content://media/42"), storage.imported)
        assertEquals(PhotoPose.BACK, photos.all.single().pose)
    }

    @Test
    fun `when recording fails the copied file is removed and the failure reported`() = runTest {
        photos.failWrites = true
        collectState()

        viewModel.importPhoto(PhotoPose.FRONT, "content://media/7")

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
        assertEquals(listOf("import-1.jpg"), storage.deleted)
    }

    @Test
    fun `deleting a photo removes the row and the file`() = runTest {
        photos.addPhoto(today, PhotoPose.FRONT, "front.jpg", nowMillis = 1)
        collectState()

        viewModel.deletePhoto(photos.all.single())
        advanceUntilIdle()

        assertTrue(photos.all.isEmpty())
        assertEquals(listOf("front.jpg"), storage.deleted)
    }
}
