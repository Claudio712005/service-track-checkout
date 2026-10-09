package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input

import java.math.BigDecimal
import java.time.OffsetDateTime

data class BoletoCheckoutInput(
    val transactionAmount: BigDecimal,
    val description: String,
    val externalReference: String,
    val payer: PayerInput,
    val address: PayerAddressInput,
    val dateOfExpiration: OffsetDateTime? = null,
    val notificationUrl: String? = null,
)
