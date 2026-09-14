package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.data.local.BackupSettingsLocalDataSource
import hr.rostanic20.gymbro.domain.model.BackupSettings
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BackupSettingsRepositoryImpl(
    private val local: BackupSettingsLocalDataSource,
) : BackupSettingsRepository {

    override fun settings(): Flow<BackupSettings> =
        local.profile().map { BackupSettings(it.backupFolderUri, it.lastAutoBackupMillis) }.distinctUntilChanged()

    override suspend fun setFolder(folderUri: String?) {
        local.setFolder(folderUri)
    }

    override suspend fun recordAutoBackup(nowMillis: Long) {
        local.recordAutoBackup(nowMillis)
    }
}
