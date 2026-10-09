package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.CartaoCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.PaymentRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.PaymentResponse
import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import org.springframework.stereotype.Component

@Component
class CartaoCheckout(
    private val paymentClient: MercadoPagoPaymentClient,
    private val properties: MercadoPagoProperties,
) {

    fun send(input: CartaoCheckoutInput, idempotencyKey: String): PaymentResponse =
        paymentClient.create(
            integration = ClientsIntegrations.ENVIAR_PAGAMENTO_CARTAO,
            request = paymentRequest(input),
            idempotencyKey = idempotencyKey,
        )

    private fun paymentRequest(input: CartaoCheckoutInput) = PaymentRequest(
        transactionAmount = input.transactionAmount,
        paymentMethodId = input.paymentMethodId,
        payer = PayerRequestFactory.payerRequest(input.payer),
        description = input.description,
        token = input.token,
        installments = input.installments,
        issuerId = input.issuerId,
        externalReference = input.externalReference,
        notificationUrl = input.notificationUrl ?: properties.notificationUrl,
        statementDescriptor = input.statementDescriptor,
        threeDSecureMode = input.threeDSecureMode,
        capture = input.capture,
    )
}
