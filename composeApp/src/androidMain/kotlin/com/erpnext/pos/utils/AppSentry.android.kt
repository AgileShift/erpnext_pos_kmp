package com.erpnext.pos.utils

import com.erpnext.pos.AppContext
import com.erpnext.pos.BuildKonfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions

actual object AppSentry {
    private var started = false
    private var initializing = false

    actual fun init() {
        if (started || initializing) return
        initializing = true
        try {
            val dsn = BuildKonfig.SENTRY_DSN
            if (dsn.isBlank()) {
                android.util.Log.w("AppSentry", "Sentry disabled: DSN is blank")
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
        } catch (t: Throwable) {
            android.util.Log.e("AppSentry", "Sentry init failed", t)
        } finally {
            initializing = false
        }
    }

    actual fun breadcrumb(
        message: String,
        category: String?,
        data: Map<String, String>
    ) {
        if (!started) {
            if (initializing) return
            init()
        }
        val crumb = Breadcrumb().apply {
            this.message = message
            this.category = category ?: "app"
            this.level = SentryLevel.INFO
            data.forEach { (key, value) -> setData(key, value) }
        }
        Sentry.addBreadcrumb(crumb)
    }

    actual fun capture(
        throwable: Throwable,
        message: String?,
        tags: Map<String, String>,
        extras: Map<String, String>
    ) {
        if (!started) {
            if (initializing) return
            init()
        }
        val contextMessage = message?.takeIf { it.isNotBlank() }
        contextMessage?.let {
            breadcrumb(
                message = it,
                category = "error.context",
                data = mapOf("exception" to throwable.javaClass.simpleName)
            )
        }
        var capturedId: Any? = null
        Sentry.withScope { scope ->
            scope.level = SentryLevel.ERROR
            contextMessage?.let { scope.setExtra("context_message", it) }
            tags.forEach { (key, value) -> scope.setTag(key, value) }
            extras.forEach { (key, value) -> scope.setExtra(key, value) }
            capturedId = Sentry.captureException(throwable)
        }
        android.util.Log.i(
            "AppSentry",
            "Sentry captured event id=$capturedId exception=${throwable.javaClass.simpleName} message=${contextMessage ?: throwable.message}"
        )
        Sentry.flush(2000)
    }
}
