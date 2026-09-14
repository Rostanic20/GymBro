package hr.rostanic20.gymbro.ui.settings

import hr.rostanic20.gymbro.core.BackupScheduler
import hr.rostanic20.gymbro.core.BackupStore
import hr.rostanic20.gymbro.domain.model.BackupSettings
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private class FakeBackupStore : BackupStore {
        val exported = mutableListOf<String>()
        val staged = mutableListOf<String>()
        val kept = mutableListOf<String>()
        val released = mutableListOf<String>()
        var fail = false

        override suspend fun exportTo(destinationUri: String) {
            if (fail) throw IOException("disk full")
            exported += destinationUri
        }

        override suspend fun exportToFolder(folderUri: String, date: LocalDate) = Unit

        override suspend fun stageRestore(sourceUri: String) {
            if (fail) throw IOException("not a backup")
            staged += sourceUri
        }

        override fun keepFolderAccess(folderUri: String) {
            kept += folderUri
        }

        override fun releaseFolderAccess(folderUri: String) {
            released += folderUri
        }
    }

    private class FakeBackupSettingsRepository : BackupSettingsRepository {
        private val state = MutableStateFlow(BackupSettings(folderUri = null, lastAutoBackupMillis = null))
        val current: BackupSettings get() = state.value

        override fun settings(): Flow<BackupSettings> = state

        override suspend fun setFolder(folderUri: String?) {
            state.update { it.copy(folderUri = folderUri) }
        }

        override suspend fun recordAutoBackup(nowMillis: Long) {
            state.update { it.copy(lastAutoBackupMillis = nowMillis) }
        }
    }

    private class FakeBackupScheduler : BackupScheduler {
        val calls = mutableListOf<Boolean>()

        override fun schedule(enabled: Boolean) {
            calls += enabled
        }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = FakeBackupStore()
    private val settings = FakeBackupSettingsRepository()
    private val scheduler = FakeBackupScheduler()
    private val viewModel by lazy {
        BackupViewModel(store, settings, scheduler, FakeDateProvider(LocalDate.of(2026, 9, 14)))
    }

    private val documents = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FGymBro"
    private val downloads = "content://com.android.externalstorage.documents/tree/primary%3ADownload"

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `the export is named after today and confirmed when written`() = runTest {
        assertEquals("gymbro-2026-09-14.zip", viewModel.exportFileName())

        viewModel.export("content://downloads/1")

        assertEquals(UserMessage.BackupExported, viewModel.messages.first())
        assertEquals(listOf("content://downloads/1"), store.exported)
    }

    @Test
    fun `a failed export is reported and frees the buttons again`() = runTest {
        store.fail = true
        collectState()

        viewModel.export("content://downloads/1")

        assertEquals(UserMessage.BackupFailed, viewModel.messages.first())
        assertFalse(viewModel.state.value!!.isBusy)
    }

    @Test
    fun `a staged restore asks for a restart`() = runTest {
        collectState()

        viewModel.restore("content://downloads/2")
        advanceUntilIdle()

        assertEquals(listOf("content://downloads/2"), store.staged)
        assertTrue(viewModel.state.value!!.restoreStaged)
    }

    @Test
    fun `a file that is not a backup is reported without asking for a restart`() = runTest {
        store.fail = true
        collectState()

        viewModel.restore("content://downloads/3")

        assertEquals(UserMessage.RestoreFailed, viewModel.messages.first())
        assertFalse(viewModel.state.value!!.restoreStaged)
    }

    @Test
    fun `picking a new folder keeps its access, schedules the backup and lets go of the old one`() = runTest {
        collectState()

        viewModel.chooseFolder(documents)
        advanceUntilIdle()
        viewModel.chooseFolder(downloads)
        advanceUntilIdle()

        assertEquals(downloads, settings.current.folderUri)
        assertEquals(listOf(documents, downloads), store.kept)
        assertEquals(listOf(documents), store.released)
        assertEquals(listOf(true, true), scheduler.calls)
        assertEquals("Download", viewModel.state.value!!.folderLabel)
    }

    @Test
    fun `turning the weekly backup off cancels it and releases the folder`() = runTest {
        settings.setFolder(documents)

        viewModel.turnOffWeeklyBackup()
        advanceUntilIdle()

        assertNull(settings.current.folderUri)
        assertEquals(listOf(false), scheduler.calls)
        assertEquals(listOf(documents), store.released)
    }

    @Test
    fun `a folder label drops the storage volume`() {
        assertEquals("Documents/GymBro", folderLabel(documents))
    }
}
