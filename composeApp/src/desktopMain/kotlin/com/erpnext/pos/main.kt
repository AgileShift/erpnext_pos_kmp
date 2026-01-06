package com.erpnext.pos

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level

fun main() = application {
    DesktopLogger.init()
    DesktopLogger.info("Desktop app started")
    com.erpnext.pos.utils.AppSentry.init()

    // Default Desktop Size
    val state = rememberWindowState(
        width = 1200.dp,
        height = 780.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "ERPNext POS",
        state = state,
    ) {
        KoinApplication(application = {
            printLogger(Level.DEBUG)
            modules(
                _root_ide_package_.com.erpnext.pos.di.appModule,
                desktopModule
            )
        }) {
            AppNavigation()
        }
    }
}
