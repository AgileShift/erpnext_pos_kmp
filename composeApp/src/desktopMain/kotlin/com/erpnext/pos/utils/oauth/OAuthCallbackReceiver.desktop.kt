package com.erpnext.pos.utils.oauth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

actual class OAuthCallbackReceiver {
    private var server: HttpServer? = null
    private var deferred: CompletableDeferred<Map<String, String>>? = null
    private var redirect: String = ""

    actual fun start(redirectUrl: String): String {
        if (server != null) return redirect

        deferred = CompletableDeferred()

        val redirectUri = URI.create(redirectUrl)
        val host = redirectUri.host ?: error("Redirect URL must include a host")
        val port = redirectUri.port
        require(port != -1) { "Redirect URL must include a fixed port" }
        val path = redirectUri.path.ifBlank { "/" }

        val s = HttpServer.create(InetSocketAddress(host, port), 0)
        s.createContext(path) { ex ->
            val params = parseQuery(ex.requestURI.rawQuery.orEmpty())
            val html = """
                <html><body>
                <h3>Login completado</h3>
                Puedes cerrar esta ventana y volver a la app.
                </body></html>
            """.trimIndent()

            ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }

            deferred?.complete(params)
        }
        s.start()

        server = s
        redirect = redirectUrl
        return redirect
    }

    actual suspend fun awaitCode(expectedState: String): String {
        val params = deferred?.await() ?: error("Receiver no iniciado")
        val state = params["state"]
        require(state == expectedState) { "State mismatch" }
        return params["code"] ?: error("No llegó 'code' en callback: $params")
    }

    actual fun stop() {
        server?.stop(0)
        server = null
        deferred = null
        redirect = ""
    }

    private fun parseQuery(q: String): Map<String, String> =
        q.split("&")
            .filter { it.isNotBlank() }
            .associate { part ->
                val (k, v) = part.split("=", limit = 2)
                URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
            }
}
