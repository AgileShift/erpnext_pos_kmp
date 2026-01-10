package com.erpnext.pos.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.loading.LoadingIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected fun executeUseCase(
        action: suspend CoroutineScope.() -> Unit,
        exceptionHandler: suspend (Throwable) -> Unit,
        finallyHandler: (suspend () -> Unit)? = null,
        showLoading: Boolean = true
    ): Job {
        return viewModelScope.launch {
            if (showLoading) LoadingIndicator.start()
            try {
                action.invoke(this)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelaciones por cambio de filtros/navigation no deben tratarse como error.
            } catch (e: Exception) {
                AppSentry.capture(e, e.message)
                exceptionHandler.invoke(e)
            } finally {
                finallyHandler?.invoke()
                if (showLoading) LoadingIndicator.stop()
            }
        }
    }
}
