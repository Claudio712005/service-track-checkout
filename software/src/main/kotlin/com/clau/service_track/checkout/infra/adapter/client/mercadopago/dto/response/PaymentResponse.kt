package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResponse(
    val id: Long,

    val status: String,

    @JsonProperty("status_detail")
    val statusDetail: String? = null,

    @JsonProperty("payment_method_id")
    val paymentMethodId: String? = null,

    @JsonProperty("payment_type_id")
    val paymentTypeId: String? = null,

    @JsonProperty("operation_type")
    val operationType: String? = null,

    @JsonProperty("currency_id")
    val currencyId: String? = null,

    val description: String? = null,

    @JsonProperty("external_reference")
    val externalReference: String? = null,

    @JsonProperty("transaction_amount")
    val transactionAmount: BigDecimal? = null,

    @JsonProperty("transaction_amount_refunded")
    val transactionAmountRefunded: BigDecimal? = null,

    @JsonProperty("net_amount")
    val netAmount: BigDecimal? = null,

    val installments: Int? = null,

    @JsonProperty("issuer_id")
    val issuerId: String? = null,

    @JsonProperty("authorization_code")
    val authorizationCode: String? = null,

    @JsonProperty("live_mode")
    val liveMode: Boolean? = null,

    val captured: Boolean? = null,

    @JsonProperty("binary_mode")
    val binaryMode: Boolean? = null,

    @JsonProperty("date_created")
    val dateCreated: OffsetDateTime? = null,

    @JsonProperty("date_approved")
    val dateApproved: OffsetDateTime? = null,

    @JsonProperty("date_last_updated")
    val dateLastUpdated: OffsetDateTime? = null,

    @JsonProperty("date_of_expiration")
    val dateOfExpiration: OffsetDateTime? = null,

    @JsonProperty("money_release_date")
    val moneyReleaseDate: OffsetDateTime? = null,

    @JsonProperty("notification_url")
    val notificationUrl: String? = null,

    @JsonProperty("statement_descriptor")
    val statementDescriptor: String? = null,

    val payer: PayerResponse? = null,

    val card: CardResponse? = null,

    @JsonProperty("transaction_details")
    val transactionDetails: TransactionDetailsResponse? = null,

    @JsonProperty("point_of_interaction")
    val pointOfInteraction: PointOfInteractionResponse? = null,

    @JsonProperty("fee_details")
    val feeDetails: List<FeeDetailResponse>? = null,

    val refunds: List<RefundResponse>? = null,

    val metadata: Map<String, Any>? = null,
)
