package hr.rostanic20.gymbro.data.backup

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val DATABASE_ENTRY = "gymbro.db"
private const val DATASTORE_DIRECTORY = "datastore"
private const val PHOTOS_DIRECTORY = "photos"
private val SAFE_FILE_NAME = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")
private val SQLITE_SIDE_FILES = listOf("-wal", "-shm", "-journal")

data class BackupLayout(val database: File, val dataStoreDir: File, val photosDir: File)

class InvalidBackupException(message: String) : IOException(message)

fun File.stagedBackupLayout(): BackupLayout =
    BackupLayout(File(this, DATABASE_ENTRY), File(this, DATASTORE_DIRECTORY), File(this, PHOTOS_DIRECTORY))

fun writeBackup(output: OutputStream, source: BackupLayout) {
    ZipOutputStream(output.buffered()).use { zip ->
        zip.putFile(DATABASE_ENTRY, source.database)
        source.dataStoreDir.backupFiles().forEach { zip.putFile("$DATASTORE_DIRECTORY/${it.name}", it) }
        source.photosDir.backupFiles().forEach { zip.putFile("$PHOTOS_DIRECTORY/${it.name}", it) }
    }
}

fun extractBackup(input: InputStream, target: BackupLayout) {
    var hasDatabase = false
    ZipInputStream(input.buffered()).use { zip ->
        generateSequence { zip.nextEntry }.forEach { entry ->
            val file = target.fileFor(entry.name) ?: throw InvalidBackupException("Unexpected entry ${entry.name}")
            file.parentFile?.mkdirs()
            file.outputStream().use { zip.copyTo(it) }
            if (file == target.database) hasDatabase = true
        }
    }
    if (!hasDatabase) throw InvalidBackupException("No database in backup")
}

fun applyStagedRestore(staged: BackupLayout, live: BackupLayout) {
    SQLITE_SIDE_FILES.forEach { File(live.database.path + it).delete() }
    staged.database.copyTo(live.database, overwrite = true)
    replaceDirectory(staged.dataStoreDir, live.dataStoreDir)
    replaceDirectory(staged.photosDir, live.photosDir)
}

private fun replaceDirectory(from: File, to: File) {
    to.deleteRecursively()
    if (from.isDirectory) from.copyRecursively(to, overwrite = true) else to.mkdirs()
}

private fun BackupLayout.fileFor(entryName: String): File? {
    if (entryName == DATABASE_ENTRY) return database
    val directory = when (entryName.substringBefore('/', missingDelimiterValue = "")) {
        DATASTORE_DIRECTORY -> dataStoreDir
        PHOTOS_DIRECTORY -> photosDir
        else -> return null
    }
    return entryName.substringAfter('/').takeIf { it.matches(SAFE_FILE_NAME) }?.let { File(directory, it) }
}

private fun File.backupFiles(): List<File> =
    listFiles().orEmpty().filter { it.isFile && it.name.matches(SAFE_FILE_NAME) }.sortedBy { it.name }

private fun ZipOutputStream.putFile(name: String, file: File) {
    putNextEntry(ZipEntry(name))
    file.inputStream().use { it.copyTo(this) }
    closeEntry()
}
