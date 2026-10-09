package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PhoneRequest(
    @JsonProperty("area_code")
    val areaCode: String? = null,

    val number: String? = null,
)
