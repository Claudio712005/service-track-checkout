package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentificationResponse(
    val type: String? = null,
    val number: String? = null,
)
