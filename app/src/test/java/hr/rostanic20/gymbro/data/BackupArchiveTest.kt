package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.data.backup.BackupLayout
import hr.rostanic20.gymbro.data.backup.InvalidBackupException
import hr.rostanic20.gymbro.data.backup.applyStagedRestore
import hr.rostanic20.gymbro.data.backup.extractBackup
import hr.rostanic20.gymbro.data.backup.stagedBackupLayout
import hr.rostanic20.gymbro.data.backup.writeBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun liveLayout(root: File) = BackupLayout(
        database = File(root, "databases/gymbro.db"),
        dataStoreDir = File(root, "files/datastore"),
        photosDir = File(root, "files/photos"),
    )

    private fun File.write(text: String) = apply {
        parentFile?.mkdirs()
        writeText(text)
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    @Test
    fun `a backup round-trips the database, settings and photos`() {
        val source = liveLayout(temp.newFolder("source"))
        source.database.write("db")
        File(source.dataStoreDir, "user_profile.json").write("{}")
        File(source.photosDir, "photo-1.jpg").write("front")
        File(source.photosDir, "photo-2.jpg").write("side")

        val archive = ByteArrayOutputStream().also { writeBackup(it, source) }.toByteArray()
        val staged = temp.newFolder("staged").stagedBackupLayout()
        extractBackup(ByteArrayInputStream(archive), staged)

        assertEquals("db", staged.database.readText())
        assertEquals("{}", File(staged.dataStoreDir, "user_profile.json").readText())
        assertEquals(listOf("photo-1.jpg", "photo-2.jpg"), staged.photosDir.list()!!.sorted())
    }

    @Test(expected = InvalidBackupException::class)
    fun `an entry that escapes its folder is refused`() {
        extractBackup(
            ByteArrayInputStream(zipOf("gymbro.db" to "db", "photos/../../evil.so" to "x")),
            temp.newFolder("staged").stagedBackupLayout(),
        )
    }

    @Test(expected = InvalidBackupException::class)
    fun `a zip without a database is refused`() {
        extractBackup(
            ByteArrayInputStream(zipOf("photos/photo-1.jpg" to "x")),
            temp.newFolder("staged").stagedBackupLayout(),
        )
    }

    @Test
    fun `applying a restore replaces everything and drops the stale write-ahead log`() {
        val live = liveLayout(temp.newFolder("live"))
        live.database.write("old db")
        val wal = File(live.database.path + "-wal").write("old wal")
        File(live.dataStoreDir, "user_profile.json").write("old")
        File(live.photosDir, "photo-old.jpg").write("old")
        val staged = temp.newFolder("staged").stagedBackupLayout()
        staged.database.write("new db")
        File(staged.dataStoreDir, "user_profile.json").write("new")
        File(staged.photosDir, "photo-new.jpg").write("new")

        applyStagedRestore(staged, live)

        assertEquals("new db", live.database.readText())
        assertFalse(wal.exists())
        assertEquals("new", File(live.dataStoreDir, "user_profile.json").readText())
        assertEquals(listOf("photo-new.jpg"), live.photosDir.list()!!.toList())
    }
}
