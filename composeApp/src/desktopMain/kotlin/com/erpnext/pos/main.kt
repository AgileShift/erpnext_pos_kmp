package com.erpnext.pos

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.erpnext.pos.di.appModule
import com.erpnext.pos.di.v2.appModulev2
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
                appModule,
                appModulev2,
                desktopModule
            )
        }) {
            AppNavigation()
        }
    }
}
