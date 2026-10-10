package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input

data class PayerInput(
    val email: String,
    val firstName: String,
    val lastName: String,
    val documentType: String,
    val documentNumber: String,
    val areaCode: String? = null,
    val phoneNumber: String? = null,
)
