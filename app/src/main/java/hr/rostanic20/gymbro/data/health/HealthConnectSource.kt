package hr.rostanic20.gymbro.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.core.HealthSource
import hr.rostanic20.gymbro.core.HealthSummary
import hr.rostanic20.gymbro.domain.TimeWindow
import hr.rostanic20.gymbro.domain.sleepWindow
import hr.rostanic20.gymbro.domain.stepsWindow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime

class HealthConnectSource(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : HealthSource {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    override fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override suspend fun hasPermissions(): Boolean = withContext(dispatchers.io) {
        client.permissionController.getGrantedPermissions().containsAll(readPermissions)
    }

    override suspend fun summaryFor(date: LocalDate, now: ZonedDateTime): HealthSummary = withContext(dispatchers.io) {
        val steps = client.aggregate(
            AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), stepsWindow(date, now).toFilter()),
        )
        val sleep = client.aggregate(
            AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), sleepWindow(date, now.zone).toFilter()),
        )
        HealthSummary(
            steps = steps[StepsRecord.COUNT_TOTAL],
            sleep = sleep[SleepSessionRecord.SLEEP_DURATION_TOTAL],
        )
    }

    private fun TimeWindow.toFilter(): TimeRangeFilter = TimeRangeFilter.between(start.toInstant(), end.toInstant())
}
