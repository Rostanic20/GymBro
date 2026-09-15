package hr.rostanic20.gymbro.ui.body

import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.core.PhotoTarget
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.util.FakePhotoRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PhotosViewModelTest {

    private class RecordingStorage : PhotoStorage {
        val deleted = mutableListOf<String>()

        override fun newCaptureTarget(): PhotoTarget = PhotoTarget("x.jpg", "content://x")

        override suspend fun importFrom(sourceUri: String): String = "x.jpg"

        override fun delete(fileName: String) {
            deleted += fileName
        }

        override fun fileFor(fileName: String): File = File(fileName)
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val photos = FakePhotoRepository()
    private val storage = RecordingStorage()
    private val viewModel by lazy { PhotosViewModel(photos, storage) }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `photos are grouped by pose, oldest first`() = runTest {
        photos.addPhoto(monday.plusWeeks(4), PhotoPose.FRONT, "front-later.jpg", nowMillis = 2)
        photos.addPhoto(monday, PhotoPose.FRONT, "front-first.jpg", nowMillis = 1)
        photos.addPhoto(monday, PhotoPose.BACK, "back.jpg", nowMillis = 3)
        collectState()

        val byPose = viewModel.state.value!!
        assertEquals(
            listOf("front-first.jpg", "front-later.jpg"),
            byPose.getValue(PhotoPose.FRONT).map { it.fileName },
        )
        assertEquals(listOf("back.jpg"), byPose.getValue(PhotoPose.BACK).map { it.fileName })
        assertNull(byPose[PhotoPose.SIDE])
    }

    @Test
    fun `deleting a photo removes the row and the file`() = runTest {
        photos.addPhoto(monday, PhotoPose.FRONT, "front.jpg", nowMillis = 1)
        collectState()

        viewModel.deletePhoto(photos.all.single())
        advanceUntilIdle()

        assertEquals(emptyMap<PhotoPose, Any>(), viewModel.state.value)
        assertEquals(listOf("front.jpg"), storage.deleted)
    }
}
