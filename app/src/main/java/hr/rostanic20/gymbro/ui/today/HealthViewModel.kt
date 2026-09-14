package hr.rostanic20.gymbro.ui.today

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.HealthSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data object Unavailable : HealthUiState
    data object NeedsPermission : HealthUiState
    data object Failed : HealthUiState
    data class Connected(val steps: Long?, val sleep: Duration?) : HealthUiState
}

class HealthViewModel(
    private val source: HealthSource,
    private val dates: DateProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    val readPermissions: Set<String> get() = source.readPermissions

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = try {
                when {
                    !source.isAvailable() -> HealthUiState.Unavailable
                    !source.hasPermissions() -> HealthUiState.NeedsPermission
                    else -> source.summaryFor(dates.today(), dates.now()).let {
                        HealthUiState.Connected(steps = it.steps, sleep = it.sleep)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GymBro", "Reading Health Connect failed", e)
                HealthUiState.Failed
            }
        }
    }
}
