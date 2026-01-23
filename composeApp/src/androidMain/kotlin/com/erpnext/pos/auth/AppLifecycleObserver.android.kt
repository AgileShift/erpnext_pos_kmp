package com.erpnext.pos.auth

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@RequiresApi(Build.VERSION_CODES.O)
actual class AppLifecycleObserver actual constructor() {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    actual val onResume: Flow<Unit> = events.asSharedFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    events.tryEmit(Unit)
                }
            }
        )
    }
}
