package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PixCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.PaymentRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.PaymentResponse
import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import org.springframework.stereotype.Component

@Component
class PixCheckout(
    private val paymentClient: MercadoPagoPaymentClient,
    private val properties: MercadoPagoProperties,
) {

    fun send(input: PixCheckoutInput, idempotencyKey: String): PaymentResponse =
        paymentClient.create(
            integration = ClientsIntegrations.ENVIAR_PAGAMENTO_PIX,
            request = paymentRequest(input),
            idempotencyKey = idempotencyKey,
        )

    private fun paymentRequest(input: PixCheckoutInput) = PaymentRequest(
        transactionAmount = input.transactionAmount,
        paymentMethodId = MercadoPagoPaymentMethod.PIX.id,
        payer = PayerRequestFactory.payerRequest(input.payer),
        description = input.description,
        externalReference = input.externalReference,
        dateOfExpiration = input.dateOfExpiration,
        notificationUrl = input.notificationUrl ?: properties.notificationUrl,
    )
}
