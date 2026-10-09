package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input

import java.math.BigDecimal

data class CartaoCheckoutInput(
    val transactionAmount: BigDecimal,
    val description: String,
    val externalReference: String,
    val payer: PayerInput,
    val token: String,
    val paymentMethodId: String,
    val installments: Int,
    val issuerId: String? = null,
    val capture: Boolean = true,
    val statementDescriptor: String? = null,
    val threeDSecureMode: String? = null,
    val notificationUrl: String? = null,
)
