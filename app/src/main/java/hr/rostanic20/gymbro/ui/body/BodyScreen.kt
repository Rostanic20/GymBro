package hr.rostanic20.gymbro.ui.body

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.core.PhotoTarget
import hr.rostanic20.gymbro.domain.CHECK_FROM_WEEK
import hr.rostanic20.gymbro.domain.WeeklyCheck
import hr.rostanic20.gymbro.domain.calorieChange
import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.ProgressPhoto
import hr.rostanic20.gymbro.domain.rollingWeeklyAverage
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.ChartPoint
import hr.rostanic20.gymbro.ui.common.ChartSeries
import hr.rostanic20.gymbro.ui.common.ChartStyle
import hr.rostanic20.gymbro.ui.common.LineChart
import hr.rostanic20.gymbro.ui.common.MIN_CHART_POINTS
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.parseKg
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.common.rememberShortDayFormatter
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
import java.util.Locale
import kotlin.math.abs

private const val MIN_WAIST_CM = 40.0
private const val MAX_WAIST_CM = 200.0

data class BodyActions(
    val applyCalorieChange: (Int) -> Unit,
    val saveWaist: (Double) -> Unit,
    val newCaptureTarget: () -> PhotoTarget,
    val photoFile: (String) -> File,
    val photoCaptured: (PhotoPose, String, Boolean) -> Unit,
    val importPhoto: (PhotoPose, String) -> Unit,
    val deletePhoto: (ProgressPhoto) -> Unit,
)

@Composable
fun BodyScreen(viewModel: BodyViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    val actions = remember(viewModel) {
        BodyActions(
            applyCalorieChange = viewModel::applyCalorieChange,
            saveWaist = viewModel::saveWaist,
            newCaptureTarget = viewModel::newCaptureTarget,
            photoFile = viewModel::photoFile,
            photoCaptured = viewModel::onPhotoCaptured,
            importPhoto = viewModel::importPhoto,
            deletePhoto = viewModel::deletePhoto,
        )
    }
    state?.let { BodyContent(state = it, actions = actions) }
}

@Composable
private fun BodyContent(state: BodyUiState, actions: BodyActions) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s16),
    ) {
        item { Text(text = stringResource(R.string.body_title), style = MaterialTheme.typography.headlineMedium) }
        item { WeeklyCheckCard(check = state.check, adjustmentKcal = state.kcalAdjustment, onApply = actions.applyCalorieChange) }
        item { WeightHistoryCard(state = state) }
        item { WaistCard(state = state, onSave = actions.saveWaist) }
        item { PhotosCard(photosByPose = state.photosByPose, actions = actions) }
    }
}

