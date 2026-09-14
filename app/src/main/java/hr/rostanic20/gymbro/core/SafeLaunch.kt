package hr.rostanic20.gymbro.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val logUncaught = CoroutineExceptionHandler { _, throwable ->
    Log.e("GymBro", "Coroutine failed", throwable)
}

fun ViewModel.safeLaunch(block: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch(logUncaught, block = block)
