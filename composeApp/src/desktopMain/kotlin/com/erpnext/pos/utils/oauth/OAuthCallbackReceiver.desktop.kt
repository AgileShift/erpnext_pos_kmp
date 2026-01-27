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

        val s = HttpServer.create(InetSocketAddress(HOST, PORT), 0)
        s.createContext(PATH) { ex ->
            val params = parseQuery(ex.requestURI.rawQuery.orEmpty())
            val html = """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>ERPNext POS</title>
                  <style>
                    :root { color-scheme: light; }
                    body {
                      margin: 0;
                      font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
                      background: radial-gradient(100% 100% at 0% 0%, #eaf3ff 0%, #f6f2ff 45%, #ffffff 100%);
                      color: #0f172a;
                    }
                    .wrap {
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      padding: 32px;
                    }
                    .card {
                      max-width: 520px;
                      width: 100%;
                      background: #ffffff;
                      border-radius: 20px;
                      box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
                      padding: 28px 26px;
                      border: 1px solid rgba(15, 23, 42, 0.08);
                    }
                    .badge {
                      display: inline-flex;
                      align-items: center;
                      gap: 10px;
                      background: #e8f2ff;
                      color: #0b5cff;
                      padding: 8px 12px;
                      border-radius: 999px;
                      font-weight: 600;
                      font-size: 13px;
                    }
                    .title {
                      font-size: 22px;
                      font-weight: 700;
                      margin: 16px 0 8px;
                    }
                    .text {
                      color: #475569;
                      font-size: 15px;
                      line-height: 1.5;
                    }
                    .hint {
                      margin-top: 16px;
                      background: #f8fafc;
                      border-radius: 14px;
                      padding: 14px 16px;
                      font-size: 14px;
                      color: #334155;
                    }
                    .check {
                      width: 36px;
                      height: 36px;
                      border-radius: 50%;
                      background: #22c55e;
                      display: inline-grid;
                      place-items: center;
                      color: white;
                      font-weight: 700;
                      margin-right: 8px;
                    }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="badge">ERPNext POS</div>
                      <div class="title">Autenticacion completada</div>
                      <p class="text">
                        Ya puedes cerrar esta ventana y regresar a la aplicacion para continuar.
                      </p>
                      <div class="hint">
                        <span class="check">✓</span>
                        La sesion se sincronizo correctamente.
                      </div>
                    </div>
                  </div>
                </body>
                </html>
            """.trimIndent()

            ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }

            deferred?.complete(params)
        }
        s.start()

        server = s
        redirect = REDIRECT_URL

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
        q.split("&").filter { it.isNotBlank() }.associate { part ->
            val (k, v) = part.split("=", limit = 2)
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        }

    private companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 8070
        const val PATH = "/oauth2redirect"
        const val REDIRECT_URL = "http://127.0.0.1:8070/oauth2redirect"
    }
}
