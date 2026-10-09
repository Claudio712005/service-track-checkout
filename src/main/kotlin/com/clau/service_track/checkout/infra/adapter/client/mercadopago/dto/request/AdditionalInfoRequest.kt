package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdditionalInfoRequest(
    @JsonProperty("ip_address")
    val ipAddress: String? = null,

    val items: List<ItemRequest>? = null,

    val payer: AdditionalInfoPayerRequest? = null,

    val shipments: ShipmentsRequest? = null,
)
