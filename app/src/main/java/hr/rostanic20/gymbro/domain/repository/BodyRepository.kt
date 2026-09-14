package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.ProgressPhoto
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BodyRepository {
    fun weights(from: LocalDate, to: LocalDate): Flow<List<BodyWeight>>
    fun waists(from: LocalDate, to: LocalDate): Flow<List<WaistMeasurement>>
    suspend fun setWeight(date: LocalDate, weightKg: Double)
    suspend fun clearWeight(date: LocalDate)
    suspend fun setWaist(date: LocalDate, waistCm: Double)
}

interface PhotoRepository {
    fun photos(): Flow<List<ProgressPhoto>>
    suspend fun addPhoto(date: LocalDate, pose: PhotoPose, fileName: String, nowMillis: Long)
    suspend fun deletePhoto(photoId: Long)
}
