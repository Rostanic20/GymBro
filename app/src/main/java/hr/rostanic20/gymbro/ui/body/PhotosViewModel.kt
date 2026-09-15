package hr.rostanic20.gymbro.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.ProgressPhoto
import hr.rostanic20.gymbro.domain.repository.PhotoRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File

class PhotosViewModel(
    private val photoRepository: PhotoRepository,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<Map<PhotoPose, List<ProgressPhoto>>?> =
        photoRepository.photos()
            .map { photos -> photos.groupBy { it.pose }.mapValues { (_, list) -> list.sortedBy { it.date } } }
            .stateInWhileSubscribed(viewModelScope, null)

    fun photoFile(fileName: String): File = photoStorage.fileFor(fileName)

    fun deletePhoto(photo: ProgressPhoto) {
        launchReporting(_messages) {
            photoRepository.deletePhoto(photo.id)
            photoStorage.delete(photo.fileName)
        }
    }
}
