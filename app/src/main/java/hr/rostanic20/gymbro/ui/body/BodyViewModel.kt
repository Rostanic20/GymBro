package hr.rostanic20.gymbro.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.core.PhotoTarget
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.WAIST_WINDOW_DAYS
import hr.rostanic20.gymbro.domain.WEIGHT_CHECK_HISTORY_DAYS
import hr.rostanic20.gymbro.domain.WeeklyCheck
import hr.rostanic20.gymbro.domain.averageForWeekEnding
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.ProgressPhoto
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import hr.rostanic20.gymbro.domain.repository.BodyRepository
import hr.rostanic20.gymbro.domain.repository.PhotoRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.waistGain
import hr.rostanic20.gymbro.domain.weekOn
import hr.rostanic20.gymbro.domain.weeklyChange
import hr.rostanic20.gymbro.domain.weeklyCheck
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.time.LocalDate

data class BodyUiState(
    val today: LocalDate,
    val check: WeeklyCheck,
    val kcalAdjustment: Int,
    val recentWeights: List<BodyWeight>,
    val weekAverageKg: Double?,
    val weeklyChangeKg: Double?,
    val waistTodayCm: Double?,
    val latestWaist: WaistMeasurement?,
    val waistGainCm: Double?,
    val photosByPose: Map<PhotoPose, List<ProgressPhoto>>,
)

private const val RECENT_WEIGHT_DAYS = 14L
private const val WAIST_HISTORY_DAYS = WAIST_WINDOW_DAYS * 2 + 14

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModel(
    private val profileRepository: ProfileRepository,
    private val bodyRepository: BodyRepository,
    private val photoRepository: PhotoRepository,
    private val photoStorage: PhotoStorage,
    private val dates: DateProvider,
    private val clock: WallClock,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<BodyUiState?> =
        combine(
            profileRepository.profile(),
            dates.todayFlow(),
            dates.todayFlow().flatMapLatest { bodyRepository.weights(it.minusDays(WEIGHT_CHECK_HISTORY_DAYS), it) },
            dates.todayFlow().flatMapLatest { bodyRepository.waists(it.minusDays(WAIST_HISTORY_DAYS), it) },
            photoRepository.photos(),
        ) { profile, today, weights, waists, photos ->
            BodyUiState(
                today = today,
                check = weeklyCheck(profile.weekOn(today), today, weights, waists, profile.lastCalorieAdjustment),
                kcalAdjustment = profile.kcalAdjustment,
                recentWeights = weights.filter { it.date > today.minusDays(RECENT_WEIGHT_DAYS) }
                    .sortedByDescending { it.date },
                weekAverageKg = weights.averageForWeekEnding(today),
                weeklyChangeKg = weights.weeklyChange(today),
                waistTodayCm = waists.firstOrNull { it.date == today }?.waistCm,
                latestWaist = waists.maxByOrNull { it.date },
                waistGainCm = waistGain(waists, today),
                photosByPose = photos.groupBy { it.pose }.mapValues { (_, list) -> list.sortedByDescending { it.date } },
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun applyCalorieChange(deltaKcal: Int) {
        launchReporting(_messages) { profileRepository.adjustCalories(deltaKcal, dates.today()) }
    }

    fun saveWaist(waistCm: Double) {
        launchReporting(_messages) { bodyRepository.setWaist(dates.today(), waistCm) }
    }

    fun newCaptureTarget(): PhotoTarget = photoStorage.newCaptureTarget()

    fun photoFile(fileName: String): File = photoStorage.fileFor(fileName)

    fun onPhotoCaptured(pose: PhotoPose, fileName: String, success: Boolean) {
        launchReporting(_messages) {
            if (success) recordPhoto(pose, fileName) else photoStorage.delete(fileName)
        }
    }

    fun importPhoto(pose: PhotoPose, sourceUri: String) {
        launchReporting(_messages) { recordPhoto(pose, photoStorage.importFrom(sourceUri)) }
    }

    fun deletePhoto(photo: ProgressPhoto) {
        launchReporting(_messages) {
            photoRepository.deletePhoto(photo.id)
            photoStorage.delete(photo.fileName)
        }
    }

    private suspend fun recordPhoto(pose: PhotoPose, fileName: String) {
        try {
            photoRepository.addPhoto(dates.today(), pose, fileName, clock.nowMillis())
        } catch (e: Exception) {
            photoStorage.delete(fileName)
            throw e
        }
    }
}
