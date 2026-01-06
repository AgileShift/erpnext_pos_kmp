package com.erpnext.pos.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.utils.AppSentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected fun executeUseCase(
        action: suspend CoroutineScope.() -> Unit,
        exceptionHandler: suspend (Throwable) -> Unit,
        finallyHandler: (suspend () -> Unit)? = null
    ): Job? {
        return viewModelScope.launch {
            try {
                action.invoke(this)
            } catch (e: Exception) {
                AppSentry.capture(e, e.message)
                exceptionHandler.invoke(e)
            } finally {
                finallyHandler?.invoke()
            }
        }
    }
}
