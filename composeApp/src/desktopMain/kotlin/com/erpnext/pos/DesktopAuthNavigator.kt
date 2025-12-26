package com.erpnext.pos

import com.erpnext.pos.navigation.AuthNavigator
import java.awt.Desktop
import java.net.URI

class DesktopAuthNavigator : AuthNavigator {
    override fun openAuthPage(authUrl: String) {
        val uri = URI(authUrl)

        // JVM Desktop API
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri)
                return
            }
        }

        // Fallback por si estás en headless / WSL / entornos raros
        fallbackOpen(uri)
    }

    private fun fallbackOpen(uri: URI) {
        val url = uri.toString()
        val os = System.getProperty("os.name").lowercase()

        val cmd = when {
            os.contains("mac") -> listOf("open", url)
            os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
            else -> listOf("xdg-open", url) // Linux
        }

        runCatching { ProcessBuilder(cmd).start() }
            .getOrElse {
                throw IllegalStateException(
                    "No se pudo abrir el navegador para: $url",
                    it
                )
            }
    }
}