package com.erpnext.pos.utils.oauth

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer
import java.util.concurrent.atomic.AtomicInteger

actual object OAuthRedirect {
    private var server: HttpServer? = null
    private val port = AtomicInteger(0)

    actual fun redirectUri(): String {
        if (server == null) {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            port.set(s.address.port)
            s.createContext("/oauth2redirect") { ex ->
                val html = "OK, puedes volver a la app."
                ex.sendResponseHeaders(200, html.toByteArray().size.toLong())
                ex.responseBody.use { it.write(html.toByteArray()) }
            }
            s.start()
            server = s
        }
        return "http://127.0.0.1:${port.get()}/oauth2redirect"
    }

    fun stop() {
        server?.stop(0)
        server = null
        port.set(0)
    }
}
