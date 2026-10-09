package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionDataResponse(
    @JsonProperty("qr_code")
    val qrCode: String? = null,

    @JsonProperty("qr_code_base64")
    val qrCodeBase64: String? = null,

    @JsonProperty("ticket_url")
    val ticketUrl: String? = null,

    @JsonProperty("transaction_id")
    val transactionId: String? = null,

    @JsonProperty("bank_transfer_id")
    val bankTransferId: Long? = null,

    @JsonProperty("financial_institution")
    val financialInstitution: Long? = null,
)
