package com.erpnext.pos.utils

import com.erpnext.pos.BuildKonfig
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions

actual object AppSentry {
    private var started = false
    private var initializing = false

    actual fun init() {
        if (started || initializing) return
        initializing = true
        val dsn = BuildKonfig.SENTRY_DSN
        if (dsn.isBlank()) {
            com.erpnext.pos.DesktopLogger.warn("Sentry disabled: DSN is blank")
            initializing = false
            return
        }
        val debug = System.getenv("SENTRY_DEBUG") == "1" ||
            System.getProperty("sentry.debug") == "true"
        Sentry.init { options: SentryOptions ->
            options.dsn = dsn
            options.environment = BuildKonfig.SENTRY_ENV
            options.tracesSampleRate = 1.0
            options.profilesSampleRate = 1.0
            options.isEnableAutoSessionTracking = true
            options.release = "erpnext-pos@${BuildKonfig.SENTRY_ENV}"
            options.isDebug = debug
            options.isEnableUncaughtExceptionHandler = true
        }
        com.erpnext.pos.DesktopLogger.info("Sentry initialized (env=${BuildKonfig.SENTRY_ENV})")
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
        com.erpnext.pos.DesktopLogger.info("Sentry captured event id=$id message=${message ?: throwable.message}")
        Sentry.flush(2000)
    }
}
