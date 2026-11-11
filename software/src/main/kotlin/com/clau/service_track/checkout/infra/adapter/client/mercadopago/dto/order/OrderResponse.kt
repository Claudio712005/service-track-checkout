package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderResponse(
    val id: String,

    val status: String? = null,

    @JsonProperty("status_detail")
    val statusDetail: String? = null,

    @JsonProperty("external_reference")
    val externalReference: String? = null,

    @JsonProperty("total_amount")
    val totalAmount: String? = null,

    val transactions: OrderTransactionsResponse? = null,
) {
    val payment: OrderPaymentResponse? get() = transactions?.payments?.firstOrNull()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderTransactionsResponse(
    val payments: List<OrderPaymentResponse> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderPaymentResponse(
    val id: String? = null,

    val status: String? = null,

    @JsonProperty("status_detail")
    val statusDetail: String? = null,

    @JsonProperty("payment_method")
    val paymentMethod: OrderPaymentMethodResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderPaymentMethodResponse(
    val id: String? = null,

    val type: String? = null,

    @JsonProperty("ticket_url")
    val ticketUrl: String? = null,

    @JsonProperty("qr_code")
    val qrCode: String? = null,

    @JsonProperty("qr_code_base64")
    val qrCodeBase64: String? = null,

    @JsonProperty("digitable_line")
    val digitableLine: String? = null,
)
