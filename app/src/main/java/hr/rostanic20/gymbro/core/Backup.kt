package hr.rostanic20.gymbro.core

import java.time.LocalDate

interface BackupStore {
    suspend fun exportTo(destinationUri: String)
    suspend fun exportToFolder(folderUri: String, date: LocalDate)
    suspend fun stageRestore(sourceUri: String)
    fun keepFolderAccess(folderUri: String)
    fun releaseFolderAccess(folderUri: String)
}

interface BackupScheduler {
    fun schedule(enabled: Boolean)
}
