package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderResponse
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.MercadoPagoErrorResponse
import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import com.clau.service_track.checkout.infra.config.client.IntegrationClient
import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

@Component
class MercadoPagoOrderClient(
    private val integrationClient: IntegrationClient,
    private val properties: MercadoPagoProperties,
    private val objectMapper: ObjectMapper,
) {

    fun create(request: OrderRequest, idempotencyKey: String): OrderResponse {
        val response = integrationClient.call(ClientsIntegrations.ORDER_CREATE) { restClient ->
            restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authorization(idempotencyKey))
                .body(request)
                .retrieve()
                .failOnError()
                .requiredBody(OrderResponse::class.java)
        }

        log.info(
            "[mercadopago] ordem criada id={} pagamento={} meio={} status={} detalhe={}",
            response.id,
            response.payment?.id,
            response.payment?.paymentMethod?.id,
            response.status,
            response.statusDetail,
        )

        return response
    }

    fun find(orderId: String): OrderResponse =
        integrationClient.call(ClientsIntegrations.ORDER_GET) { restClient ->
            restClient.get()
                .uri("/{orderId}", orderId)
                .headers(authorization())
                .retrieve()
                .failOnError()
                .requiredBody(OrderResponse::class.java)
        }

    private fun authorization(idempotencyKey: String? = null): Consumer<HttpHeaders> = Consumer { headers ->
        headers.setBearerAuth(properties.accessToken)
        idempotencyKey?.let { headers.set(IDEMPOTENCY_HEADER, it) }
    }

    private fun RestClient.ResponseSpec.failOnError(): RestClient.ResponseSpec =
        this
            .onStatus({ it.is4xxClientError }) { _, response ->
                throw MercadoPagoRequestException(response.statusCode, errorOf(response))
            }
            .onStatus({ it.is5xxServerError }) { _, response ->
                throw MercadoPagoUnavailableException(response.statusCode, errorOf(response))
            }

    private fun errorOf(response: ClientHttpResponse): MercadoPagoErrorResponse? =
        runCatching {
            objectMapper.readValue(response.body, MercadoPagoErrorResponse::class.java)
        }.getOrNull()

    private companion object {
        val log = LoggerFactory.getLogger(MercadoPagoOrderClient::class.java)
        const val IDEMPOTENCY_HEADER = "X-Idempotency-Key"
    }
}
