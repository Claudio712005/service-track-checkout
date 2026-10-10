package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PointOfInteractionResponse(
    val type: String? = null,

    @JsonProperty("sub_type")
    val subType: String? = null,

    @JsonProperty("linked_to")
    val linkedTo: String? = null,

    @JsonProperty("transaction_data")
    val transactionData: TransactionDataResponse? = null,
)
