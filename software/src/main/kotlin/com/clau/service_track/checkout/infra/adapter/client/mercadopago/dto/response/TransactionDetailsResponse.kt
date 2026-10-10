package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionDetailsResponse(
    @JsonProperty("financial_institution")
    val financialInstitution: String? = null,

    @JsonProperty("net_received_amount")
    val netReceivedAmount: BigDecimal? = null,

    @JsonProperty("total_paid_amount")
    val totalPaidAmount: BigDecimal? = null,

    @JsonProperty("installment_amount")
    val installmentAmount: BigDecimal? = null,

    @JsonProperty("overpaid_amount")
    val overpaidAmount: BigDecimal? = null,

    @JsonProperty("external_resource_url")
    val externalResourceUrl: String? = null,

    @JsonProperty("digitable_line")
    val digitableLine: String? = null,

    @JsonProperty("verification_code")
    val verificationCode: String? = null,

    @JsonProperty("payment_method_reference_id")
    val paymentMethodReferenceId: String? = null,

    @JsonProperty("acquirer_reference")
    val acquirerReference: String? = null,

    @JsonProperty("transaction_id")
    val transactionId: String? = null,

    @JsonProperty("bank_transfer_id")
    val bankTransferId: String? = null,

    @JsonProperty("payable_deferral_period")
    val payableDeferralPeriod: String? = null,

    val barcode: BarcodeResponse? = null,
)
