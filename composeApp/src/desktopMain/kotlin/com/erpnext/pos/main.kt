package com.erpnext.pos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.erpnext.pos.di.appModule
import com.erpnext.pos.utils.AppSentry
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level

fun main() = application {
    DesktopLogger.init()
    DesktopLogger.info("Desktop app started")
    AppSentry.init()

    // Default Desktop Size
    val state = rememberWindowState(
        width = 1215.dp,
        height = 810.dp,
    )

    Window(
        undecorated = false,
        transparent = false,
        icon = rememberVectorPainter(Icons.Default.PointOfSale),
        onCloseRequest = ::exitApplication,
        title = "ERPNext POS",
        state = state,
    ) {
        KoinApplication(application = {
            printLogger(Level.DEBUG)
            modules(
                appModule,
                desktopModule
            )
        }) {
            AppNavigation()
        }
    }
}
