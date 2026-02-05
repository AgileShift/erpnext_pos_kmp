package com.erpnext.pos.utils.loading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoadingUiState(
    val isLoading: Boolean = false,
    val message: String = "",
    val progress: Float? = null,
    val currentStep: Int? = null,
    val totalSteps: Int? = null
)

object LoadingIndicator {
    private val mutex = Mutex()
    private var counter: Int = 0
    private val _isLoading = MutableStateFlow(false)
    private val _state = MutableStateFlow(LoadingUiState())
    val state = _state.asStateFlow()

    suspend fun start(
        message: String = "Procesando...",
        progress: Float? = null,
        currentStep: Int? = null,
        totalSteps: Int? = null
    ) {
        mutex.withLock {
            counter += 1
            publish(
                LoadingUiState(
                    isLoading = true,
                    message = message,
                    progress = progress?.coerceIn(0f, 1f),
                    currentStep = currentStep,
                    totalSteps = totalSteps
                )
            )
        }
    }

    suspend fun update(
        message: String? = null,
        progress: Float? = null,
        currentStep: Int? = null,
        totalSteps: Int? = null
    ) {
        mutex.withLock {
            if (counter <= 0) return
            val current = _state.value
            publish(
                current.copy(
                    isLoading = true,
                    message = message ?: current.message,
                    progress = progress?.coerceIn(0f, 1f) ?: current.progress,
                    currentStep = currentStep ?: current.currentStep,
                    totalSteps = totalSteps ?: current.totalSteps
                )
            )
        }
    }

    suspend fun stop() {
        mutex.withLock {
            counter = (counter - 1).coerceAtLeast(0)
            if (counter <= 0) {
                publish(LoadingUiState())
            } else {
                publish(
                    _state.value.copy(
                        isLoading = true,
                        progress = null,
                        currentStep = null,
                        totalSteps = null
                    )
                )
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            counter = 0
            publish(LoadingUiState())
        }
    }

    private fun publish(value: LoadingUiState) {
        _state.value = value
        _isLoading.value = value.isLoading
    }
}
