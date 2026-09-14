package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.BackupSettings
import kotlinx.coroutines.flow.Flow

interface BackupSettingsRepository {
    fun settings(): Flow<BackupSettings>
    suspend fun setFolder(folderUri: String?)
    suspend fun recordAutoBackup(nowMillis: Long)
}