@Composable
private fun WeeklyCheckCard(check: WeeklyCheck, adjustmentKcal: Int, onApply: (Int) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val change = check.calorieChange
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (change != 0) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(text = stringResource(R.string.check_title), style = MaterialTheme.typography.titleMedium)
            Text(text = checkMessage(check), style = MaterialTheme.typography.bodyMedium)
            if (adjustmentKcal != 0) {
                Text(
                    text = stringResource(R.string.check_current_adjustment, String.format(locale, "%+d", adjustmentKcal)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (change != 0) {
                Button(onClick = { onApply(change) }) {
                    Text(
                        stringResource(
                            if (change > 0) R.string.check_apply_more else R.string.check_apply_less,
                            formatCount(abs(change), locale),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun checkMessage(check: WeeklyCheck): String {
    val locale = LocalLocale.current.platformLocale
    val formatter = rememberDayFormatter()
    return when (check) {
        WeeklyCheck.TooEarly -> stringResource(R.string.check_too_early, CHECK_FROM_WEEK)
        is WeeklyCheck.RecentlyAdjusted -> stringResource(R.string.check_recently_adjusted, check.recheckOn.format(formatter))
        WeeklyCheck.NotEnoughData -> stringResource(R.string.check_not_enough)
        is WeeklyCheck.OnTrack -> stringResource(R.string.check_on_track, signed(check.weeklyChangeKg, locale))
        is WeeklyCheck.EatMore -> stringResource(R.string.check_eat_more, signed(check.weeklyChangeKg, locale))
        is WeeklyCheck.EatLessFastGain -> stringResource(R.string.check_eat_less_fast, signed(check.weeklyChangeKg, locale))
        is WeeklyCheck.EatLessWaist -> stringResource(R.string.check_eat_less_waist, signed(check.waistGainCm, locale))
    }
}

private fun signed(value: Double, locale: Locale): String = String.format(locale, "%+.1f", value)

@Composable
private fun WeightHistoryCard(state: BodyUiState) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val formatter = rememberShortDayFormatter()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            Text(text = stringResource(R.string.weight_history_title), style = MaterialTheme.typography.titleMedium)
            state.weekAverageKg?.let {
                Text(
                    text = stringResource(R.string.weight_week_average, String.format(locale, "%.1f", it)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.weeklyChangeKg?.let {
                Text(
                    text = stringResource(R.string.weight_weekly_change, signed(it, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.recentWeights.isEmpty()) {
                Text(
                    text = stringResource(R.string.weight_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.weightTrend.size >= MIN_CHART_POINTS) {
                WeightTrendChart(weights = state.weightTrend)
            }
            state.recentWeights.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entry.date.format(formatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.weight_entry, formatKg(entry.weightKg, locale)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private const val WEIGHT_CHART_ASPECT_RATIO = 2f

@Composable
private fun WeightTrendChart(weights: List<BodyWeight>) {
    val spacing = LocalSpacing.current
    val dotColor = MaterialTheme.colorScheme.outline
    val lineColor = MaterialTheme.colorScheme.primary
    val series = remember(weights, dotColor, lineColor) {
        listOf(
            ChartSeries(weights.map { it.toChartPoint() }, dotColor, ChartStyle.DOTS),
            ChartSeries(weights.rollingWeeklyAverage().map { it.toChartPoint() }, lineColor, ChartStyle.LINE),
        )
    }
    Column(
        modifier = Modifier.padding(top = spacing.s8),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        LineChart(
            series = series,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(WEIGHT_CHART_ASPECT_RATIO),
        )
        Text(
            text = stringResource(R.string.weight_trend_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun BodyWeight.toChartPoint() = ChartPoint(date.toEpochDay().toFloat(), weightKg.toFloat())

@Composable
private fun WaistCard(state: BodyUiState, onSave: (Double) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val formatter = rememberShortDayFormatter()
    var text by rememberSaveable(state.waistTodayCm) {
        mutableStateOf(state.waistTodayCm?.let { formatKg(it, locale) }.orEmpty())
    }
    val parsed = parseKg(text)?.takeIf { it in MIN_WAIST_CM..MAX_WAIST_CM }
    val focusManager = LocalFocusManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(text = stringResource(R.string.waist_title), style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.waist_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        parsed?.let(onSave)
                        focusManager.clearFocus()
                    },
                    enabled = parsed != null && parsed != state.waistTodayCm,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
            state.latestWaist?.let {
                Text(
                    text = stringResource(R.string.waist_last, formatKg(it.waistCm, locale), it.date.format(formatter)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.waistGainCm?.let {
                Text(
                    text = stringResource(R.string.waist_gain, signed(it, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.waist_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhotosCard(photosByPose: Map<PhotoPose, List<ProgressPhoto>>, actions: BodyActions) {
    val spacing = LocalSpacing.current
    var capturePose by rememberSaveable { mutableStateOf<PhotoPose?>(null) }
    var captureFile by rememberSaveable { mutableStateOf<String?>(null) }
    var importPose by rememberSaveable { mutableStateOf<PhotoPose?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val pose = capturePose
        val file = captureFile
        if (pose != null && file != null) actions.photoCaptured(pose, file, success)
        capturePose = null
        captureFile = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pose = importPose
        if (pose != null && uri != null) actions.importPhoto(pose, uri.toString())
        importPose = null
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            Text(text = stringResource(R.string.photos_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.photos_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PhotoPose.entries.forEach { pose ->
                val photos = photosByPose[pose].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s8)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(pose.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                val target = actions.newCaptureTarget()
                                capturePose = pose
                                captureFile = target.fileName
                                camera.launch(target.uri.toUri())
                            },
                        ) {
                            Text(stringResource(R.string.photo_take))
                        }
                        TextButton(
                            onClick = {
                                importPose = pose
                                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Text(stringResource(R.string.photo_pick))
                        }
                    }
                    if (photos.isEmpty()) {
                        Text(
                            text = stringResource(R.string.photo_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s12)) {
                            val latest = photos.first()
                            val first = photos.last()
                            if (first.id != latest.id) {
                                PhotoTile(photo = first, label = R.string.photo_first, file = actions.photoFile(first.fileName)) {
                                    deletingId = first.id
                                }
                            }
                            PhotoTile(photo = latest, label = R.string.photo_latest, file = actions.photoFile(latest.fileName)) {
                                deletingId = latest.id
                            }
                        }
                    }
                }
            }
        }
    }
    photosByPose.values.flatten().firstOrNull { it.id == deletingId }?.let { photo ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.photo_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingId = null
                        actions.deletePhoto(photo)
                    },
                ) {
                    Text(stringResource(R.string.delete_set))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhoto, @StringRes label: Int, file: File, onDelete: () -> Unit) {
    val spacing = LocalSpacing.current
    val formatter = rememberShortDayFormatter()
    val text = stringResource(label, photo.date.format(formatter))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
        PhotoThumbnail(
            file = file,
            contentDescription = text,
            modifier = Modifier
                .width(dimensionResource(R.dimen.photo_thumbnail_width))
                .height(dimensionResource(R.dimen.photo_thumbnail_height)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = text, style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = stringResource(R.string.photo_delete))
            }
        }
    }
}

@get:StringRes
private val PhotoPose.labelRes: Int
    get() = when (this) {
        PhotoPose.FRONT -> R.string.photo_pose_front
        PhotoPose.SIDE -> R.string.photo_pose_side
        PhotoPose.BACK -> R.string.photo_pose_back
    }
