package com.erpnext.pos.utils

import com.erpnext.pos.AppContext
import com.erpnext.pos.BuildKonfig
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.SentryLevel

actual object AppSentry {
    private var started = false
    private var initializing = false

    actual fun init() {
        if (started || initializing) return
        initializing = true
        val dsn = BuildKonfig.SENTRY_DSN
        if (dsn.isBlank()) {
            android.util.Log.w("AppSentry", "Sentry disabled: DSN is blank")
            initializing = false
            return
        }
        val debug = System.getenv("SENTRY_DEBUG") == "1" ||
            System.getProperty("sentry.debug") == "true"
        val context = AppContext.get()
        SentryAndroid.init(context) { options: SentryAndroidOptions ->
            options.dsn = dsn
            options.environment = BuildKonfig.SENTRY_ENV
            options.tracesSampleRate = 1.0
            options.profilesSampleRate = 1.0
            options.isEnableAutoSessionTracking = true
            options.release = "erpnext-pos@${BuildKonfig.SENTRY_ENV}"
            options.isDebug = debug
            options.isEnableUncaughtExceptionHandler = true
        }
        android.util.Log.i("AppSentry", "Sentry initialized (env=${BuildKonfig.SENTRY_ENV})")
        started = true
        initializing = false
    }

    actual fun breadcrumb(message: String) {
        if (!started) {
            if (initializing) return
            init()
        }
        Sentry.addBreadcrumb(message)
    }

    actual fun capture(throwable: Throwable, message: String?) {
        if (!started) {
            if (initializing) return
            init()
        }
        if (message != null) {
            Sentry.captureMessage(message, SentryLevel.ERROR)
        }
        val id = Sentry.captureException(throwable)
        android.util.Log.i("AppSentry", "Sentry captured event id=$id message=${message ?: throwable.message}")
        Sentry.flush(2000)
    }
}
