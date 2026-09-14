package hr.rostanic20.gymbro.ui.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration

private const val MINUTES_PER_HOUR = 60L

@Composable
fun HealthCard(viewModel: HealthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    HealthCardContent(
        state = state,
        onConnect = { permissions.launch(viewModel.readPermissions) },
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun HealthCardContent(state: HealthUiState, onConnect: () -> Unit, onRetry: () -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(text = stringResource(R.string.health_title), style = MaterialTheme.typography.titleMedium)
            when (state) {
                HealthUiState.Loading -> Text(
                    text = stringResource(R.string.health_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HealthUiState.Unavailable -> Text(
                    text = stringResource(R.string.health_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HealthUiState.NeedsPermission -> {
                    Text(text = stringResource(R.string.health_connect_body), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onConnect) {
                        Text(stringResource(R.string.health_connect))
                    }
                }
                HealthUiState.Failed -> {
                    Text(text = stringResource(R.string.health_failed), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.health_retry))
                    }
                }
                is HealthUiState.Connected -> {
                    val noValue = stringResource(R.string.health_no_value)
                    Text(
                        text = stringResource(
                            R.string.health_steps,
                            state.steps?.let { formatCount(it.toInt(), locale) } ?: noValue,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.health_sleep, state.sleep?.let { durationLabel(it) } ?: noValue),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.steps == null && state.sleep == null) {
                        Text(
                            text = stringResource(R.string.health_no_data_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun durationLabel(duration: Duration): String {
    val minutes = duration.toMinutes()
    return stringResource(
        R.string.duration_hours_minutes,
        (minutes / MINUTES_PER_HOUR).toString(),
        (minutes % MINUTES_PER_HOUR).toString(),
    )
}
