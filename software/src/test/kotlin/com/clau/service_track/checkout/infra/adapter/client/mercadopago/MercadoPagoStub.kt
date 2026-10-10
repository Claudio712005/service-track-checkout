package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

class MercadoPagoStub {

    data class RecordedRequest(
        val method: String,
        val path: String,
        val headers: Headers,
        val body: String,
    )

    private data class StubResponse(val status: Int, val body: String)

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val recorded = CopyOnWriteArrayList<RecordedRequest>()
    private val responses = ConcurrentLinkedQueue<StubResponse>()

    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}$BASE_PATH"

    val requests: List<RecordedRequest> get() = recorded

    val lastRequest: RecordedRequest get() = recorded.last()

    init {
        server.createContext(BASE_PATH) { exchange ->
            recorded += RecordedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                headers = exchange.requestHeaders,
                body = exchange.requestBody.readAllBytes().decodeToString(),
            )

            val response = responses.poll() ?: StubResponse(500, """{"message":"sem resposta na fila"}""")
            val payload = response.body.toByteArray()

            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(response.status, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()
    }

    fun enqueue(status: Int, body: String) {
        responses += StubResponse(status, body)
    }

    fun reset() {
        recorded.clear()
        responses.clear()
    }

    private companion object {
        const val BASE_PATH = "/v1/payments"
    }
}
