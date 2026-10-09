package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class RefundResponse(
    val id: Long,

    @JsonProperty("payment_id")
    val paymentId: Long? = null,

    val amount: BigDecimal? = null,

    @JsonProperty("adjustment_amount")
    val adjustmentAmount: BigDecimal? = null,

    val status: String? = null,

    @JsonProperty("refund_mode")
    val refundMode: String? = null,

    val reason: String? = null,

    @JsonProperty("unique_sequence_number")
    val uniqueSequenceNumber: String? = null,

    @JsonProperty("date_created")
    val dateCreated: OffsetDateTime? = null,
)
