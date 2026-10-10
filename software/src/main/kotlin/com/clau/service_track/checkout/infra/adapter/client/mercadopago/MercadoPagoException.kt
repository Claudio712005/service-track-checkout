package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.MercadoPagoErrorResponse
import com.clau.service_track.checkout.infra.config.client.RetryableIntegrationFailure
import org.springframework.http.HttpStatusCode

sealed class MercadoPagoException(
    val statusCode: HttpStatusCode,
    val error: MercadoPagoErrorResponse?,
    message: String,
) : RuntimeException(message)

class MercadoPagoRequestException(
    statusCode: HttpStatusCode,
    error: MercadoPagoErrorResponse?,
) : MercadoPagoException(statusCode, error, "mercado pago recusou a requisicao: status=$statusCode causa=${error?.causes()}")

class MercadoPagoUnavailableException(
    statusCode: HttpStatusCode,
    error: MercadoPagoErrorResponse?,
) : MercadoPagoException(statusCode, error, "mercado pago indisponivel: status=$statusCode causa=${error?.causes()}"),
    RetryableIntegrationFailure

private fun MercadoPagoErrorResponse.causes(): String =
    cause?.joinToString(";") { "${it.code}:${it.description}" } ?: message ?: error.orEmpty()
