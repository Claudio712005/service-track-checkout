package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MercadoPagoErrorItemResponse(
    val code: String? = null,
    val message: String? = null,
    val details: List<String>? = null,
)
