package hr.rostanic20.gymbro.data.local

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.core.PhotoTarget
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val PHOTO_DIRECTORY = "photos"

class AndroidPhotoStorage(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : PhotoStorage {

    private val directory: File
        get() = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }

    override fun newCaptureTarget(): PhotoTarget {
        val file = newFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
        return PhotoTarget(fileName = file.name, uri = uri.toString())
    }

    override suspend fun importFrom(sourceUri: String): String = withContext(dispatchers.io + NonCancellable) {
        val file = newFile()
        val input = requireNotNull(context.contentResolver.openInputStream(sourceUri.toUri())) {
            "Cannot open $sourceUri"
        }
        input.use { source -> file.outputStream().use { source.copyTo(it) } }
        file.name
    }

    override fun delete(fileName: String) {
        fileFor(fileName).delete()
    }

    override fun fileFor(fileName: String): File = File(directory, fileName)

    private fun newFile(): File = File(directory, "photo-${UUID.randomUUID()}.jpg")
}
