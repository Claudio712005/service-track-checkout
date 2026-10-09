package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

data class IdentificationRequest(
    val type: String,
    val number: String,
)
