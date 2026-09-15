package hr.rostanic20.gymbro.ui.body

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.PhotoPose
import hr.rostanic20.gymbro.domain.model.ProgressPhoto
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.rememberShortDayFormatter
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

private val SELECTED_BORDER = 3.dp

@Composable
fun PhotosScreen(onBack: () -> Unit, viewModel: PhotosViewModel = koinViewModel()) {
    val photos by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    photos?.let {
        PhotosContent(
            photosByPose = it,
            onBack = onBack,
            photoFile = viewModel::photoFile,
            onDelete = viewModel::deletePhoto,
        )
    }
}

@Composable
internal fun PhotosContent(
    photosByPose: Map<PhotoPose, List<ProgressPhoto>>,
    onBack: () -> Unit,
    photoFile: (String) -> File,
    onDelete: (ProgressPhoto) -> Unit,
) {
    val spacing = LocalSpacing.current
    var selected by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val all = photosByPose.values.flatten()
    val compared = selected.mapNotNull { id -> all.firstOrNull { it.id == id } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s12),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
                Text(text = stringResource(R.string.photos_title), style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Text(
                text = stringResource(if (compared.size < 2) R.string.photos_pick_two else R.string.photos_comparing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (compared.size == 2) {
            item { ComparisonCard(photos = compared, photoFile = photoFile, onClear = { selected = emptyList() }) }
        }
        PhotoPose.entries.forEach { pose ->
            val photos = photosByPose[pose].orEmpty()
            if (photos.isNotEmpty()) {
                item(key = pose) {
                    PoseRow(
                        pose = pose,
                        photos = photos,
                        selected = selected,
                        photoFile = photoFile,
                        onSelect = { photo ->
                            selected = when {
                                photo.id in selected -> selected - photo.id
                                selected.size < 2 -> selected + photo.id
                                else -> listOf(selected.last(), photo.id)
                            }
                        },
                        onLongPress = { deleting = it.id },
                    )
                }
            }
        }
        if (photosByPose.values.all { it.isEmpty() }) {
            item {
                Text(
                    text = stringResource(R.string.photo_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    all.firstOrNull { it.id == deleting }?.let { photo ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.photo_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        selected = selected - photo.id
                        onDelete(photo)
                    },
                ) {
                    Text(stringResource(R.string.delete_set))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PoseRow(
    pose: PhotoPose,
    photos: List<ProgressPhoto>,
    selected: List<Long>,
    photoFile: (String) -> File,
    onSelect: (ProgressPhoto) -> Unit,
    onLongPress: (ProgressPhoto) -> Unit,
) {
    val spacing = LocalSpacing.current
    val formatter = rememberShortDayFormatter()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s8)) {
        Text(text = stringResource(pose.labelRes), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.s8)) {
            items(photos, key = { it.id }) { photo ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.s4),
                ) {
                    val isSelected = photo.id in selected
                    PhotoThumbnail(
                        file = photoFile(photo.fileName),
                        contentDescription = photo.date.format(formatter),
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.photo_thumbnail_width))
                            .height(dimensionResource(R.dimen.photo_thumbnail_height))
                            .border(
                                width = if (isSelected) SELECTED_BORDER else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            )
                            .clickable(
                                onClick = { onSelect(photo) },
                                onClickLabel = stringResource(R.string.photos_compare_one),
                            ),
                    )
                    Text(text = photo.date.format(formatter), style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { onLongPress(photo) }) {
                        Text(stringResource(R.string.photo_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(photos: List<ProgressPhoto>, photoFile: (String) -> File, onClear: () -> Unit) {
    val spacing = LocalSpacing.current
    val formatter = rememberShortDayFormatter()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s12)) {
                photos.sortedBy { it.date }.forEach { photo ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.s4),
                    ) {
                        PhotoThumbnail(
                            file = photoFile(photo.fileName),
                            contentDescription = photo.date.format(formatter),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(R.dimen.photo_compare_height)),
                        )
                        Text(text = photo.date.format(formatter), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.photos_clear))
            }
        }
    }
}
