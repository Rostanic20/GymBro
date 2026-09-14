package hr.rostanic20.gymbro.data.backup

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.dataStoreFile
import app.cash.sqldelight.db.SqlDriver
import hr.rostanic20.gymbro.core.BackupStore
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.PHOTO_DIRECTORY
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.di.DATABASE_NAME
import hr.rostanic20.gymbro.di.PROFILE_FILE
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate

private const val ZIP_MIME_TYPE = "application/zip"
private const val WEEKLY_PREFIX = "gymbro-weekly-"
private const val WEEKLY_BACKUPS_KEPT = 4
private const val STAGING_DIRECTORY = "restore-staging"
private const val PENDING_DIRECTORY = "restore-pending"
private const val FOLDER_ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

class AndroidBackupStore(
    private val context: Context,
    private val driver: SqlDriver,
    private val dispatchers: DispatcherProvider,
) : BackupStore {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun exportTo(destinationUri: String): Unit = withContext(dispatchers.io) {
        openOutput(destinationUri.toUri()).use { writeSnapshot(it) }
    }

    override suspend fun exportToFolder(folderUri: String, date: LocalDate): Unit = withContext(dispatchers.io) {
        val tree = folderUri.toUri()
        val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val document = DocumentsContract.createDocument(resolver, folder, ZIP_MIME_TYPE, "$WEEKLY_PREFIX$date.zip")
            ?: throw IOException("Cannot create a backup in $folderUri")
        try {
            openOutput(document).use { writeSnapshot(it) }
        } catch (e: Exception) {
            DocumentsContract.deleteDocument(resolver, document)
            throw e
        }
        pruneWeeklyBackups(tree)
    }

    override suspend fun stageRestore(sourceUri: String): Unit = withContext(dispatchers.io) {
        val staging = File(context.filesDir, STAGING_DIRECTORY).apply { deleteRecursively() }
        try {
            val layout = staging.stagedBackupLayout()
            val input = resolver.openInputStream(sourceUri.toUri()) ?: throw IOException("Cannot open $sourceUri")
            input.use { extractBackup(it, layout) }
            checkSchemaVersion(layout.database)
            val pending = File(context.filesDir, PENDING_DIRECTORY).apply { deleteRecursively() }
            if (!staging.renameTo(pending)) throw IOException("Cannot stage the restore")
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
    }

    override fun keepFolderAccess(folderUri: String) {
        resolver.takePersistableUriPermission(folderUri.toUri(), FOLDER_ACCESS)
    }

    override fun releaseFolderAccess(folderUri: String) {
        try {
            resolver.releasePersistableUriPermission(folderUri.toUri(), FOLDER_ACCESS)
        } catch (e: SecurityException) {
            Log.w("GymBro", "Folder access was already gone", e)
        }
    }

    private fun openOutput(uri: Uri): OutputStream =
        resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write $uri")

    private fun writeSnapshot(output: OutputStream) {
        val snapshot = File.createTempFile("backup", ".db", context.cacheDir)
        try {
            driver.execute(identifier = null, sql = "VACUUM INTO ?", parameters = 1) { bindString(0, snapshot.path) }
            writeBackup(output, context.liveBackupLayout().copy(database = snapshot))
        } finally {
            snapshot.delete()
        }
    }

    private fun checkSchemaVersion(database: File) {
        val version = SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        if (version.toLong() !in 1L..AppDb.Schema.version) {
            throw InvalidBackupException("Backup schema $version is not supported")
        }
    }

    private fun pruneWeeklyBackups(tree: Uri) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val weekly = resolver.query(children, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1).orEmpty()
                    if (name.startsWith(WEEKLY_PREFIX)) add(cursor.getString(0) to cursor.getLong(2))
                }
            }
        }.orEmpty()
        weekly.sortedByDescending { it.second }.drop(WEEKLY_BACKUPS_KEPT).forEach { (documentId, _) ->
            DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, documentId))
        }
    }

    companion object {

        fun applyPendingRestore(context: Context) {
            val pending = File(context.filesDir, PENDING_DIRECTORY)
            if (!pending.isDirectory) return
            try {
                applyStagedRestore(pending.stagedBackupLayout(), context.liveBackupLayout())
                pending.deleteRecursively()
            } catch (e: IOException) {
                Log.e("GymBro", "Applying the restore failed, retrying on next start", e)
            }
        }

        private fun Context.liveBackupLayout(): BackupLayout = BackupLayout(
            database = getDatabasePath(DATABASE_NAME),
            dataStoreDir = requireNotNull(dataStoreFile(PROFILE_FILE).parentFile),
            photosDir = File(filesDir, PHOTO_DIRECTORY),
        )
    }
}
