package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input

data class PayerAddressInput(
    val zipCode: String,
    val streetName: String,
    val streetNumber: String,
    val neighborhood: String,
    val city: String,
    val federalUnit: String,
)
