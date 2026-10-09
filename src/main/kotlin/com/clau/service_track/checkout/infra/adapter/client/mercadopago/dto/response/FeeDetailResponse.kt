package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDetailResponse(
    val type: String? = null,

    @JsonProperty("fee_payer")
    val feePayer: String? = null,

    val amount: BigDecimal? = null,
)
