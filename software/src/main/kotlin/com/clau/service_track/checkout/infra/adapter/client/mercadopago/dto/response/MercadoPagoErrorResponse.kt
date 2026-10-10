package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MercadoPagoErrorResponse(
    val message: String? = null,
    val error: String? = null,
    val status: Int? = null,
    val cause: List<MercadoPagoErrorCauseResponse>? = null,
)
