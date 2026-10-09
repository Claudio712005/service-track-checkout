package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.CancelPaymentRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.PaymentRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.RefundRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.MercadoPagoErrorResponse
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.PaymentResponse
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.RefundResponse
import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import com.clau.service_track.checkout.infra.config.client.IntegrationClient
import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import java.math.BigDecimal
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

@Component
class MercadoPagoPaymentClient(
    private val integrationClient: IntegrationClient,
    private val properties: MercadoPagoProperties,
    private val objectMapper: ObjectMapper,
) {

    fun create(
        integration: ClientsIntegrations,
        request: PaymentRequest,
        idempotencyKey: String,
    ): PaymentResponse {
        val response = integrationClient.call(integration) { restClient ->
            restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authorization(idempotencyKey))
                .body(request)
                .retrieve()
                .failOnError()
                .requiredBody(PaymentResponse::class.java)
        }

        log.info(
            "[mercadopago] pagamento criado integracao={} id={} metodo={} status={} detalhe={}",
            integration,
            response.id,
            response.paymentMethodId,
            response.status,
            response.statusDetail,
        )

        return response
    }

    fun find(paymentId: Long): PaymentResponse =
        integrationClient.call(ClientsIntegrations.CONSULTAR_PAGAMENTO) { restClient ->
            restClient.get()
                .uri("/{paymentId}", paymentId)
                .headers(authorization())
                .retrieve()
                .failOnError()
                .requiredBody(PaymentResponse::class.java)
        }

    fun cancel(paymentId: Long, idempotencyKey: String): PaymentResponse {
        val response = integrationClient.call(ClientsIntegrations.CANCELAR_PAGAMENTO) { restClient ->
            restClient.put()
                .uri("/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authorization(idempotencyKey))
                .body(CancelPaymentRequest(status = CANCELLED))
                .retrieve()
                .failOnError()
                .requiredBody(PaymentResponse::class.java)
        }

        log.info("[mercadopago] pagamento cancelado id={} status={}", response.id, response.status)

        return response
    }

    fun refund(paymentId: Long, amount: BigDecimal?, idempotencyKey: String): RefundResponse {
        val response = integrationClient.call(ClientsIntegrations.ESTORNAR_PAGAMENTO) { restClient ->
            restClient.post()
                .uri("/{paymentId}/refunds", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authorization(idempotencyKey))
                .body(RefundRequest(amount = amount))
                .retrieve()
                .failOnError()
                .requiredBody(RefundResponse::class.java)
        }

        log.info(
            "[mercadopago] pagamento estornado pagamentoId={} estornoId={} status={}",
            paymentId,
            response.id,
            response.status,
        )

        return response
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
        val log = LoggerFactory.getLogger(MercadoPagoPaymentClient::class.java)
        const val IDEMPOTENCY_HEADER = "X-Idempotency-Key"
        const val CANCELLED = "cancelled"
    }
}
