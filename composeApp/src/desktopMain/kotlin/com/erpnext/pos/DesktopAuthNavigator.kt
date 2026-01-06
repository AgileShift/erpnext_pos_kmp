package com.erpnext.pos

import com.erpnext.pos.navigation.AuthNavigator
import java.awt.Desktop
import java.net.URI

class DesktopAuthNavigator : AuthNavigator {
    override fun openAuthPage(authUrl: String) {
        val uri = URI(authUrl)

        DesktopLogger.info("Opening auth URL: $authUrl")
        System.err.println("DesktopAuthNavigator.openAuthPage -> $authUrl")

        // Prefer OS command (more reliable in packaged apps)
        runCatching { openWithSystemCommand(uri) }
            .onSuccess {
                DesktopLogger.info("Opened auth URL using system command.")
                return
            }
            .onFailure {
                DesktopLogger.warn("System command failed for auth URL.", it)
            }

        // JVM Desktop API fallback
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                runCatching {
                    desktop.browse(uri)
                }.onFailure {
                    DesktopLogger.warn("Desktop.browse failed for auth URL.", it)
                }.onSuccess {
                    DesktopLogger.info("Opened auth URL using Desktop.browse.")
                    return
                }
            }
        }

        throw IllegalStateException("No se pudo abrir el navegador para: $authUrl")
    }

    private fun openWithSystemCommand(uri: URI) {
        val url = uri.toString()
        val os = System.getProperty("os.name").lowercase()

        val cmd = when {
            os.contains("mac") -> listOf("open", url)
            os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
            else -> listOf("xdg-open", url) // Linux
        }

        ProcessBuilder(cmd).start()
    }
}
