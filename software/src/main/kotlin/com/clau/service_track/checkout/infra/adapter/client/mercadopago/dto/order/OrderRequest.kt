package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrderRequest(
    val type: String = "online",

    @JsonProperty("processing_mode")
    val processingMode: String = "automatic",

    @JsonProperty("total_amount")
    val totalAmount: String,

    @JsonProperty("external_reference")
    val externalReference: String,

    val description: String? = null,

    val payer: OrderPayerRequest,

    val transactions: OrderTransactionsRequest,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrderPayerRequest(
    val email: String,

    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    val identification: OrderIdentificationRequest? = null,

    val address: OrderAddressRequest? = null,
)

data class OrderIdentificationRequest(
    val type: String,
    val number: String,
)

data class OrderAddressRequest(
    @JsonProperty("zip_code")
    val zipCode: String,

    @JsonProperty("street_name")
    val streetName: String,

    @JsonProperty("street_number")
    val streetNumber: String,

    val neighborhood: String,

    val city: String,

    val state: String,
)

data class OrderTransactionsRequest(
    val payments: List<OrderPaymentRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrderPaymentRequest(
    val amount: String,

    @JsonProperty("payment_method")
    val paymentMethod: OrderPaymentMethodRequest,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrderPaymentMethodRequest(
    val id: String,

    val type: String,

    val token: String? = null,

    val installments: Int? = null,
)
