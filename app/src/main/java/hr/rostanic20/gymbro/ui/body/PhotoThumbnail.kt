// minSdk 34: the framework ExifInterface bugs this lint check guards against were fixed in API 25.
@file:Suppress("ExifInterface")

package hr.rostanic20.gymbro.ui.body

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import hr.rostanic20.gymbro.core.DispatcherProvider
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File

private const val THUMBNAIL_MAX_PX = 480
private const val DEGREES_90 = 90f
private const val DEGREES_180 = 180f
private const val DEGREES_270 = 270f

@Composable
fun PhotoThumbnail(file: File, contentDescription: String, modifier: Modifier = Modifier) {
    val dispatchers = koinInject<DispatcherProvider>()
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file) {
        value = withContext(dispatchers.io) { decodeThumbnail(file) }
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun decodeThumbnail(file: File): ImageBitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= THUMBNAIL_MAX_PX && bounds.outHeight / (sampleSize * 2) >= THUMBNAIL_MAX_PX) {
        sampleSize *= 2
    }
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        ?: return null
    val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> DEGREES_90
        ExifInterface.ORIENTATION_ROTATE_180 -> DEGREES_180
        ExifInterface.ORIENTATION_ROTATE_270 -> DEGREES_270
        else -> 0f
    }
    val upright = if (rotation == 0f) {
        bitmap
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
    }
    return upright.asImageBitmap()
}
