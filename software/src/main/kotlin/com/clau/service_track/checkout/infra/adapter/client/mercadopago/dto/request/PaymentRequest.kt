package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentRequest(
    @JsonProperty("transaction_amount")
    val transactionAmount: BigDecimal,

    @JsonProperty("payment_method_id")
    val paymentMethodId: String,

    val payer: PayerRequest,

    val description: String? = null,

    val token: String? = null,

    val installments: Int? = null,

    @JsonProperty("issuer_id")
    val issuerId: String? = null,

    @JsonProperty("payment_method_option_id")
    val paymentMethodOptionId: String? = null,

    @JsonProperty("external_reference")
    val externalReference: String? = null,

    @JsonProperty("notification_url")
    val notificationUrl: String? = null,

    @JsonProperty("callback_url")
    val callbackUrl: String? = null,

    @JsonProperty("statement_descriptor")
    val statementDescriptor: String? = null,

    @JsonProperty("date_of_expiration")
    val dateOfExpiration: OffsetDateTime? = null,

    val capture: Boolean? = null,

    @JsonProperty("binary_mode")
    val binaryMode: Boolean? = null,

    @JsonProperty("three_d_secure_mode")
    val threeDSecureMode: String? = null,

    @JsonProperty("application_fee")
    val applicationFee: BigDecimal? = null,

    @JsonProperty("net_amount")
    val netAmount: BigDecimal? = null,

    @JsonProperty("coupon_amount")
    val couponAmount: BigDecimal? = null,

    val metadata: Map<String, Any>? = null,

    @JsonProperty("transaction_details")
    val transactionDetails: TransactionDetailsRequest? = null,

    @JsonProperty("additional_info")
    val additionalInfo: AdditionalInfoRequest? = null,
)
