package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.DataStore
import hr.rostanic20.gymbro.core.DispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface BackupSettingsLocalDataSource {
    fun profile(): Flow<UserProfile>
    suspend fun setFolder(folderUri: String?)
    suspend fun recordAutoBackup(nowMillis: Long)
}

class BackupSettingsLocalDataSourceImpl(
    private val ds: DataStore<UserProfile>,
    dispatchers: DispatcherProvider,
) : BackupSettingsLocalDataSource {

    private val writeContext = dispatchers.io + NonCancellable

    override fun profile(): Flow<UserProfile> = ds.data

    override suspend fun setFolder(folderUri: String?): Unit = withContext(writeContext) {
        ds.updateData { it.copy(backupFolderUri = folderUri) }
    }

    override suspend fun recordAutoBackup(nowMillis: Long): Unit = withContext(writeContext) {
        ds.updateData { it.copy(lastAutoBackupMillis = nowMillis) }
    }
}
